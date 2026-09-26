package com.seef.checkqr.core.model

/**
 * El comprobante que el cajero fotografia de la pantalla del cliente. El OCR
 * corre en el dispositivo; la foto no se sube a ningun lado.
 */
data class Receipt(
    val id: String,
    val capturedAtMillis: Long,
    val textoOcr: String,
    val amountCents: Long?,
    val reference: String?,
    /** Pago con el que casó, si casó. */
    val matchedPaymentId: String?,
    val resultado: MatchResult,
)

/** Veredicto de comparar un comprobante contra los pagos recibidos. */
enum class MatchResult(val id: String) {
    /** Un unico pago no reclamado calza. */
    COINCIDE("coincide"),

    /** Varios pagos calzan, o el OCR quedo incompleto: decide una persona. */
    AMBIGUO("ambiguo"),

    /** Ningun aviso corresponde: es el caso 🔴 "no llegó". */
    NO_LLEGO("no_llego"),

    /** El pago que calza ya fue reclamado por otro comprobante. */
    YA_RECLAMADO("ya_reclamado"),
    ;

    companion object {
        fun porId(id: String): MatchResult? = entries.firstOrNull { it.id == id }
    }
}
