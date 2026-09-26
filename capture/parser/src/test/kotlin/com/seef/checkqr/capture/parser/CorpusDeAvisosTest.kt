package com.seef.checkqr.capture.parser

import com.seef.checkqr.core.model.RawNotice
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Corre las plantillas empaquetadas contra un corpus de avisos guardados en
 * `src/test/resources/notices/`.
 *
 * Este corpus es el activo mas valioso del proyecto: cada aviso real que se
 * capture de un banco entra aqui como un caso, y a partir de ese momento
 * cualquier cambio de plantilla que rompa un banco que antes funcionaba falla en
 * la JVM en un segundo, sin emulador y sin transferencias de prueba.
 *
 * Como anadir un caso:
 *  1. Exportar el JSON desde la pantalla de captura de la variante `debug`.
 *  2. Guardarlo en `notices/<billetera>/<algo-descriptivo>.json` con la forma de
 *     [CasoDeAviso].
 *  3. Correr este test. Si falla, la plantilla es la que hay que corregir.
 */
class CorpusDeAvisosTest {

    @Serializable
    data class AvisoFixture(
        val sourcePackage: String,
        val titulo: String? = null,
        val texto: String? = null,
        val textoLargo: String? = null,
        val subtexto: String? = null,
        val lineas: List<String> = emptyList(),
        val postedAtMillis: Long = 1_700_000_000_000L,
        val capturedAtMillis: Long = 1_700_000_000_500L,
    ) {
        fun aRawNotice() = RawNotice(
            sourcePackage = sourcePackage,
            titulo = titulo,
            texto = texto,
            textoLargo = textoLargo,
            subtexto = subtexto,
            lineas = lineas,
            postedAtMillis = postedAtMillis,
            capturedAtMillis = capturedAtMillis,
        )
    }

    @Serializable
    data class Esperado(
        /** "cobro", "no_es_cobro", "contenido_oculto" o "no_reconocido". */
        val tipo: String,
        val wallet: String? = null,
        val amountCents: Long? = null,
        val payerName: String? = null,
        val reference: String? = null,
        val confidence: String? = null,
    )

    @Serializable
    data class CasoDeAviso(
        val descripcion: String,
        /** True si el aviso salio de un celular real, false si es sintetico. */
        val real: Boolean = false,
        val aviso: AvisoFixture,
        val esperado: Esperado,
    )

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val parser = NoticeParser(PlantillasBase.cargar())

    @Test
    fun `cada aviso del corpus produce el resultado esperado`() {
        val casos = cargarCasos()
        val fallos = mutableListOf<String>()

        casos.forEach { (archivo, caso) ->
            val obtenido = parser.parse(caso.aviso.aRawNotice())
            verificar(archivo, caso, obtenido)?.let(fallos::add)
        }

        assertTrue(
            "Casos del corpus que no dan el resultado esperado:\n" + fallos.joinToString("\n"),
            fallos.isEmpty(),
        )
    }

    @Test
    fun `informe del corpus`() {
        val casos = cargarCasos()
        val reales = casos.count { it.second.real }
        println(
            "Corpus de avisos: ${casos.size} casos, $reales de celulares reales, " +
                "${casos.size - reales} sinteticos.",
        )
        if (reales == 0) {
            println(
                "AVISO: todavia no hay ni un aviso real. Las plantillas y los " +
                    "nombres de paquete de plantillas_base.json siguen sin verificar, " +
                    "asi que la captura NO se puede considerar lista para el piloto.",
            )
        }
    }

    /** @return el mensaje del fallo, o null si el caso paso. */
    private fun verificar(archivo: String, caso: CasoDeAviso, obtenido: ParseResult): String? {
        val etiqueta = "$archivo (${caso.descripcion})"
        return when (caso.esperado.tipo) {
            "cobro" -> {
                val cobro = obtenido as? ParseResult.Cobro
                    ?: return "$etiqueta: se esperaba un cobro y salio $obtenido"
                val e = caso.esperado
                buildList {
                    e.wallet?.let {
                        if (cobro.wallet.name != it) add("wallet ${cobro.wallet.name} != $it")
                    }
                    e.amountCents?.let {
                        if (cobro.amountCents != it) add("monto ${cobro.amountCents} != $it")
                    }
                    if (e.payerName != null && cobro.payerName != e.payerName) {
                        add("pagador '${cobro.payerName}' != '${e.payerName}'")
                    }
                    if (e.reference != null && cobro.reference != e.reference) {
                        add("referencia '${cobro.reference}' != '${e.reference}'")
                    }
                    e.confidence?.let {
                        if (cobro.confidence.name != it) add("confianza ${cobro.confidence.name} != $it")
                    }
                }.takeIf { it.isNotEmpty() }?.let { "$etiqueta: ${it.joinToString("; ")}" }
            }
            "no_es_cobro" -> if (obtenido is ParseResult.NoEsCobro) null
            else "$etiqueta: se esperaba NoEsCobro y salio $obtenido"
            "contenido_oculto" -> if (obtenido is ParseResult.ContenidoOculto) null
            else "$etiqueta: se esperaba ContenidoOculto y salio $obtenido"
            "no_reconocido" -> if (obtenido is ParseResult.NoReconocido) null
            else "$etiqueta: se esperaba NoReconocido y salio $obtenido"
            else -> "$etiqueta: tipo esperado desconocido '${caso.esperado.tipo}'"
        }
    }

    private fun cargarCasos(): List<Pair<String, CasoDeAviso>> {
        val raiz = File(javaClass.getResource("/notices")?.toURI() ?: return emptyList())
        return raiz.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .sortedBy { it.path }
            .map { f ->
                val nombre = f.toRelativeString(raiz)
                nombre to json.decodeFromString(CasoDeAviso.serializer(), f.readText())
            }
            .toList()
    }

    // --- El paquete empaquetado tiene que ser valido -------------------------

    @Test
    fun `las plantillas empaquetadas cargan y compilan`() {
        val bundle = PlantillasBase.cargar()
        assertEquals(6, bundle.templates.size)

        // Cada billetera soportada tiene al menos una plantilla.
        val cubiertas = bundle.templates.map { it.wallet }.toSet()
        assertEquals(
            com.seef.checkqr.core.model.Wallet.soportadas.toSet(),
            cubiertas,
        )

        // Y todas compilan: ninguna se cae por un patron mal escrito.
        val p = NoticeParser(bundle)
        val paquetesDeclarados = bundle.templates.flatMap { it.packageNames }.toSet()
        assertEquals(
            "alguna plantilla no compilo y su paquete quedo fuera de la lista blanca",
            paquetesDeclarados,
            p.paquetesPermitidos,
        )
    }

    @Test
    fun `las plantillas empaquetadas se marcan como sin verificar`() {
        // Cuando se confirmen los paquetes reales contra celulares, hay que
        // subir la version del paquete y este test cambia de expectativa.
        assertTrue(
            "plantillas_base.json ya no esta en version 0: actualizar este test " +
                "y quitar el aviso del onboarding",
            PlantillasBase.sinVerificar,
        )
    }
}
