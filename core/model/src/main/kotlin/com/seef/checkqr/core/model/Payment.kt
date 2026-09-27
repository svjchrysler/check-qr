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
         * Clave de deduplicacion: app de origen, monto, y lo mejor que haya
         * para distinguir dos cobros — la referencia, o la clave del aviso, o
         * el bloque de tiempo.
         *
         * El orden importa y responde a una asimetria: contar de mas produce un
         * pago repetido que el comerciante ve y corrige; contar de menos pierde
         * plata sin dejar rastro. Ante la duda, esta clave prefiere separar.
         *
         * - **Con referencia** basta el monto y la referencia. Es lo unico que
         *   identifica el cobro de verdad, y no depende del reloj: el mismo
         *   aviso reemitido media hora despues sigue siendo el mismo cobro.
         * - **Sin referencia pero con [claveDelSistema]**, se usa esa. Android
         *   conserva la clave cuando una app actualiza su notificacion y emite
         *   otra cuando publica una nueva, que es exactamente la distincion que
         *   hace falta. El bloque de tiempo acompaña para el caso del banco que
         *   reutiliza una sola notificacion para cobros sucesivos.
         * - **Sin nada de eso** queda el bloque de tiempo solo. Es el peor caso
         *   y el unico que puede fundir dos cobros distintos en uno: dos
         *   clientes pagando lo mismo, por la misma billetera, en el mismo
         *   minuto. En un puesto con cola no es raro.
         *
         * Los tres caminos hay que revisarlos con avisos reales; hoy ningun
         * banco esta verificado y no se sabe cuales traen referencia ni como
         * reemiten.
         */
        fun dedupKeyDe(
            sourcePackage: String,
            amountCents: Long,
            reference: String?,
            postedAtMillis: Long,
            claveDelSistema: String? = null,
            ventanaMillis: Long = VENTANA_DEDUP_MILLIS,
        ): String {
            val ref = reference?.trim()?.uppercase().orEmpty()
            if (ref.isNotEmpty()) return "$sourcePackage|$amountCents|ref=$ref"

            val bloque = postedAtMillis / ventanaMillis
            val clave = claveDelSistema?.trim().orEmpty()
            if (clave.isNotEmpty()) return "$sourcePackage|$amountCents|k=$clave|$bloque"

            return "$sourcePackage|$amountCents||$bloque"
        }
    }
}
