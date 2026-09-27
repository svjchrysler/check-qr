package com.seef.checkqr.feature.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seef.checkqr.core.data.permisos.AccesoAAvisos
import com.seef.checkqr.core.data.repositorios.RepositorioDePagos
import com.seef.checkqr.core.datastore.PreferenciasCheckQr
import com.seef.checkqr.core.model.Wallet
import com.seef.checkqr.voice.EstadoDeVoz
import com.seef.checkqr.voice.LectorDeVoz
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Los pasos de la configuracion inicial, en orden.
 *
 * El orden no es estetico. [POR_QUE] va antes que cualquier dialogo del sistema
 * porque pedir acceso a las notificaciones sin explicar para que es la forma mas
 * rapida de que el comerciante diga que no y abandone. Y [PRUEBA] va al final
 * porque es lo unico que convierte "creo que funciona" en "sé que funciona".
 */
enum class PasoDelOnboarding {
    POR_QUE,
    PERMISO_DE_AVISOS,
    ACCESO_A_NOTIFICACIONES,
    BATERIA,
    VOZ,
    PRUEBA,
    LISTO,
}

data class UiOnboarding(
    val paso: PasoDelOnboarding = PasoDelOnboarding.POR_QUE,
    val accesoAAvisosConcedido: Boolean = false,
    val estadoDeVoz: EstadoDeVoz = EstadoDeVoz.SinIniciar,
    val guiaDeBateria: AjustesDeBateria.Guia? = null,
    /** Billeteras con las que ya se recibio un pago de prueba. */
    val billeterasProbadas: Set<Wallet> = emptySet(),
) {
    val todasLasBilleterasProbadas: Boolean
        get() = Wallet.soportadas.all { it in billeterasProbadas }

    val vozLista: Boolean get() = estadoDeVoz is EstadoDeVoz.Listo
    val faltaInstalarVoz: Boolean get() = estadoDeVoz is EstadoDeVoz.SinEspanol
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferenciasCheckQr,
    private val lector: LectorDeVoz,
    private val pagos: RepositorioDePagos,
    private val acceso: AccesoAAvisos,
) : ViewModel() {

    private val paso = MutableStateFlow(PasoDelOnboarding.POR_QUE)
    private val accesoConcedido = MutableStateFlow(false)

    val ui: StateFlow<UiOnboarding> = combine(
        paso,
        accesoConcedido,
        lector.estado,
        prefs.billeterasProbadas,
    ) { pasoActual, concedido, voz, probadas ->
        UiOnboarding(
            paso = pasoActual,
            accesoAAvisosConcedido = concedido,
            estadoDeVoz = voz,
            guiaDeBateria = AjustesDeBateria.para(context),
            billeterasProbadas = probadas.mapNotNull(Wallet::porId).toSet(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiOnboarding())

    init {
        lector.iniciar()
        refrescarAcceso()
        vigilarPagosDePrueba()
    }

    /**
     * Marca automaticamente la billetera cuando llega su primer pago.
     *
     * El comerciante no tiene que tocar nada: hace la transferencia de Bs 1 y ve
     * el tilde aparecer solo. Eso es lo que demuestra que la cadena entera
     * funciona para ese banco en ese celular.
     */
    private fun vigilarPagosDePrueba() = viewModelScope.launch {
        pagos.ultimos(50).collect { lista ->
            lista.map { it.wallet }.distinct().forEach { billetera ->
                if (billetera != Wallet.DESCONOCIDA) {
                    prefs.marcarBilleteraProbada(billetera.id)
                }
            }
        }
    }

    /** Vuelve a consultar el permiso al volver de los ajustes del sistema. */
    fun refrescarAcceso() {
        accesoConcedido.value = acceso.concedido()
    }

    fun intentDeAjustesDeAvisos() = acceso.intentDeAjustes()

    fun siguiente() {
        val orden = PasoDelOnboarding.entries
        val i = orden.indexOf(paso.value)
        if (i < orden.lastIndex) paso.value = orden[i + 1]
    }

    fun anterior() {
        val orden = PasoDelOnboarding.entries
        val i = orden.indexOf(paso.value)
        if (i > 0) paso.value = orden[i - 1]
    }

    /** Prueba la voz para que el comerciante la oiga antes de confiar en ella. */
    fun probarVoz() {
        lector.anunciar(
            "Así vas a escuchar los pagos. Recibiste cincuenta bolivianos de Juan Pérez por Yape.",
            id = "prueba-de-voz",
        )
    }

    fun terminar() = viewModelScope.launch {
        prefs.marcarOnboardingCompletado()
        paso.value = PasoDelOnboarding.LISTO
    }

    /** Permite seguir sin haber probado las 6 billeteras, pero dejando constancia. */
    fun saltarPrueba() = terminar()
}
