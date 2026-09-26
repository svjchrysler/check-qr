package com.seef.checkqr.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.seef.checkqr.core.common.LectorDeMontos
import com.seef.checkqr.core.data.captura.BusDeCaptura
import com.seef.checkqr.core.data.captura.EventoDeCaptura
import com.seef.checkqr.core.data.repositorios.RepositorioDeTurnos
import com.seef.checkqr.core.datastore.PreferenciasCheckQr
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * El servicio que hace hablar a la app mientras la caja esta abierta.
 *
 * Por que existe y por que funciona asi:
 *
 * En Android 17 reproducir audio o pedir el foco de audio sin una pantalla
 * visible exige un servicio en primer plano, y con targetSdk 37 ese servicio
 * tiene que haber sido iniciado por una accion del usuario o con la app visible.
 * Si no se cumple, el audio **falla en silencio, sin ningun error**. De ahi todo
 * el diseno:
 *
 * - Solo se arranca desde el boton "Abrir caja", con la app en pantalla. No hay
 *   ningun camino que lo inicie desde BOOT_COMPLETED ni desde el listener.
 * - Deja una notificacion fija "Caja abierta · escuchando pagos", con la accion
 *   "Cerrar caja".
 * - Si el celular se reinicia, el listener sigue capturando por su cuenta pero
 *   este servicio no vuelve solo: la app muestra "Toca para reactivar la voz".
 *
 * El tipo declarado es `mediaPlayback` y se mantiene una [MediaSession] activa
 * mientras la caja esta abierta, para que el sistema vea una reproduccion
 * legitima. Si Play lo objeta, el respaldo es `specialUse`; el manifest tiene el
 * bloque listo y comentado.
 */
@AndroidEntryPoint
class ServicioDeCaja : Service() {

    @Inject lateinit var lector: LectorDeVoz
    @Inject lateinit var bus: BusDeCaptura
    @Inject lateinit var turnos: RepositorioDeTurnos
    @Inject lateinit var prefs: PreferenciasCheckQr

    private val ambito = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var sesion: MediaSession? = null
    private var pedidoDeFoco: AudioFocusRequest? = null

    private val audioManager by lazy { getSystemService(AudioManager::class.java) }

    override fun onCreate() {
        super.onCreate()
        crearCanal()
        lector.iniciar()
        abrirSesionDeMedios()
        escucharPagos()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACCION_CERRAR -> {
                cerrarCajaYDetener()
                return START_NOT_STICKY
            }
        }

        // Entrar en primer plano de inmediato: el sistema da pocos segundos, y
        // pasarse significa ANR.
        ServiceCompat.startForeground(
            this,
            ID_NOTIFICACION,
            construirNotificacion(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            },
        )

        pedirFoco()
        ambito.launch { prefs.fijarVozHabilitada(true) }
        anunciarPendientes()

