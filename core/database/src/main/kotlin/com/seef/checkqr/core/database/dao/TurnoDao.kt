package com.seef.checkqr.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.seef.checkqr.core.database.entidades.TurnoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TurnoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(turno: TurnoEntity)

    /** El turno abierto de este dispositivo, si hay uno. */
    @Query("SELECT * FROM turnos WHERE closed_at_millis IS NULL AND device_id = :deviceId LIMIT 1")
    fun turnoAbierto(deviceId: String): Flow<TurnoEntity?>

    @Query("SELECT * FROM turnos WHERE closed_at_millis IS NULL AND device_id = :deviceId LIMIT 1")
    suspend fun turnoAbiertoAhora(deviceId: String): TurnoEntity?

    @Query("UPDATE turnos SET closed_at_millis = :cerradoEnMillis WHERE id = :turnoId AND closed_at_millis IS NULL")
    suspend fun cerrar(turnoId: String, cerradoEnMillis: Long): Int

    @Query(
        """
        SELECT * FROM turnos
        WHERE opened_at_millis >= :desdeMillis AND opened_at_millis < :hastaMillis
        ORDER BY opened_at_millis DESC
        """,
    )
    fun enRango(desdeMillis: Long, hastaMillis: Long): Flow<List<TurnoEntity>>

    @Query("SELECT * FROM turnos WHERE id = :id")
    suspend fun porId(id: String): TurnoEntity?
}
