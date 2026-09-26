package com.seef.checkqr.capture.parser

import com.seef.checkqr.core.common.Dinero
import com.seef.checkqr.core.model.BankTemplate
import com.seef.checkqr.core.model.ParseConfidence
import com.seef.checkqr.core.model.RawNotice
import com.seef.checkqr.core.model.TemplateBundle

/**
 * Convierte el aviso de una app de banco en un cobro.
 *
 * Kotlin puro a proposito: sin nada de Android, sus tests corren en la JVM en
 * milisegundos y el corpus de avisos reales se puede crecer sin emulador. Es la
 * pieza que mas va a cambiar durante la vida del producto, asi que es la que
 * mejor tiene que estar cubierta.
 */
class NoticeParser(templates: List<BankTemplate>) {

    /**
     * Compiladas y ordenadas por prioridad descendente: con dos plantillas
     * aplicables al mismo aviso gana la mas especifica.
     */
    private val porPaquete: Map<String, List<CompiledTemplate>> =
        templates
            .mapNotNull(CompiledTemplate::de)
            .flatMap { c -> c.origen.packageNames.map { it to c } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, lista) -> lista.sortedByDescending(CompiledTemplate::prioridad) }

    /**
     * Los paquetes que el listener debe aceptar. Todo lo demas se descarta en
     * memoria sin tocar disco.
     */
    val paquetesPermitidos: Set<String> = porPaquete.keys

    fun aceptaPaquete(sourcePackage: String): Boolean = sourcePackage in paquetesPermitidos

    fun parse(notice: RawNotice): ParseResult {
        val plantillas = porPaquete[notice.sourcePackage]
            ?: return ParseResult.NoEsCobro("paquete fuera de la lista blanca")

        if (notice.contenidoOculto) {
            return ParseResult.ContenidoOculto(notice.sourcePackage)
        }

        // Se acota el texto antes de aplicar cualquier patron. Los patrones
        // vienen del backend, y aunque el paquete va firmado, una regex con
        // retroceso explosivo sobre un texto largo colgaria el hilo de captura.
        val texto = notice.textoCompleto.take(MAX_CARACTERES)
        val titulo = notice.titulo.orEmpty().take(MAX_CARACTERES)

        for (plantilla in plantillas) {
            if (plantilla.excluir.any { it.containsMatchIn(texto) }) {
                return ParseResult.NoEsCobro("excluido por la plantilla de ${plantilla.wallet.id}")
            }

            if (plantilla.titulo.isNotEmpty() && plantilla.titulo.none { it.containsMatchIn(titulo) }) {
                continue
            }

            val match = plantilla.cuerpo.find(texto) ?: continue

            val montoTexto = plantilla.grupo(match, CompiledTemplate.GRUPO_MONTO) ?: continue
            val centavos = Dinero.aCentavos(montoTexto, plantilla.formatoMonto) ?: continue
            if (centavos <= 0L) continue

            val pagador = plantilla.grupo(match, CompiledTemplate.GRUPO_PAGADOR)
                ?.let(::limpiarNombre)
            val referencia = plantilla.grupo(match, CompiledTemplate.GRUPO_REFERENCIA)
                ?.let(::limpiarReferencia)

            return ParseResult.Cobro(
                wallet = plantilla.wallet,
                sourcePackage = notice.sourcePackage,
                amountCents = centavos,
                payerName = pagador,
                reference = referencia,
                notifPostedAtMillis = notice.postedAtMillis,
                capturedAtMillis = notice.capturedAtMillis,
                confidence = if (pagador != null && referencia != null) {
                    ParseConfidence.COMPLETA
                } else {
                    ParseConfidence.PARCIAL
                },
                rawText = texto,
            )
        }

        return ParseResult.NoReconocido(notice)
    }

    private companion object {
        /**
         * Tope del texto que se analiza. Un aviso de banco real no pasa de unos
         * cientos de caracteres; el limite existe para acotar el peor caso de
         * una regex, no por el tamano esperado.
         */
        const val MAX_CARACTERES = 4_000

        val ESPACIOS = Regex("""\s+""")
        val BORDES_SUCIOS = Regex("""^[\s\p{Punct}]+|[\s\p{Punct}]+$""")

        /**
         * Deja el nombre del pagador presentable y pronunciable: sin espacios
         * repetidos ni puntuacion de los bordes. Los enmascarados del propio
         * banco ("J*** P***") se dejan como vienen, porque son informacion real
         * y el comerciante los reconoce.
         */
        fun limpiarNombre(bruto: String): String? =
            bruto.replace(ESPACIOS, " ")
                .replace(BORDES_SUCIOS, "")
                .takeIf { it.isNotBlank() }

        /** La referencia se normaliza a mayusculas porque es lo que se compara
         *  contra el comprobante y contra la clave de deduplicacion. */
        fun limpiarReferencia(bruto: String): String? =
            bruto.replace(ESPACIOS, "")
                .replace(BORDES_SUCIOS, "")
                .uppercase()
                .takeIf { it.isNotBlank() }
    }
}

/** Construye el parser desde un paquete de plantillas ya verificado. */
fun NoticeParser(bundle: TemplateBundle): NoticeParser = NoticeParser(bundle.templates)
