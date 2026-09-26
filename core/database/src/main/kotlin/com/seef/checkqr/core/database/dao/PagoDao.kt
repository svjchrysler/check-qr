package com.seef.checkqr.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.seef.checkqr.core.database.entidades.PagoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PagoDao {

    /**
     * Inserta un pago descartando el duplicado.
     *
     * `IGNORE` sobre el indice unico de `dedup_key` es toda la deduplicacion: si
     * el banco reemite el mismo aviso, o dos hilos lo procesan a la vez, la
     * segunda insercion no hace nada y devuelve -1. No hay un "consultar y
     * despues insertar" que pueda perder la carrera.
     *
     * @return el rowId, o -1 si era un duplicado.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarSiEsNuevo(pago: PagoEntity): Long

    @Query("SELECT * FROM pagos WHERE id = :id")
    suspend fun porId(id: String): PagoEntity?

    @Query("SELECT * FROM pagos WHERE dedup_key = :dedupKey")
    suspend fun porDedupKey(dedupKey: String): PagoEntity?

    /**
     * Los pagos de un rango de tiempo, del mas nuevo al mas viejo.
     *
     * El rango es semiabierto `[desde, hasta)` para que dos dias consecutivos no
     * se solapen ni dejen un milisegundo afuera.
     */
    @Query(
        """
        SELECT * FROM pagos
        WHERE notif_posted_at_millis >= :desdeMillis
          AND notif_posted_at_millis < :hastaMillis
        ORDER BY notif_posted_at_millis DESC
        """,
    )
    fun enRango(desdeMillis: Long, hastaMillis: Long): Flow<List<PagoEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(amount_cents), 0) FROM pagos
        WHERE notif_posted_at_millis >= :desdeMillis
          AND notif_posted_at_millis < :hastaMillis
        """,
    )
    fun totalEnRango(desdeMillis: Long, hastaMillis: Long): Flow<Long>

    @Query("SELECT * FROM pagos ORDER BY notif_posted_at_millis DESC LIMIT :cuantos")
    fun ultimos(cuantos: Int): Flow<List<PagoEntity>>

    @Query("SELECT * FROM pagos WHERE shift_id = :shiftId ORDER BY notif_posted_at_millis DESC")
    fun porTurno(shiftId: String): Flow<List<PagoEntity>>

    // --- Reclamo de comprobante ---------------------------------------------

    /**
     * Marca el pago como reclamado, solo si nadie lo reclamo antes.
     *
     * El `AND claimed_by_receipt_id IS NULL` es lo que garantiza que un pago se
     * reclame una sola vez. Es una unica sentencia, asi que es atomica por si
     * misma: comprobarlo en Kotlin y despues escribir dejaria una ventana en la
     * que dos cajeros cobran el mismo aviso.
     *
     * @return 1 si el reclamo se hizo, 0 si ya estaba reclamado o no existe.
     */
    @Query(
        """
        UPDATE pagos
        SET claimed_by_receipt_id = :receiptId
        WHERE id = :pagoId AND claimed_by_receipt_id IS NULL
        """,
    )
    suspend fun reclamarSiEstaLibre(pagoId: String, receiptId: String): Int

    /**
     * Candidatos para casar un comprobante: mismo monto, sin reclamar y dentro
     * de una ventana de tiempo.
     */
    @Query(
        """
        SELECT * FROM pagos
        WHERE amount_cents = :amountCents
          AND claimed_by_receipt_id IS NULL
          AND notif_posted_at_millis >= :desdeMillis
          AND notif_posted_at_millis < :hastaMillis
        ORDER BY notif_posted_at_millis DESC
        """,
    )
    suspend fun candidatosPorMonto(
        amountCents: Long,
        desdeMillis: Long,
        hastaMillis: Long,
    ): List<PagoEntity>

    /** Busqueda por referencia, que cuando existe es la senal mas fuerte. */
    @Query(
        """
        SELECT * FROM pagos
        WHERE reference = :reference AND claimed_by_receipt_id IS NULL
        ORDER BY notif_posted_at_millis DESC
        """,
    )
    suspend fun candidatosPorReferencia(reference: String): List<PagoEntity>

    // --- Sincronizado --------------------------------------------------------

    @Query("SELECT * FROM pagos WHERE sync_state = :estadoId ORDER BY notif_posted_at_millis ASC LIMIT :cuantos")
    suspend fun porEstadoDeSync(estadoId: String, cuantos: Int): List<PagoEntity>

    @Query("UPDATE pagos SET sync_state = :estadoId WHERE id = :pagoId")
    suspend fun marcarEstadoDeSync(pagoId: String, estadoId: String)

    // --- Asignacion de turno -------------------------------------------------

    @Query("UPDATE pagos SET shift_id = :shiftId, cashier_id = :cashierId WHERE id = :pagoId")
    suspend fun asignarTurno(pagoId: String, shiftId: String?, cashierId: String?)

    // --- Retencion -----------------------------------------------------------

    /**
     * Borra el texto crudo de los pagos viejos, dejando el pago intacto.
     *
     * El texto solo sirve para depurar plantillas y puede traer el nombre del
     * pagador, asi que no tiene por que quedarse en el celular para siempre.
     */
    @Query("UPDATE pagos SET raw_text = NULL WHERE notif_posted_at_millis < :antesDeMillis AND raw_text IS NOT NULL")
    suspend fun purgarTextoCrudoAnteriorA(antesDeMillis: Long): Int
}
