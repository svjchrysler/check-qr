package com.seef.checkqr.feature.cuadre

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seef.checkqr.core.common.Calendario
import com.seef.checkqr.core.common.Reloj
import com.seef.checkqr.core.data.repositorios.RepositorioDePagos
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

@HiltViewModel
class CuadreViewModel @Inject constructor(
    private val pagos: RepositorioDePagos,
    private val exportador: Exportador,
    reloj: Reloj,
) : ViewModel() {

    private val _dia = MutableStateFlow(Calendario.diaDe(reloj.ahoraMillis()))
    val dia: StateFlow<LocalDate> = _dia

    @OptIn(ExperimentalCoroutinesApi::class)
    val cuadre: StateFlow<CuadreDelDia?> = _dia
        .flatMapLatest { d ->
            pagos.enRango(
                Calendario.inicioDelDiaMillis(d),
                Calendario.finDelDiaMillis(d),
            ).map { lista -> ArmadorDeCuadre.armar(d, lista) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun diaAnterior() {
        _dia.value = _dia.value.minus(1, DateTimeUnit.DAY)
    }

    fun diaSiguiente() {
        _dia.value = _dia.value.plus(1, DateTimeUnit.DAY)
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
