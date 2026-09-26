package com.seef.checkqr.core.data.repositorios

import com.seef.checkqr.core.common.Reloj
import com.seef.checkqr.core.database.dao.TurnoDao
import com.seef.checkqr.core.database.entidades.TurnoEntity
import com.seef.checkqr.core.database.entidades.aDominio
import com.seef.checkqr.core.datastore.PreferenciasCheckQr
import com.seef.checkqr.core.model.Shift
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * Abrir y cerrar caja.
 *
 * Un turno se abre siempre por una accion del usuario, nunca automaticamente:
 * ademas de ser lo que el comerciante espera, es lo que Android 17 exige para
 * poder arrancar el servicio en primer plano que hace hablar a la app.
 */
@Singleton
class RepositorioDeTurnos @Inject constructor(
    private val dao: TurnoDao,
    private val prefs: PreferenciasCheckQr,
    private val reloj: Reloj,
) {

    /** El turno abierto de este dispositivo, o null si la caja esta cerrada. */
    suspend fun turnoAbierto(): Flow<Shift?> {
        val deviceId = prefs.deviceId()
        return dao.turnoAbierto(deviceId).map { it?.aDominio() }
    }

    suspend fun turnoAbiertoAhora(): Shift? =
        dao.turnoAbiertoAhora(prefs.deviceId())?.aDominio()

    /**
     * Abre la caja. Si ya habia un turno abierto en este dispositivo devuelve
     * ese: abrir dos veces no debe crear dos turnos, porque partiria el cuadre.
     */
    suspend fun abrirCaja(cashierId: String): Shift {
        val deviceId = prefs.deviceId()
        dao.turnoAbiertoAhora(deviceId)?.let { return it.aDominio() }

        val turno = TurnoEntity(
            id = UUID.randomUUID().toString(),
            cashierId = cashierId,
            deviceId = deviceId,
            openedAtMillis = reloj.ahoraMillis(),
            closedAtMillis = null,
        )
        dao.guardar(turno)
        return turno.aDominio()
    }

    /**
     * Cierra la caja.
     *
     * @return el turno cerrado, o null si no habia ninguno abierto.
     */
    suspend fun cerrarCaja(): Shift? {
        val abierto = dao.turnoAbiertoAhora(prefs.deviceId()) ?: return null
        val cerrado = reloj.ahoraMillis()
        return if (dao.cerrar(abierto.id, cerrado) == 1) {
            abierto.copy(closedAtMillis = cerrado).aDominio()
        } else {
            null // alguien lo cerro entre medio
        }
    }

    fun turnosEnRango(desdeMillis: Long, hastaMillis: Long): Flow<List<Shift>> =
        dao.enRango(desdeMillis, hastaMillis).map { it.map { e -> e.aDominio() } }

    suspend fun porId(id: String): Shift? = dao.porId(id)?.aDominio()
}
