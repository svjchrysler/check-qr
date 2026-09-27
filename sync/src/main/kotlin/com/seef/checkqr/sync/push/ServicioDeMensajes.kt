package com.seef.checkqr.sync.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.seef.checkqr.core.common.Reloj
import com.seef.checkqr.core.data.captura.BusDeCaptura
import com.seef.checkqr.core.data.captura.EventoDeCaptura
import com.seef.checkqr.core.database.dao.PagoDao
import com.seef.checkqr.core.database.entidades.aEntity
import com.seef.checkqr.core.datastore.PreferenciasCheckQr
import com.seef.checkqr.core.model.ParseConfidence
import com.seef.checkqr.core.model.Payment
import com.seef.checkqr.core.model.PaymentLevel
import com.seef.checkqr.core.model.SyncState
import com.seef.checkqr.core.model.Wallet
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Recibe los pagos que capturo el celular del dueno.
 *
 * Es lo que hace que el cajero oiga un cobro que entro en el telefono de otra
 * persona. Llega como **mensaje de datos**, sin bloque de notificacion, a
 * proposito: un push de notificacion lo mostraria el sistema directamente y la
 * app nunca se enteraria, asi que no podria guardarlo ni anunciarlo.
 */
@AndroidEntryPoint
class ServicioDeMensajes : FirebaseMessagingService() {

    @Inject lateinit var pagoDao: PagoDao
    @Inject lateinit var prefs: PreferenciasCheckQr
    @Inject lateinit var bus: BusDeCaptura
    @Inject lateinit var reloj: Reloj

    private val ambito = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(mensaje: RemoteMessage) {
        val datos = mensaje.data
        if (datos["tipo"] != TIPO_PAGO) return

        val dedupKey = datos["dedup_key"] ?: return
        val centavos = datos["monto_centavos"]?.toLongOrNull() ?: return
        val wallet = Wallet.porId(datos["wallet"].orEmpty()) ?: Wallet.DESCONOCIDA
        val avisoEn = datos["aviso_en"]?.toLongOrNull() ?: reloj.ahoraMillis()

        ambito.launch {
            val pago = Payment(
                id = UUID.randomUUID().toString(),
                dedupKey = dedupKey,
                wallet = wallet,
                sourcePackage = ORIGEN_REMOTO,
                amountCents = centavos,
                payerName = datos["pagador"],
                reference = datos["referencia"],
                notifPostedAtMillis = avisoEn,
                capturedAtMillis = reloj.ahoraMillis(),
                level = PaymentLevel.AVISO_BANCO,
                confidence = ParseConfidence.PARCIAL,
                // Llego por push: no hay que volver a subirlo.
                syncState = SyncState.RECIBIDO_REMOTO,
                shiftId = null,
                cashierId = null,
                claimedByReceiptId = null,
                rawText = null,
            )

            // La misma deduplicacion que la captura local: si el push se
            // reentrega, el pago no entra dos veces ni se anuncia dos veces.
            if (pagoDao.insertarSiEsNuevo(pago.aEntity()) != FILA_IGNORADA) {
                bus.emitir(EventoDeCaptura.PagoNuevo(pago))
            }
        }
    }

    /**
     * FCM rota los tokens. Si no se vuelve a registrar, el celular deja de
     * recibir pagos en silencio.
     */
    override fun onNewToken(token: String) {
        ambito.launch { prefs.fijarTokenDePush(token) }
    }

    override fun onDestroy() {
        ambito.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TIPO_PAGO = "pago"
        const val ORIGEN_REMOTO = "checkqr.remoto"
        const val FILA_IGNORADA = -1L
    }
}
