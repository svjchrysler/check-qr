package com.seef.checkqr.capture.listener

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.seef.checkqr.core.common.Reloj
import com.seef.checkqr.core.data.captura.IngestorDePagos
import com.seef.checkqr.core.data.plantillas.RepositorioDePlantillas
import com.seef.checkqr.core.data.repositorios.RepositorioDeEstado
import com.seef.checkqr.core.model.RawNotice
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Lee los avisos de las apps de los bancos.
 *
 * Es la unica pieza de la app que ve notificaciones, y la que hace que la
 * captura funcione con la app cerrada: el sistema mantiene y reconecta este
 * servicio por su cuenta.
 *
 * Dos reglas que no se pueden relajar:
 *
 * 1. **El filtro por paquete es la primera sentencia de [onNotificationPosted].**
 *    Un aviso de una app que no esta en la lista de bancos se descarta en memoria
 *    sin tocar disco y sin que nada mas lo mire. No es un filtro entre otros: es
 *    la garantia que se le da al usuario y la que se declara ante Play.
 *
 * 2. **El callback no bloquea.** Es un callback del sistema y tiene que devolver
 *    de inmediato: se arma el aviso, se entrega a una corrutina y se vuelve.
 *
 * En `release` no existe ninguna clase capaz de grabar avisos de otras apps; ver
 * [RegistradorCrudo].
 */
@AndroidEntryPoint
class ServicioDeAvisosBancarios : NotificationListenerService() {

    @Inject lateinit var ingestor: IngestorDePagos
    @Inject lateinit var plantillas: RepositorioDePlantillas
    @Inject lateinit var estado: RepositorioDeEstado
    @Inject lateinit var registrador: RegistradorCrudo
    @Inject lateinit var reloj: Reloj

    /**
     * Ambito propio en lugar de `GlobalScope`: se cancela en [onDestroy], asi el
     * trabajo pendiente no sobrevive al servicio.
     */
    private val ambito = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Copia en memoria de la lista blanca, para poder filtrar sin suspender.
     *
     * El filtro tiene que ocurrir en el propio callback, y leer las plantillas es
     * una operacion suspendida. Se resuelve cargandola al conectar y dejandola
     * aqui. Si todavia no esta lista, no se descarta el aviso: se manda a la
     * corrutina, que ya puede suspender y consultar bien. Perder un pago por una
     * carrera de arranque seria peor que gastar una corrutina.
     */
    @Volatile
    private var listaBlanca: Set<String>? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        ambito.launch {
            listaBlanca = plantillas.parser().paquetesPermitidos
            estado.registrarLatidoListener(conectado = true)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        ambito.launch { estado.registrarLatidoListener(conectado = false) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val aviso = sbn ?: return
        val paquete = aviso.packageName ?: return

        // No se mira nada mas de un aviso ajeno. En debug hay una excepcion
        // explicita, mas abajo, que es la herramienta de captura de datos.
        val permitido = listaBlanca?.contains(paquete)
        if (permitido == false && !registrador.activo) return

        // Nunca el propio paquete: la notificacion de "Caja abierta" es nuestra.
        if (paquete == applicationContext.packageName) return

        val notice = aRawNotice(aviso, paquete)

        ambito.launch {
            if (registrador.activo) {
                registrador.registrar(notice, enListaBlanca = permitido ?: enListaBlancaAhora(paquete))
            }
            // Si la lista todavia no estaba cargada, aqui ya se puede consultar
            // bien: el propio parser vuelve a filtrar por paquete.
            ingestor.ingerir(notice)
        }
    }

    private suspend fun enListaBlancaAhora(paquete: String): Boolean =
        plantillas.parser().aceptaPaquete(paquete)

    private fun aRawNotice(aviso: StatusBarNotification, paquete: String): RawNotice {
        val extras: Bundle? = aviso.notification?.extras
        return RawNotice(
            sourcePackage = paquete,
            titulo = extras?.texto(Notification.EXTRA_TITLE),
            texto = extras?.texto(Notification.EXTRA_TEXT),
            // Muchos bancos ponen el detalle completo (monto, pagador,
            // referencia) solo en el texto expandido.
            textoLargo = extras?.texto(Notification.EXTRA_BIG_TEXT),
            subtexto = extras?.texto(Notification.EXTRA_SUB_TEXT),
            lineas = extras?.lineas(Notification.EXTRA_TEXT_LINES).orEmpty(),
            postedAtMillis = aviso.postTime,
            // No es lo mismo que postTime: si el celular estaba dormido, la app
            // puede ver el aviso bastante despues. El cuadre usa postTime.
            capturedAtMillis = reloj.ahoraMillis(),
        )
    }

    override fun onDestroy() {
        ambito.cancel()
        super.onDestroy()
    }

    private companion object {
        /** Los extras pueden traer CharSequence, SpannableString o nada. */
        fun Bundle.texto(clave: String): String? =
            getCharSequence(clave)?.toString()?.takeIf { it.isNotBlank() }

        fun Bundle.lineas(clave: String): List<String> =
            getCharSequenceArray(clave)
                ?.mapNotNull { it?.toString()?.takeIf { s -> s.isNotBlank() } }
                .orEmpty()
    }
}
