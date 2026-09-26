package com.seef.checkqr.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.seef.checkqr.core.database.entidades.ComprobanteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ComprobanteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(comprobante: ComprobanteEntity)

    @Query("SELECT * FROM comprobantes ORDER BY captured_at_millis DESC LIMIT :cuantos")
    fun ultimos(cuantos: Int): Flow<List<ComprobanteEntity>>

    @Query("SELECT * FROM comprobantes WHERE id = :id")
    suspend fun porId(id: String): ComprobanteEntity?

    /** El texto del OCR tampoco tiene por que quedarse para siempre. */
    @Query("DELETE FROM comprobantes WHERE captured_at_millis < :antesDeMillis")
    suspend fun purgarAnterioresA(antesDeMillis: Long): Int
}
