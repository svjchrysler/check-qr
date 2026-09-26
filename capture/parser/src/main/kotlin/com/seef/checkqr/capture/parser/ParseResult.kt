package com.seef.checkqr.capture.parser

import com.seef.checkqr.core.model.ParseConfidence
import com.seef.checkqr.core.model.RawNotice
import com.seef.checkqr.core.model.Wallet

/** Lo que el parser saca de un aviso, sin decidir nada de persistencia. */
sealed interface ParseResult {

    /**
     * Un cobro. No es todavia un `Payment`: el id, el turno, el cajero y el
     * estado de sincronizado los pone la capa de datos, que es la que sabe en
     * que dispositivo y en que turno esta.
     */
    data class Cobro(
        val wallet: Wallet,
        val sourcePackage: String,
        val amountCents: Long,
        val payerName: String?,
        val reference: String?,
        val notifPostedAtMillis: Long,
        val capturedAtMillis: Long,
        val confidence: ParseConfidence,
        val rawText: String,
    ) : ParseResult

    /**
     * El aviso es de un banco de la lista pero no es un cobro entrante: un
     * "Enviaste Bs 50", un aviso de saldo, una promocion. Se descarta.
     */
    data class NoEsCobro(val motivo: String) : ParseResult

    /**
     * El aviso llego sin contenido legible.
     *
     * Desde Android 15 el sistema oculta a los listeners no confiables el texto
     * de los avisos con codigos OTP, y a veces alcanza a avisos que no lo son.
     * Esto no es un fallo del parser: la app tiene que mostrar "Revisa tu app
     * del banco" en vez de quedarse callada.
     */
    data class ContenidoOculto(val sourcePackage: String) : ParseResult

    /**
     * Ninguna plantilla reconocio el aviso. Es la senal de que un banco cambio
     * de formato: se guarda para corregir la plantilla, que es lo que evita
     * publicar una version nueva de la app.
     */
    data class NoReconocido(val notice: RawNotice) : ParseResult
}
