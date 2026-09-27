package com.seef.checkqr.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.seef.checkqr.core.data.captura.BusDeCaptura
import com.seef.checkqr.core.data.captura.EventoDeCaptura

/**
 * Refresca el widget cuando entra un pago.
 *
 * Escucha el mismo bus que la voz, en vez de sondear la base con un worker
 * periodico: el widget tiene que mostrar el pago en el momento, y un worker con
 * el intervalo minimo de WorkManager llegaria quince minutos tarde.
 */
@Singleton
class RefrescoDelWidget @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bus: BusDeCaptura,
) {
    private val ambito = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun empezar() {
        ambito.launch {
            bus.eventos.collect { evento ->
                when (evento) {
                    is EventoDeCaptura.PagoNuevo,
                    is EventoDeCaptura.ContenidoOculto,
                    -> refrescar()
                    else -> Unit
                }
            }
        }
    }

    suspend fun refrescar() {
        WidgetDeCaja().updateAll(context)
    }
}
