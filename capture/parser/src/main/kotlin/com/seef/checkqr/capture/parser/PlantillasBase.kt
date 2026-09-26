package com.seef.checkqr.capture.parser

import com.seef.checkqr.core.model.TemplateBundle
import kotlinx.serialization.json.Json

/**
 * Las plantillas que van dentro del APK, como respaldo para cuando todavia no se
 * bajo ninguna del backend (primer arranque, o celular sin internet).
 *
 * ADVERTENCIA IMPORTANTE
 * ----------------------
 * Los nombres de paquete de este archivo **no estan verificados**. Se pusieron
 * como candidatos razonables para que la cadena completa se pueda desarrollar y
 * probar, pero hay que confirmarlos uno por uno contra celulares reales con las
 * 6 apps instaladas, porque la lista blanca es lo que decide si un aviso se lee
 * o se descarta: un paquete equivocado significa cero capturas para ese banco, y
 * en silencio.
 *
 * Como conseguir los reales:
 *  1. Instalar la variante `debug`, que registra **todos** los avisos.
 *  2. Hacerse un pago de Bs 1 con cada billetera.
 *  3. Abrir la pantalla de captura y exportar el JSON.
 *  4. Reemplazar aqui los `packages` y ajustar cada `patron_cuerpo` contra el
 *     texto literal, anadiendo cada aviso al corpus de
 *     `src/test/resources/notices/`.
 *
 * Alternativa por consola: `adb shell dumpsys notification --noredact`.
 *
 * Los patrones tampoco son definitivos: cubren las formas habituales del
 * espanol ("Recibiste Bs 50 de Juan Perez") y sirven de punto de partida.
 */
object PlantillasBase {

    private const val RECURSO = "/plantillas_base.json"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Cierto mientras las plantillas empaquetadas sigan sin verificarse contra
     * avisos reales. La pantalla de estado del sistema lo usa para advertir
     * durante el piloto, en vez de dejar creer que la captura esta lista.
     */
    val sinVerificar: Boolean get() = cargar().version == 0

    fun cargar(): TemplateBundle {
        val texto = checkNotNull(PlantillasBase::class.java.getResourceAsStream(RECURSO)) {
            "Falta $RECURSO en los recursos de :capture:parser"
        }.use { it.readBytes().decodeToString() }
        return json.decodeFromString(TemplateBundle.serializer(), texto)
    }
}
