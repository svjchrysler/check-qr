package com.seef.checkqr.core.data.captura

import com.seef.checkqr.capture.parser.ParseResult
import com.seef.checkqr.core.common.Reloj
import com.seef.checkqr.core.data.plantillas.RepositorioDePlantillas
import com.seef.checkqr.core.database.dao.DiagnosticoDao
import com.seef.checkqr.core.database.dao.PagoDao
import com.seef.checkqr.core.database.dao.TurnoDao
import com.seef.checkqr.core.database.entidades.AvisoNoReconocidoEntity
import com.seef.checkqr.core.database.entidades.SenalPorBancoEntity
import com.seef.checkqr.core.database.entidades.aEntity
import com.seef.checkqr.core.datastore.PreferenciasCheckQr
import com.seef.checkqr.core.model.Payment
import com.seef.checkqr.core.model.PaymentLevel
import com.seef.checkqr.core.model.RawNotice
import com.seef.checkqr.core.model.SyncState
import com.seef.checkqr.core.model.Wallet
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * El nudo de la cadena de captura: aviso -> parser -> deduplicacion -> base ->
 * evento para la voz, el widget y el sync.
 *
 * Es `suspend` y no bloquea: quien lo llama es un callback del sistema
 * (`onNotificationPosted`) que tiene que devolver de inmediato, asi que entrega
 * el aviso y se va.
 *
 * Todo lo que decide aqui es idempotente respecto del mismo aviso: si el sistema
 * reentrega una notificacion, el resultado es [ResultadoDeIngesta.Duplicado] y no
 * un segundo cobro.
 */
@Singleton
class IngestorDePagos @Inject constructor(
    private val plantillas: RepositorioDePlantillas,
    private val pagoDao: PagoDao,
    private val turnoDao: TurnoDao,
    private val diagnosticoDao: DiagnosticoDao,
    private val prefs: PreferenciasCheckQr,
    private val bus: BusDeCaptura,
    private val reloj: Reloj,
) {

    suspend fun ingerir(notice: RawNotice): ResultadoDeIngesta {
        val parser = plantillas.parser()

        return when (val r = parser.parse(notice)) {
            is ParseResult.Cobro -> guardarCobro(r, notice.claveDelSistema)

            is ParseResult.ContenidoOculto -> {
                // Se registra la senal para que "Estado del sistema" pueda decir
                // "el ultimo aviso de este banco llego sin contenido".
                registrarSenal(r.sourcePackage, Wallet.DESCONOCIDA, oculto = true)
                bus.emitir(EventoDeCaptura.ContenidoOculto(r.sourcePackage))
                ResultadoDeIngesta.ContenidoOculto(r.sourcePackage)
            }

            is ParseResult.NoEsCobro -> ResultadoDeIngesta.Descartado(r.motivo)

            is ParseResult.NoReconocido -> {
                guardarNoReconocido(r.notice)
                bus.emitir(EventoDeCaptura.AvisoNoReconocido(r.notice.sourcePackage))
                ResultadoDeIngesta.NoReconocido(null)
            }
        }
    }

    private suspend fun guardarCobro(
        cobro: ParseResult.Cobro,
        claveDelSistema: String?,
    ): ResultadoDeIngesta {
        val dedupKey = Payment.dedupKeyDe(
            sourcePackage = cobro.sourcePackage,
            amountCents = cobro.amountCents,
            reference = cobro.reference,
            postedAtMillis = cobro.notifPostedAtMillis,
            claveDelSistema = claveDelSistema,
        )

        // El turno se resuelve al momento de la ingesta: un pago que llega con la
        // caja cerrada queda sin turno y el cuadre lo muestra aparte, en lugar de
        // colgarse del turno equivocado.
        val deviceId = prefs.deviceId()
        val turno = turnoDao.turnoAbiertoAhora(deviceId)

        val pago = Payment(
            id = UUID.randomUUID().toString(),
            dedupKey = dedupKey,
            wallet = cobro.wallet,
            sourcePackage = cobro.sourcePackage,
            amountCents = cobro.amountCents,
            payerName = cobro.payerName,
            reference = cobro.reference,
            notifPostedAtMillis = cobro.notifPostedAtMillis,
            capturedAtMillis = cobro.capturedAtMillis,
            level = PaymentLevel.AVISO_BANCO,
            confidence = cobro.confidence,
            syncState = SyncState.PENDIENTE,
            shiftId = turno?.id,
            cashierId = turno?.cashierId,
            claimedByReceiptId = null,
            rawText = cobro.rawText,
        )

        registrarSenal(cobro.sourcePackage, cobro.wallet, oculto = false)

        // La deduplicacion la resuelve el indice unico de la base. Si devuelve
        // -1, otro hilo o un aviso reemitido ya lo guardo.
        return if (pagoDao.insertarSiEsNuevo(pago.aEntity()) == FILA_IGNORADA) {
            bus.emitir(EventoDeCaptura.PagoDuplicado(dedupKey))
            ResultadoDeIngesta.Duplicado(dedupKey)
        } else {
            if (!prefs.vozHabilitada.first()) {
                // La caja esta cerrada: el pago se guarda igual y al reabrir se
                // anuncia el resumen, en lugar de soltar veinte anuncios seguidos.
                prefs.sumarPagoSinAnunciar()
            }
            bus.emitir(EventoDeCaptura.PagoNuevo(pago))
            ResultadoDeIngesta.Nuevo(pago)
        }
    }

    private suspend fun registrarSenal(sourcePackage: String, wallet: Wallet, oculto: Boolean) {
        diagnosticoDao.registrarSenal(
            SenalPorBancoEntity(
                sourcePackage = sourcePackage,
                walletId = wallet.id,
                ultimoAvisoMillis = reloj.ahoraMillis(),
                ultimoOculto = oculto,
            ),
        )
    }

    private suspend fun guardarNoReconocido(notice: RawNotice) {
        diagnosticoDao.guardarNoReconocido(
            AvisoNoReconocidoEntity(
                id = UUID.randomUUID().toString(),
                sourcePackage = notice.sourcePackage,
                titulo = notice.titulo,
                texto = notice.textoCompleto.take(MAX_TEXTO_GUARDADO),
                postedAtMillis = notice.postedAtMillis,
                // Nunca compartido por omision: el texto puede traer el nombre
                // de una persona, y mejorar nuestras plantillas no alcanza como
                // razon para enviarlo sin permiso explicito.
                compartido = false,
            ),
        )
    }

    private companion object {
        /** Lo que devuelve Room cuando `IGNORE` descarto la fila. */
        const val FILA_IGNORADA = -1L
        const val MAX_TEXTO_GUARDADO = 1_000
    }
}
