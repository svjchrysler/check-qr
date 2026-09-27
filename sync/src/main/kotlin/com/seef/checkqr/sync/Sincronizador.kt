package com.seef.checkqr.sync

import android.content.Context
import com.seef.checkqr.core.data.captura.BusDeCaptura
import com.seef.checkqr.core.data.captura.EventoDeCaptura
import com.seef.checkqr.core.datastore.PreferenciasCheckQr
import com.seef.checkqr.core.network.api.ApiDeCheckQr
import com.seef.checkqr.core.network.api.PeticionDispositivo
import com.seef.checkqr.sync.trabajos.TrabajoDePlantillas
import com.seef.checkqr.sync.trabajos.TrabajoDeSubidaDePagos
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Arranca el sincronizado y lo mantiene al dia.
 *
 * Todo lo que hace es en segundo plano y ninguna parte de la captura lo espera:
 * si el backend no existe o no responde, la caja sigue capturando, anunciando y
 * cuadrando igual. Esa es la propiedad que hace usable la app en un mostrador
 * con red intermitente.
 */
@Singleton
class Sincronizador @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bus: BusDeCaptura,
    private val prefs: PreferenciasCheckQr,
    private val api: ApiDeCheckQr,
) {
    private val ambito = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun empezar() {
        TrabajoDePlantillas.programar(context)
        registrarDispositivo()
        subirCuandoEntreUnPago()
    }

    /**
     * Sube cada pago nuevo.
     *
     * Se escucha el bus en vez de sondear la base: el trabajo se encola en el
     * momento y WorkManager decide cuando correrlo segun la red.
     */
    private fun subirCuandoEntreUnPago() = ambito.launch {
        bus.eventos.collect { evento ->
            if (evento is EventoDeCaptura.PagoNuevo) {
                TrabajoDeSubidaDePagos.encolar(context)
            }
        }
    }

    /**
     * Registra el celular y su token de push.
     *
     * Falla en silencio a proposito: sin sesion todavia no hay a donde
     * registrarse, y eso no es un error que deba molestar al comerciante.
     */
    private fun registrarDispositivo() = ambito.launch {
        val token = prefs.tokenDePush.first() ?: return@launch
        val deviceId = prefs.deviceId()
        runCatching { api.registrarDispositivo(PeticionDispositivo(deviceId, token)) }
    }
}
