package com.seef.checkqr.feature.verificar

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seef.checkqr.core.common.Reloj
import com.seef.checkqr.core.common.Veredicto
import com.seef.checkqr.core.data.repositorios.RepositorioDeComprobantes
import com.seef.checkqr.core.model.MatchResult
import com.seef.checkqr.core.model.Payment
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EstadoDeVerificacion {
    data object Esperando : EstadoDeVerificacion
    data object Leyendo : EstadoDeVerificacion
    data class Resuelto(
        val veredicto: Veredicto,
        /** True cuando el pago quedo efectivamente reclamado por este comprobante. */
        val reclamado: Boolean,
    ) : EstadoDeVerificacion

    data class Fallo(val motivo: String) : EstadoDeVerificacion
}

@HiltViewModel
class VerificarViewModel @Inject constructor(
    private val lector: LectorDeCapturas,
    private val comprobantes: RepositorioDeComprobantes,
    private val reloj: Reloj,
) : ViewModel() {

    private val _estado = MutableStateFlow<EstadoDeVerificacion>(EstadoDeVerificacion.Esperando)
    val estado: StateFlow<EstadoDeVerificacion> = _estado.asStateFlow()

    fun verificar(captura: Bitmap) {
        _estado.value = EstadoDeVerificacion.Leyendo
        viewModelScope.launch {
            try {
                val texto = lector.leer(captura)
                val resultado = comprobantes.verificar(texto, reloj.ahoraMillis())
                _estado.value = EstadoDeVerificacion.Resuelto(resultado.veredicto, resultado.reclamado)
            } catch (e: Exception) {
                _estado.value = EstadoDeVerificacion.Fallo(
                    "No se pudo leer la imagen. Prueba de nuevo con mejor luz.",
                )
            }
        }
    }

    /**
     * Resuelve una ambiguedad: la persona eligio cual de los pagos corresponde.
     *
     * El reclamo sigue pasando por la base con la condicion de que este libre, no
     * porque se desconfie del cajero, sino porque otro cajero pudo reclamarlo
     * entre que se mostro la lista y se toco la opcion.
     */
    fun elegir(pago: Payment) {
        viewModelScope.launch {
            val resultado = comprobantes.reclamarElegido(pago, reloj.ahoraMillis())
            _estado.value = EstadoDeVerificacion.Resuelto(resultado.veredicto, resultado.reclamado)
        }
    }

    fun reiniciar() {
        _estado.value = EstadoDeVerificacion.Esperando
    }
}
