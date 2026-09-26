package com.seef.checkqr.core.database.entidades

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.seef.checkqr.core.model.Shift

@Entity(
    tableName = "turnos",
    indices = [Index(value = ["closed_at_millis"]), Index(value = ["cashier_id"])],
)
data class TurnoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "cashier_id") val cashierId: String,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "opened_at_millis") val openedAtMillis: Long,
    /** Null mientras la caja este abierta. */
    @ColumnInfo(name = "closed_at_millis") val closedAtMillis: Long?,
)

fun TurnoEntity.aDominio() = Shift(id, cashierId, deviceId, openedAtMillis, closedAtMillis)

fun Shift.aEntity() = TurnoEntity(id, cashierId, deviceId, openedAtMillis, closedAtMillis)
