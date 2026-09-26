package com.seef.checkqr.core.model

import kotlinx.serialization.Serializable

/**
 * Cuanta confianza hay en que el pago realmente entro.
 *
 * El plan Basico solo produce [AVISO_BANCO] y [NO_LLEGO]; [CONFIRMADO_API] se
 * modela desde ya para no migrar el esquema al llegar el plan Pro.
 */
@Serializable
enum class PaymentLevel(val id: String) {
    /** 🟡 Lo dice el aviso de la app del banco. */
    AVISO_BANCO("aviso_banco"),

    /** 🔴 El cajero reclamo un comprobante que no corresponde a ningun aviso. */
    NO_LLEGO("no_llego"),

    /** 🟢 Confirmado contra la API del banco. Plan Pro. */
    CONFIRMADO_API("confirmado_api"),
    ;

    companion object {
        fun porId(id: String): PaymentLevel? = entries.firstOrNull { it.id == id }
    }
}
