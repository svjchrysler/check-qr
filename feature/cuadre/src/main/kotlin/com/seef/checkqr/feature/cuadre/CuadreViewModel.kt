package com.seef.checkqr.feature.cuadre

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seef.checkqr.core.common.Calendario
import com.seef.checkqr.core.common.Reloj
import com.seef.checkqr.core.data.repositorios.RepositorioDePagos
import com.seef.checkqr.core.data.repositorios.RepositorioDeTurnos
import com.seef.checkqr.core.model.Shift
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

@HiltViewModel
class CuadreViewModel @Inject constructor(
    private val pagos: RepositorioDePagos,
    private val turnos: RepositorioDeTurnos,
    private val exportador: Exportador,
    private val reloj: Reloj,
) : ViewModel() {

    private val _dia = MutableStateFlow(Calendario.diaDe(reloj.ahoraMillis()))
    val dia: StateFlow<LocalDate> = _dia

    @OptIn(ExperimentalCoroutinesApi::class)
    val cuadre: StateFlow<CuadreDelDia?> = _dia
        .flatMapLatest { d ->
            val inicio = Calendario.inicioDelDiaMillis(d)
            val fin = Calendario.finDelDiaMillis(d)
            // Los turnos del dia se traen junto con los pagos para poder
            // nombrarlos por su hora de apertura. Sin ellos el cuadre cae al
            // nombre por omision, que es un trozo del UUID y no le dice nada a
            // nadie.
            combine(pagos.enRango(inicio, fin), turnos.turnosEnRango(inicio, fin)) { lista, delDia ->
                val porId = delDia.associateBy(Shift::id)
                ArmadorDeCuadre.armar(
                    dia = d,
                    pagos = lista,
                    nombreDeTurno = { id -> etiquetaDeTurno(porId[id]) },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * "Turno de las 17:15", no "Turno a51d46d2".
     *
     * Un turno se reconoce por cuando empezo, que es como lo recuerda quien lo
     * trabajo. Si el turno no esta (un pago que quedo apuntando a un turno ya
     * borrado) queda el nombre generico antes que un identificador crudo.
     */
    private fun etiquetaDeTurno(turno: Shift?): String {
        val abierto = turno?.openedAtMillis ?: return "Turno"
        val hora = Calendario.horaDe(abierto)
        return "Turno de las %02d:%02d".format(hora.hour, hora.minute)
    }

    fun diaAnterior() {
        _dia.value = _dia.value.minus(1, DateTimeUnit.DAY)
    }

    /**
     * Avanza un dia, sin pasar de hoy.
     *
     * Manana no tiene pagos y nunca los va a tener, asi que avanzar al futuro
     * solo lleva a una sucesion de pantallas vacias. El tope se comprueba aqui
     * ademas de desactivar el boton: la UI puede quedarse abierta cruzando la
     * medianoche, y entonces "hoy" cambia debajo.
     */
    fun diaSiguiente() {
        val siguiente = _dia.value.plus(1, DateTimeUnit.DAY)
        if (siguiente <= Calendario.diaDe(reloj.ahoraMillis())) {
            _dia.value = siguiente
        }
    }

    /**
     * Prepara el archivo y devuelve el intent para compartirlo.
     *
     * Generar el PDF puede tardar con muchos pagos, asi que se hace en una
     * corrutina y se entrega el intent por callback, en vez de bloquear el hilo
     * principal durante el tap.
     */
    fun exportar(formato: FormatoDeExportacion, alTerminar: (Intent) -> Unit) {
        viewModelScope.launch {
            val actual = cuadre.value ?: return@launch
            alTerminar(exportador.exportar(actual, formato))
        }
    }
}
