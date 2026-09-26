package com.seef.checkqr.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.seef.checkqr.core.database.entidades.MiembroEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MiembroDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(miembro: MiembroEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(miembros: List<MiembroEntity>)

    @Query("SELECT * FROM miembros WHERE activo = 1 ORDER BY nombre")
    fun activos(): Flow<List<MiembroEntity>>

    @Query("SELECT * FROM miembros WHERE id = :id")
    suspend fun porId(id: String): MiembroEntity?

    @Query("UPDATE miembros SET activo = 0 WHERE id = :id")
    suspend fun desactivar(id: String)
}
