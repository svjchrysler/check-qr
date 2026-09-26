package com.seef.checkqr.core.data.captura

import com.seef.checkqr.core.model.Payment
import com.seef.checkqr.core.model.Wallet
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Lo que paso con un aviso, para que la voz, el widget y el sync reaccionen. */
sealed interface EventoDeCaptura {

    /** Un cobro nuevo. Es lo que dispara el anuncio y el refresco del widget. */
    data class PagoNuevo(val pago: Payment) : EventoDeCaptura

    /** El mismo cobro llego dos veces: no se anuncia ni se cuenta otra vez. */
    data class PagoDuplicado(val dedupKey: String) : EventoDeCaptura

    /**
     * Llego un aviso de un banco pero sin contenido legible.
     *
     * La UI muestra "Revisa tu app del banco". Es importante que sea un evento
     * visible: el modo de fallo por omision de Android 15 en adelante es el
     * silencio, y un comerciante que no sabe que perdio un aviso es peor que uno
     * que ve un error.
     */
    data class ContenidoOculto(val sourcePackage: String) : EventoDeCaptura

    /**
     * Ninguna plantilla reconocio el aviso: el banco cambio de formato.
     */
    data class AvisoNoReconocido(val sourcePackage: String) : EventoDeCaptura
}

/**
 * Bus de eventos de captura.
 *
 * `extraBufferCapacity` con `DROP_OLDEST` y no un canal sin limite: si por lo que
 * sea nadie esta consumiendo, es mejor perder el evento mas viejo que acumular
 * memoria dentro de un callback del sistema. El pago en si ya quedo en Room, que
 * es la fuente de verdad; esto es solo la notificacion en vivo.
 */
@Singleton
class BusDeCaptura @Inject constructor() {

    private val _eventos = MutableSharedFlow<EventoDeCaptura>(
        replay = 0,
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val eventos: SharedFlow<EventoDeCaptura> = _eventos.asSharedFlow()

    fun emitir(evento: EventoDeCaptura) {
        _eventos.tryEmit(evento)
    }
}

/** Resultado de procesar un aviso, para quien lo entrego. */
sealed interface ResultadoDeIngesta {
    data class Nuevo(val pago: Payment) : ResultadoDeIngesta
    data class Duplicado(val dedupKey: String) : ResultadoDeIngesta
    data class Descartado(val motivo: String) : ResultadoDeIngesta
    data class ContenidoOculto(val sourcePackage: String) : ResultadoDeIngesta
    data class NoReconocido(val wallet: Wallet?) : ResultadoDeIngesta
}
