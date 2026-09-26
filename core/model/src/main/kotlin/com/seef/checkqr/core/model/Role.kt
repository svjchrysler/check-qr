package com.seef.checkqr.core.model

import kotlinx.serialization.Serializable

/** Roles del equipo, de mas a menos permisos. */
@Serializable
enum class Role(val id: String, val nombreVisible: String) {
    DUENO("dueno", "Dueño"),
    ENCARGADO("encargado", "Encargado"),
    CAJERO("cajero", "Cajero"),
    ;

    /** Solo el dueno invita gente y cambia roles. */
    val puedeGestionarEquipo: Boolean get() = this == DUENO

    /** Dueno y encargado ven el cuadre completo y lo exportan. */
    val puedeVerCuadreCompleto: Boolean get() = this != CAJERO

    /** Cualquiera abre y cierra su propia caja. */
    val puedeAbrirCaja: Boolean get() = true

    companion object {
        fun porId(id: String): Role? = entries.firstOrNull { it.id == id }
    }
}
