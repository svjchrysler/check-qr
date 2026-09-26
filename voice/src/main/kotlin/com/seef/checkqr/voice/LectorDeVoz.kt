package com.seef.checkqr.voice

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** En que estado esta el motor de voz. */
sealed interface EstadoDeVoz {
    data object SinIniciar : EstadoDeVoz
    data object Iniciando : EstadoDeVoz

    /** Listo, con el idioma que finalmente se pudo usar. */
    data class Listo(val idioma: Locale) : EstadoDeVoz

    /**
     * El motor arranco pero no tiene espanol instalado. La app tiene que ofrecer
     * instalarlo, no fallar en silencio: el anuncio por voz es la razon de ser
     * del producto.
     */
    data object SinEspanol : EstadoDeVoz

    data class Fallo(val motivo: String) : EstadoDeVoz
}

/**
 * Anuncia los pagos por voz.
 *
 * Los importes ya vienen en palabras desde [com.seef.checkqr.core.common.LectorDeMontos]:
 * aqui solo se sintetiza. Eso hace que el anuncio no dependa de como el motor
 * instalado interprete "1.234,56", que varia entre versiones.
 *
 * Funciona sin internet siempre que el paquete de voz espanol este instalado en
 * el dispositivo; el onboarding se encarga de comprobarlo y ofrecer la descarga.
 */
@Singleton
class LectorDeVoz @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private var tts: TextToSpeech? = null

    private val _estado = MutableStateFlow<EstadoDeVoz>(EstadoDeVoz.SinIniciar)
    val estado: StateFlow<EstadoDeVoz> = _estado.asStateFlow()

    /** Cierto mientras haya algo sonando, para sostener el foco de audio. */
    private val _hablando = MutableStateFlow(false)
    val hablando: StateFlow<Boolean> = _hablando.asStateFlow()

    /**
     * Arranca el motor. Es asincrono: el estado pasa a [EstadoDeVoz.Listo] o a un
     * fallo a traves de [estado].
     */
    fun iniciar() {
        if (tts != null) return
        _estado.value = EstadoDeVoz.Iniciando

        val listener = OnInitListener { status ->
            if (status != TextToSpeech.SUCCESS) {
                _estado.value = EstadoDeVoz.Fallo("el motor de voz no pudo iniciarse")
                return@OnInitListener
            }
            configurar()
        }
        tts = TextToSpeech(context, listener)
    }

    private fun configurar() {
        val motor = tts ?: return

        val idioma = idiomaDisponible(motor)
        if (idioma == null) {
            _estado.value = EstadoDeVoz.SinEspanol
            return
        }
        motor.language = idioma

        // USAGE_MEDIA con CONTENT_TYPE_SPEECH: el anuncio sale por el volumen de
        // multimedia, que es el que el comerciante sube cuando hay ruido en el
        // local, y es coherente con el tipo mediaPlayback del servicio.
        motor.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )

        motor.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _hablando.value = true
            }

            override fun onDone(utteranceId: String?) {
                _hablando.value = false
            }

            @Deprecated("Lo pide la clase base; la version con codigo de error es la que se usa.")
            override fun onError(utteranceId: String?) {
                _hablando.value = false
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _hablando.value = false
                Log.w(TAG, "fallo al sintetizar '$utteranceId', codigo $errorCode")
            }
        })

        _estado.value = EstadoDeVoz.Listo(idioma)
    }

    /**
     * El primer idioma de la lista que el motor tenga, o null si no hay espanol.
     *
     * Se prueba `es-BO` primero porque es el acento del usuario, pero casi ningun
     * motor lo trae: la cadena de respaldo evita que la app se quede muda por
     * pedir una variante demasiado especifica.
     */
    private fun idiomaDisponible(motor: TextToSpeech): Locale? = IDIOMAS.firstOrNull {
        when (motor.isLanguageAvailable(it)) {
            TextToSpeech.LANG_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE,
            -> true
            else -> false
        }
    }

    /**
     * Encola un anuncio.
     *
     * Se encola con `QUEUE_ADD` y no `QUEUE_FLUSH`: si entran tres pagos
     * seguidos, el comerciante tiene que oir los tres, no solo el ultimo.
     */
    fun anunciar(texto: String, id: String) {
        val motor = tts ?: return
        if (_estado.value !is EstadoDeVoz.Listo) return
        motor.speak(texto, TextToSpeech.QUEUE_ADD, null, id)
    }

    /** Corta lo que este sonando. Se usa al cerrar la caja. */
    fun callar() {
        tts?.stop()
        _hablando.value = false
    }

    fun liberar() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _hablando.value = false
        _estado.value = EstadoDeVoz.SinIniciar
    }

    companion object {
        private const val TAG = "LectorDeVoz"

        /** De mas especifico a mas general. */
        private val IDIOMAS = listOf(
            Locale.Builder().setLanguage("es").setRegion("BO").build(),
            Locale.Builder().setLanguage("es").setRegion("US").build(),
            Locale.Builder().setLanguage("es").setRegion("MX").build(),
            Locale.Builder().setLanguage("es").setRegion("ES").build(),
            Locale.Builder().setLanguage("es").build(),
        )

        /**
         * Lleva al usuario a instalar los datos de voz.
         *
         * Se usa en el onboarding, despues de detectar [EstadoDeVoz.SinEspanol].
         * Sin esto la app se quedaria muda sin decir por que, que es justo el modo
         * de fallo que hay que evitar.
         */
        fun intentDeInstalarVoz(): Intent =
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
