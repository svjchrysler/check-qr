package com.seef.checkqr.core.model

import kotlinx.serialization.Serializable

/** Estado del pago frente al backend. Room es la fuente de verdad. */
@Serializable
enum class SyncState(val id: String) {
    /** Solo existe en este celular. */
    PENDIENTE("pendiente"),

    /** Confirmado por el backend. */
    SINCRONIZADO("sincronizado"),

    /** Llego por push desde el celular del dueno; no hay que volver a subirlo. */
    RECIBIDO_REMOTO("recibido_remoto"),
    ;

    companion object {
        fun porId(id: String): SyncState? = entries.firstOrNull { it.id == id }
    }
}
