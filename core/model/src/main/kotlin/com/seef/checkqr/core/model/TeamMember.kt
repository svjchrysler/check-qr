package com.seef.checkqr.core.model

/** Alguien del equipo del comercio. */
data class TeamMember(
    val id: String,
    val nombre: String,
    val role: Role,
    /** Null mientras la invitacion por QR no se acepta. */
    val cuentaGoogle: String?,
    val activo: Boolean,
)
