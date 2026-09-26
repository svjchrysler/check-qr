package com.seef.checkqr.capture.parser

import com.seef.checkqr.core.model.BankTemplate

/**
 * Una plantilla con sus expresiones ya compiladas.
 *
 * Se compila una sola vez al cargar el paquete y no en cada aviso: compilar una
 * regex por notificacion seria trabajo repetido dentro de un callback del
 * sistema que no puede tardar.
 */
internal class CompiledTemplate(
    val origen: BankTemplate,
    val titulo: List<Regex>,
    val cuerpo: Regex,
    val excluir: List<Regex>,
    /**
     * Grupos con nombre que el patron declara de verdad.
     *
     * Hace falta porque pedirle a un `MatchResult` un grupo que el patron no
     * define lanza excepcion: hay que saber de antemano si preguntar.
     */
    val gruposDeclarados: Set<String>,
) {
    val wallet get() = origen.wallet
    val prioridad get() = origen.prioridad
    val formatoMonto get() = origen.amountFormat

    companion object {
        /** Nombres de grupo que el parser sabe usar. */
        const val GRUPO_MONTO = "monto"
        const val GRUPO_PAGADOR = "pagador"
        const val GRUPO_REFERENCIA = "referencia"

        /**
         * Trampa al escribir un `patron_cuerpo`: si el grupo `pagador` usa una
         * clase de caracteres amplia y el grupo `referencia` que lo sigue es
         * opcional, el pagador se traga la referencia entera, porque la regex
         * prefiere el match mas largo y el grupo opcional acepta quedar vacio.
         *
         * La forma correcta es cortar el pagador con un lookahead negativo
         * delante de las palabras que abren la referencia:
         *
         * ```
         * (?<pagador>(?:(?!\s*(?:ref|referencia)\b)[^.,\n]){2,60})
         * ```
         */

        private val NOMBRE_DE_GRUPO = Regex("""\(\?<([A-Za-z][A-Za-z0-9]*)>""")

        /** Las mismas opciones para todos los patrones, para que una plantilla
         *  no dependa de banderas embebidas. */
        private val OPCIONES = setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL,
        )

        /**
         * @return la plantilla compilada, o null si algun patron es invalido o
         *   le falta el grupo `monto`. Una plantilla mala se descarta sola sin
         *   tumbar las demas: viene del backend y un error de dedo alla no puede
         *   dejar al comerciante sin captura.
         */
        fun de(plantilla: BankTemplate): CompiledTemplate? {
            val grupos = NOMBRE_DE_GRUPO.findAll(plantilla.bodyPattern)
                .map { it.groupValues[1] }
                .toSet()
            if (GRUPO_MONTO !in grupos) return null

            return try {
                CompiledTemplate(
                    origen = plantilla,
                    titulo = plantilla.titlePatterns.map { Regex(it, OPCIONES) },
                    cuerpo = Regex(plantilla.bodyPattern, OPCIONES),
                    excluir = plantilla.excludePatterns.map { Regex(it, OPCIONES) },
                    gruposDeclarados = grupos,
                )
            } catch (e: IllegalArgumentException) {
                null // patron mal formado
            }
        }
    }

    /** Lee un grupo con nombre, o null si el patron no lo declara o no caso. */
    fun grupo(match: MatchResult, nombre: String): String? {
        if (nombre !in gruposDeclarados) return null
        return match.groups[nombre]?.value?.takeIf { it.isNotBlank() }
    }
}