        // START_NOT_STICKY a proposito: si el sistema mata el servicio, NO debe
        // recrearlo solo. Un servicio recreado sin accion del usuario es
        // exactamente lo que Android 17 no permite, y volveria a fallar en
        // silencio. La app pide al usuario que vuelva a abrir la caja.
        return START_NOT_STICKY
    }

    /**
     * Anuncia el resumen de lo que entro con la caja cerrada, en lugar de soltar
     * veinte anuncios seguidos al abrir.
     */
    private fun anunciarPendientes() = ambito.launch {
        val cuantos = prefs.pagosSinAnunciar.first()
        if (cuantos > 0) {
            val texto = if (cuantos == 1) {
                "Mientras la caja estuvo cerrada llegó un pago"
            } else {
                "Mientras la caja estuvo cerrada llegaron " +
                    "${LectorDeMontos.numero(cuantos.toLong())} pagos"
            }
            lector.anunciar(texto, id = "resumen-apertura")
            prefs.limpiarPagosSinAnunciar()
        }
    }

    private fun escucharPagos() = ambito.launch {
        bus.eventos.collect { evento ->
            when (evento) {
                is EventoDeCaptura.PagoNuevo -> {
                    val texto = LectorDeMontos.anuncioDePago(
                        centavos = evento.pago.amountCents,
                        pagador = evento.pago.payerName,
                        billetera = evento.pago.wallet.nombreVisible,
                    )
                    lector.anunciar(texto, id = evento.pago.id)
                    actualizarNotificacion()
                }

                is EventoDeCaptura.ContenidoOculto -> {
                    // El sistema oculto el texto del aviso. No se puede decir el
                    // monto, pero callarse seria peor: el comerciante tiene que
                    // saber que entro algo y mirar la app del banco.
                    lector.anunciar(
                        "Llegó un aviso del banco pero no se pudo leer. Revisa tu app del banco.",
                        id = "oculto-${evento.sourcePackage}",
                    )
                }

                is EventoDeCaptura.PagoDuplicado,
                is EventoDeCaptura.AvisoNoReconocido,
                -> Unit // no se anuncian
            }
        }
    }

    // --- Sesion de medios y foco de audio ------------------------------------

    /**
     * Mantiene una sesion de medios activa mientras la caja esta abierta.
     *
     * No es adorno: legitima el tipo `mediaPlayback` del servicio frente a la
     * plataforma y frente a la revision de Play. Se usa la MediaSession de la
     * plataforma y no la de media3 porque esa exige una implementacion de Player
     * completa para algo que aqui solo necesita informar un estado.
     */
    private fun abrirSesionDeMedios() {
        sesion = MediaSession(this, "CheckQrCaja").apply {
            setPlaybackState(
                PlaybackState.Builder()
                    .setState(PlaybackState.STATE_PLAYING, 0L, 1f)
                    .setActions(PlaybackState.ACTION_STOP)
                    .build(),
            )
            isActive = true
        }
    }

    /**
     * Foco de audio transitorio con atenuacion: si el comerciante tiene musica o
     * la radio, baja el volumen para el anuncio y vuelve, en lugar de cortarla.
     */
    private fun pedirFoco() {
        val atributos = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val pedido = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(atributos)
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener { cambio ->
                // Si otra app toma el foco de forma permanente (una llamada, por
                // ejemplo), se corta el anuncio en curso. El pago ya esta en Room
                // y el widget lo muestra: no se pierde, solo no se oye.
                if (cambio == AudioManager.AUDIOFOCUS_LOSS) lector.callar()
            }
            .build()
        pedidoDeFoco = pedido
        audioManager?.requestAudioFocus(pedido)
    }

    private fun soltarFoco() {
        pedidoDeFoco?.let { audioManager?.abandonAudioFocusRequest(it) }
        pedidoDeFoco = null
    }

    // --- Notificacion fija ----------------------------------------------------

    private fun crearCanal() {
        val canal = NotificationChannel(
            CANAL,
            getString(R.string.canal_caja_abierta),
            NotificationManager.IMPORTANCE_LOW, // sin sonido: la voz ya avisa
        ).apply {
            description = getString(R.string.canal_caja_abierta_descripcion)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    private fun construirNotificacion(): Notification {
        val cerrar = PendingIntent.getService(
            this,
            0,
            Intent(this, ServicioDeCaja::class.java).setAction(ACCION_CERRAR),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CANAL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(R.string.caja_abierta_titulo))
            .setContentText(getString(R.string.caja_abierta_texto))
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, getString(R.string.cerrar_caja), cerrar)
            .build()
    }

    private fun actualizarNotificacion() {
        getSystemService(NotificationManager::class.java)
            .notify(ID_NOTIFICACION, construirNotificacion())
    }

    // --- Cierre ---------------------------------------------------------------

    private fun cerrarCajaYDetener() {
        ambito.launch {
            lector.callar()
            prefs.fijarVozHabilitada(false)
            // Cerrar la caja genera el cuadre del turno.
            turnos.cerrarCaja()
            ServiceCompat.stopForeground(this@ServicioDeCaja, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        soltarFoco()
        sesion?.isActive = false
        sesion?.release()
        sesion = null
        lector.callar()
        ambito.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CANAL = "caja_abierta"
        private const val ID_NOTIFICACION = 1001

        const val ACCION_ABRIR = "com.seef.checkqr.voice.ABRIR_CAJA"
        const val ACCION_CERRAR = "com.seef.checkqr.voice.CERRAR_CAJA"

        /**
         * Arranca el servicio.
         *
         * **Solo se puede llamar desde una Activity visible, por una accion del
         * usuario.** Con targetSdk 37, llamarlo desde otro sitio hace que el audio
         * falle sin ningun error visible.
         */
        fun abrir(context: Context) {
            val intent = Intent(context, ServicioDeCaja::class.java).setAction(ACCION_ABRIR)
            context.startForegroundService(intent)
        }

        fun cerrar(context: Context) {
            val intent = Intent(context, ServicioDeCaja::class.java).setAction(ACCION_CERRAR)
            context.startService(intent)
        }
    }
}
