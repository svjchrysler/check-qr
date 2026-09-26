package com.seef.checkqr.voice

import android.content.Context
import com.seef.checkqr.core.data.repositorios.RepositorioDeEstado
import com.seef.checkqr.core.data.repositorios.RepositorioDeTurnos
import com.seef.checkqr.core.datastore.PreferenciasCheckQr
import com.seef.checkqr.core.model.Shift
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/** Lo que la pantalla de caja necesita saber. */
data class EstadoDeCaja(
    val turno: Shift?,
    val vozActiva: Boolean,
    val estadoDeVoz: EstadoDeVoz,
    /**
     * Cierto cuando hay un turno abierto en la base pero la voz no esta corriendo.
     *
     * Es el caso del celular reiniciado: el listener sigue capturando porque el
     * sistema lo reconecta solo, pero el servicio de voz no puede volver por su
     * cuenta. La pantalla tiene que mostrar "Toca para reactivar la voz" en vez de
     * quedarse muda sin explicacion.
     */
    val hayQueReactivarVoz: Boolean,
) {
    val cajaAbierta: Boolean get() = turno != null
    val faltaInstalarVoz: Boolean get() = estadoDeVoz is EstadoDeVoz.SinEspanol
}

/**
 * Abrir y cerrar caja.
 *
 * Es la unica puerta para arrancar el servicio de voz, y por eso es importante
 * donde se la llama: [abrir] tiene que invocarse desde una accion del usuario con
 * la app en pantalla. Con targetSdk 37, hacerlo desde cualquier otro sitio hace
 * que el audio falle sin ningun error visible.
 */
@Singleton
class ControladorDeCaja @Inject constructor(
    @ApplicationContext private val context: Context,
    private val turnos: RepositorioDeTurnos,
    private val estadoDelSistema: RepositorioDeEstado,
    private val prefs: PreferenciasCheckQr,
    private val lector: LectorDeVoz,
) {

    // El builder `flow` hace falta porque obtener el flujo del turno abierto es
    // una llamada suspendida: necesita el deviceId, que se lee de DataStore.
    fun estado(): Flow<EstadoDeCaja> = flow {
        val turnoAbierto = turnos.turnoAbierto()
        emitAll(
            combine(
                turnoAbierto,
                prefs.vozHabilitada,
                lector.estado,
            ) { turno, vozHabilitada, estadoVoz ->
                EstadoDeCaja(
                    turno = turno,
                    vozActiva = vozHabilitada && estadoVoz is EstadoDeVoz.Listo,
                    estadoDeVoz = estadoVoz,
                    // Turno abierto pero la voz no esta: hay que reactivarla a mano.
                    hayQueReactivarVoz = turno != null && !vozHabilitada,
                )
            },
        )
    }

    /**
     * Abre la caja: crea el turno y arranca el servicio de voz.
     *
     * **Llamar solo desde una accion del usuario con la app visible.**
     */
    suspend fun abrirCaja(cashierId: String): Shift {
        val turno = turnos.abrirCaja(cashierId)
        ServicioDeCaja.abrir(context)
        return turno
    }

    /** Cierra la caja: detiene la voz y dispara el cuadre del turno. */
    fun cerrarCaja() {
        ServicioDeCaja.cerrar(context)
    }

    /**
     * Comprueba si el celular se reinicio. Se llama al arrancar la app.
     *
     * @return true si hubo reinicio, con lo que la voz quedo apagada aunque el
     *   turno siga abierto en la base.
     */
    suspend fun comprobarReinicio(): Boolean {
        val hubo = estadoDelSistema.detectarReinicio()
        if (hubo) {
            // El servicio no sobrevivio al reinicio y no puede volver solo.
            prefs.fijarVozHabilitada(false)
        }
        return hubo
    }
}
