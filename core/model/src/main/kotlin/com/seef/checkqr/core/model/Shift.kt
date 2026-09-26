package com.seef.checkqr.core.model

/**
 * Un turno de caja. Se abre con "Abrir caja" y se cierra con "Cerrar caja",
 * que es lo que dispara el cuadre.
 */
data class Shift(
    val id: String,
    val cashierId: String,
    val deviceId: String,
    val openedAtMillis: Long,
    val closedAtMillis: Long?,
) {
    val estaAbierto: Boolean get() = closedAtMillis == null
}
