package com.seef.checkqr.core.database.entidades

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.seef.checkqr.core.model.Role
import com.seef.checkqr.core.model.TeamMember

@Entity(tableName = "miembros")
data class MiembroEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    @ColumnInfo(name = "role") val roleId: String,
    @ColumnInfo(name = "cuenta_google") val cuentaGoogle: String?,
    val activo: Boolean,
)

fun MiembroEntity.aDominio() = TeamMember(
    id = id,
    nombre = nombre,
    role = Role.porId(roleId) ?: Role.CAJERO,
    cuentaGoogle = cuentaGoogle,
    activo = activo,
)

fun TeamMember.aEntity() = MiembroEntity(id, nombre, role.id, cuentaGoogle, activo)
