package com.seef.checkqr.widget

import android.content.Context
import com.seef.checkqr.core.common.Dinero
import com.seef.checkqr.core.data.repositorios.RepositorioDePagos
import com.seef.checkqr.core.datastore.PreferenciasCheckQr
import com.seef.checkqr.core.model.Payment
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

/** Lo que el widget necesita para dibujarse. */
data class DatosDelWidget(
    val totalCentavos: Long,
    val cantidad: Int,
    val ultimos: List<Payment>,
    val cajaAbierta: Boolean,
    val listenerConectado: Boolean,
    val discreto: Boolean,
) {
    fun totalVisible(): String =
        if (discreto) WidgetDeCaja.MONTO_OCULTO else Dinero.formatear(totalCentavos)

    fun etiquetaDeEstado(): String = when {
        !listenerConectado -> "No está escuchando"
        cajaAbierta -> "Caja abierta"
        else -> "Caja cerrada"
    }

    /**
     * El widget se dibuja en el proceso de la app pero fuera de cualquier
     * Activity o Service con Hilt, asi que las dependencias se sacan por un
     * EntryPoint en vez de por inyeccion de campos.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencias {
        fun pagos(): RepositorioDePagos
        fun prefs(): PreferenciasCheckQr
    }

    companion object {
        suspend fun leer(context: Context): DatosDelWidget {
            val deps = EntryPointAccessors.fromApplication(
                context.applicationContext,
                Dependencias::class.java,
            )
            val pagos = deps.pagos()
            val prefs = deps.prefs()

            val delDia = pagos.pagosDeHoy().first()
            return DatosDelWidget(
                totalCentavos = delDia.sumOf(Payment::amountCents),
                cantidad = delDia.size,
                ultimos = delDia.take(3),
                cajaAbierta = prefs.vozHabilitada.first(),
                // null (nunca reporto) cuenta como escuchando: lo contrario
                // mostraria una falsa alarma en cada arranque.
                listenerConectado = prefs.listenerConectado.first() != false,
                discreto = prefs.modoDiscreto.first(),
            )
        }
    }
}
