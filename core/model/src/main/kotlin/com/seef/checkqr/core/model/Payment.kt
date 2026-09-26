package com.seef.checkqr.core.model

/**
 * Un pago recibido. El monto vive siempre en centavos enteros: ningun importe
 * pasa por Float ni por Double en toda la app.
 *
 * El tiempo va en milisegundos epoch, no en un tipo de fecha, porque es lo que
 * guarda Room y lo que trae el aviso; el calendario (dia, turno) se calcula en
 * :core:common con la zona de Bolivia.
 */
data class Payment(
    val id: String,
    /**
     * Clave de deduplicacion. Derivada, no aleatoria: es lo que impide que el
     * mismo aviso reemitido por el banco entre dos veces. La calcula
     * [dedupKeyDe] y la base la exige unica.
     */
    val dedupKey: String,
    val wallet: Wallet,
    val sourcePackage: String,
    val amountCents: Long,
    val currency: String = MONEDA_BOLIVIA,
    val payerName: String?,
    val reference: String?,
    val notifPostedAtMillis: Long,
    val capturedAtMillis: Long,
    val level: PaymentLevel,
    val confidence: ParseConfidence,
    val syncState: SyncState,
    val shiftId: String?,
    val cashierId: String?,
    /**
     * Id del comprobante que ya reclamo este pago. Un pago se reclama una sola
     * vez, y eso lo garantiza una transaccion que solo escribe si esto es null.
     */
    val claimedByReceiptId: String?,
    /**
     * Texto del aviso, solo para depurar plantillas. Se purga pasados unos dias
     * porque puede contener el nombre del pagador.
     */
    val rawText: String?,
) {
    val estaReclamado: Boolean get() = claimedByReceiptId != null

    companion object {
        const val MONEDA_BOLIVIA: String = "BOB"

        /**
         * Ventana de agrupacion del `postTime`, en milisegundos.
         *
         * Algunos bancos reemiten o actualizan el mismo aviso con un `postTime`
         * levemente distinto. Redondear a un bloque de 60 s hace que las dos
         * copias produzcan la misma clave y la segunda se descarte.
         *
         * 60 s es el punto de partida; hay que ajustarlo con avisos reales.
         */
        const val VENTANA_DEDUP_MILLIS: Long = 60_000L

        /**
         * Clave de deduplicacion: app de origen, monto, referencia y bloque de
         * tiempo. Sin la referencia (muchos avisos no la traen) quedan el monto
         * y el bloque, que es lo minimo que distingue dos cobros distintos.
         */
        fun dedupKeyDe(
            sourcePackage: String,
            amountCents: Long,
            reference: String?,
            postedAtMillis: Long,
            ventanaMillis: Long = VENTANA_DEDUP_MILLIS,
        ): String {
            val bloque = postedAtMillis / ventanaMillis
            val ref = reference?.trim()?.uppercase().orEmpty()
            return "$sourcePackage|$amountCents|$ref|$bloque"
        }
    }
}
