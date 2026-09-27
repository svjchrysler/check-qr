package com.seef.checkqr.feature.caja

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seef.checkqr.core.data.captura.BusDeCaptura
import com.seef.checkqr.core.data.captura.EventoDeCaptura
import com.seef.checkqr.core.data.repositorios.EstadoDelSistema
import com.seef.checkqr.core.data.repositorios.RepositorioDeEstado
import com.seef.checkqr.core.data.repositorios.RepositorioDePagos
import com.seef.checkqr.core.datastore.PreferenciasCheckQr
import com.seef.checkqr.core.model.Payment
import com.seef.checkqr.voice.ControladorDeCaja
import com.seef.checkqr.voice.EstadoDeCaja
import com.seef.checkqr.voice.EstadoDeVoz
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiCaja(
    val cargando: Boolean = true,
    val caja: EstadoDeCaja? = null,
    val pagosDeHoy: List<Payment> = emptyList(),
    val totalDeHoyCentavos: Long = 0L,
    val estadoDelSistema: EstadoDelSistema? = null,
    val modoDiscreto: Boolean = false,
    /**
     * Ultimo aviso que llego sin contenido legible.
     *
     * Se muestra explicitamente porque el sistema, desde Android 15, puede ocultar
     * el texto de un aviso a los listeners no confiables. Sin este cartel el
     * comerciante no tendria forma de saber que entro algo que no se pudo leer.
     */
    val avisoConContenidoOculto: String? = null,
)

@HiltViewModel
class CajaViewModel @Inject constructor(
    private val controlador: ControladorDeCaja,
    private val pagos: RepositorioDePagos,
    private val estado: RepositorioDeEstado,
    private val prefs: PreferenciasCheckQr,
    private val bus: BusDeCaptura,
) : ViewModel() {

    private val contenidoOculto = MutableStateFlow<String?>(null)

    val ui: StateFlow<UiCaja> = combine(
        controlador.estado(),
        pagos.pagosDeHoy(),
        pagos.totalDeHoy(),
        estado.estado(),
        combine(prefs.modoDiscreto, contenidoOculto) { d, o -> d to o },
    ) { caja, listaDePagos, total, sistema, (discreto, oculto) ->
        UiCaja(
            cargando = false,
            caja = caja,
            pagosDeHoy = listaDePagos,
            totalDeHoyCentavos = total,
            estadoDelSistema = sistema,
            modoDiscreto = discreto,
            avisoConContenidoOculto = oculto,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiCaja(),
    )

    init {
        // Al arrancar se comprueba si el celular se reinicio: en ese caso el
        // listener sigue capturando pero la voz quedo apagada, y hay que decirlo.
        viewModelScope.launch { controlador.comprobarReinicio() }

        viewModelScope.launch {
            bus.eventos.collect { evento ->
                if (evento is EventoDeCaptura.ContenidoOculto) {
                    contenidoOculto.value = evento.sourcePackage
                }
            }
        }
    }

    /**
     * Abre la caja.
     *
     * Solo se llama desde el tap del boton, con la pantalla visible: es lo que
     * Android 17 exige para que el servicio de voz pueda arrancar.
     */
    fun abrirCaja() = viewModelScope.launch {
        val cajero = prefs.cajeroActualId.first() ?: CAJERO_POR_OMISION
        controlador.abrirCaja(cajero)
    }

    fun cerrarCaja() = controlador.cerrarCaja()

    fun alternarDiscreto() = viewModelScope.launch {
        prefs.fijarModoDiscreto(!prefs.modoDiscreto.first())
    }

    fun descartarAvisoOculto() {
        contenidoOculto.value = null
    }

    private companion object {
        /**
         * Mientras no haya equipo configurado, el dueno es el cajero. Se reemplaza
         * cuando :feature:equipo cree miembros de verdad.
         */
        const val CAJERO_POR_OMISION = "dueno"
    }
}
