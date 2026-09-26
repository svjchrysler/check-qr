package com.seef.checkqr.core.data.plantillas

import android.content.Context
import com.seef.checkqr.capture.parser.NoticeParser
import com.seef.checkqr.capture.parser.PlantillasBase
import com.seef.checkqr.capture.parser.VerificadorDePlantillas
import com.seef.checkqr.core.datastore.PreferenciasCheckQr
import com.seef.checkqr.core.model.SignedTemplateBundle
import com.seef.checkqr.core.model.TemplateBundle
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * De donde salen las plantillas con las que se leen los avisos.
 *
 * Hay dos fuentes, en este orden: el paquete firmado que se bajo del backend, y
 * si no hay ninguno o esta corrupto, las plantillas empaquetadas en el APK. La
 * app nunca se queda sin parser.
 *
 * El parser se construye una vez y se guarda: compilar seis expresiones
 * regulares en cada notificacion seria trabajo repetido dentro de un callback del
 * sistema que tiene que devolver rapido.
 */
@Singleton
class RepositorioDePlantillas @Inject constructor(
    @ApplicationContext private val context: Context,
    private val verificador: VerificadorDePlantillas,
    private val prefs: PreferenciasCheckQr,
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val candado = Mutex()

    @Volatile
    private var cache: NoticeParser? = null

    private val archivo: File
        get() = File(context.filesDir, ARCHIVO)

    /** El parser vigente. Se construye la primera vez que se pide. */
    suspend fun parser(): NoticeParser {
        cache?.let { return it }
        return candado.withLock {
            cache ?: construir().also { cache = it }
        }
    }

    /**
     * Guarda un paquete bajado del backend, **solo** si la firma es valida.
     *
     * La verificacion va antes de escribir en disco, nunca despues: las
     * plantillas son expresiones regulares que la app ejecuta para decidir que
     * aviso es un cobro y por cuanto. Quien pueda cambiarlas puede hacer que la
     * app anuncie pagos que no existen.
     *
     * @return el paquete aceptado, o null si la firma no corresponde.
     */
    suspend fun guardarSiLaFirmaEsValida(firmado: SignedTemplateBundle): TemplateBundle? {
        val verificado = verificador.verificar(firmado) ?: return null

        // Tampoco se acepta un paquete mas viejo que el que ya esta instalado:
        // evita que alguien reponga una version anterior con un fallo conocido.
        val versionActual = versionInstalada()
        if (verificado.version <= versionActual) return null

        return candado.withLock {
            try {
                archivo.parentFile?.mkdirs()
                val temporal = File(archivo.parentFile, "$ARCHIVO.tmp")
                temporal.writeText(json.encodeToString(TemplateBundle.serializer(), verificado))
                // Se reemplaza de golpe: si el proceso muere a mitad, queda el
                // paquete anterior completo y no un archivo truncado.
                if (!temporal.renameTo(archivo)) {
                    temporal.delete()
                    return@withLock null
                }
                prefs.fijarVersionDePlantillas(verificado.version)
                cache = NoticeParser(verificado)
                verificado
            } catch (e: IOException) {
                null
            }
        }
    }

    /** Version del paquete instalado, o 0 si solo estan las empaquetadas. */
    private suspend fun versionInstalada(): Int =
        prefs.versionDePlantillas.first()

    private fun construir(): NoticeParser {
        val bajado = leerDelDisco()
        val bundle = bajado ?: PlantillasBase.cargar()
        return NoticeParser(bundle)
    }

    private fun leerDelDisco(): TemplateBundle? {
        val f = archivo
        if (!f.exists()) return null
        return try {
            json.decodeFromString(TemplateBundle.serializer(), f.readText())
        } catch (e: Exception) {
            // Archivo corrupto: se descarta y se cae a las empaquetadas. Es
            // preferible leer los avisos con plantillas viejas que no leerlos.
            f.delete()
            null
        }
    }

    private companion object {
        const val ARCHIVO = "plantillas_firmadas.json"
    }
}
