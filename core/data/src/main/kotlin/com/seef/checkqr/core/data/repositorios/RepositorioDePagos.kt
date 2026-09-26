package com.seef.checkqr.core.data.repositorios

import com.seef.checkqr.core.common.Calendario
import com.seef.checkqr.core.common.Reloj
import com.seef.checkqr.core.database.dao.PagoDao
import com.seef.checkqr.core.database.entidades.aDominio
import com.seef.checkqr.core.model.Payment
import com.seef.checkqr.core.model.SyncState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Lectura de pagos para la UI y el widget.
 *
 * Los rangos se calculan con el dia de Bolivia, no con el dia UTC: un pago de las
 * 21:00 en La Paz pertenece a ese dia comercial y no al siguiente.
 */
@Singleton
class RepositorioDePagos @Inject constructor(
    private val dao: PagoDao,
    private val reloj: Reloj,
) {

    /** Los pagos del dia comercial en curso, del mas nuevo al mas viejo. */
    fun pagosDeHoy(): Flow<List<Payment>> {
        val rango = Calendario.rangoDelDiaDe(reloj.ahoraMillis())
        return dao.enRango(rango.first, rango.last + 1).map { it.map { e -> e.aDominio() } }
    }

    /** Total cobrado hoy, en centavos. */
    fun totalDeHoy(): Flow<Long> {
        val rango = Calendario.rangoDelDiaDe(reloj.ahoraMillis())
        return dao.totalEnRango(rango.first, rango.last + 1)
    }

    fun ultimos(cuantos: Int): Flow<List<Payment>> =
        dao.ultimos(cuantos).map { it.map { e -> e.aDominio() } }

    fun deTurno(shiftId: String): Flow<List<Payment>> =
        dao.porTurno(shiftId).map { it.map { e -> e.aDominio() } }

    fun enRango(desdeMillis: Long, hastaMillis: Long): Flow<List<Payment>> =
        dao.enRango(desdeMillis, hastaMillis).map { it.map { e -> e.aDominio() } }

    suspend fun porId(id: String): Payment? = dao.porId(id)?.aDominio()

    // --- Sincronizado ---------------------------------------------------------

    suspend fun pendientesDeSubir(cuantos: Int): List<Payment> =
        dao.porEstadoDeSync(SyncState.PENDIENTE.id, cuantos).map { it.aDominio() }

    suspend fun marcarSincronizado(pagoId: String) =
        dao.marcarEstadoDeSync(pagoId, SyncState.SINCRONIZADO.id)

    // --- Retencion -------------------------------------------------------------

    /**
     * Borra el texto crudo de los avisos viejos. El pago queda; lo que se va es
     * el texto, que solo sirve para depurar plantillas y puede traer el nombre
     * del pagador.
     */
    suspend fun purgarTextoCrudo(diasDeRetencion: Int): Int =
        dao.purgarTextoCrudoAnteriorA(
            reloj.ahoraMillis() - diasDeRetencion * MILLIS_POR_DIA,
        )

    private companion object {
        const val MILLIS_POR_DIA = 24L * 60L * 60L * 1_000L
    }
}
