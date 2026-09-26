package com.seef.checkqr.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.seef.checkqr.core.database.entidades.AvisoNoReconocidoEntity
import com.seef.checkqr.core.database.entidades.CapturaCrudaDebugEntity
import com.seef.checkqr.core.database.entidades.SenalPorBancoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiagnosticoDao {

    // --- Avisos que ninguna plantilla reconocio ------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarNoReconocido(aviso: AvisoNoReconocidoEntity)

    @Query("SELECT * FROM avisos_no_reconocidos ORDER BY posted_at_millis DESC LIMIT :cuantos")
    fun noReconocidos(cuantos: Int): Flow<List<AvisoNoReconocidoEntity>>

    @Query("SELECT COUNT(*) FROM avisos_no_reconocidos WHERE source_package = :sourcePackage")
    suspend fun cuantosNoReconocidosDe(sourcePackage: String): Int

    @Query("DELETE FROM avisos_no_reconocidos WHERE posted_at_millis < :antesDeMillis")
    suspend fun purgarNoReconocidosAnterioresA(antesDeMillis: Long): Int

    // --- Estado del sistema ---------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun registrarSenal(senal: SenalPorBancoEntity)

    @Query("SELECT * FROM senal_por_banco")
    fun senales(): Flow<List<SenalPorBancoEntity>>

    // --- Volcado de depuracion (solo se escribe en la variante debug) --------

    @Insert
    suspend fun guardarCapturaCruda(captura: CapturaCrudaDebugEntity)

    @Query("SELECT * FROM captura_cruda_debug ORDER BY posted_at_millis DESC LIMIT :cuantos")
    fun capturasCrudas(cuantos: Int): Flow<List<CapturaCrudaDebugEntity>>

    /**
     * Los paquetes que emitieron avisos y NO estan en la lista blanca. Es la
     * consulta con la que se descubre el nombre real de la app de cada banco.
     */
    @Query(
        """
        SELECT DISTINCT source_package FROM captura_cruda_debug
        WHERE en_lista_blanca = 0
        ORDER BY source_package
        """,
    )
    fun paquetesFueraDeLaLista(): Flow<List<String>>

    @Query("SELECT * FROM captura_cruda_debug ORDER BY posted_at_millis DESC")
    suspend fun todasLasCapturasCrudas(): List<CapturaCrudaDebugEntity>

    @Query("DELETE FROM captura_cruda_debug")
    suspend fun borrarCapturasCrudas()
}
