package com.seef.checkqr.core.model

import kotlinx.serialization.Serializable

/**
 * Que tan completo quedo el parseo del aviso. Un pago con [PARCIAL] se cuenta
 * y se anuncia igual, pero la pantalla lo marca para que el dueno lo revise.
 */
@Serializable
enum class ParseConfidence(val id: String) {
    /** Monto, pagador y referencia extraidos. */
    COMPLETA("completa"),

    /** Monto seguro, pero falta el pagador o la referencia. */
    PARCIAL("parcial"),
    ;

    companion object {
        fun porId(id: String): ParseConfidence? = entries.firstOrNull { it.id == id }
    }
}
