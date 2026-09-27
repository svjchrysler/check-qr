package com.seef.checkqr.sync.trabajos

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.seef.checkqr.core.data.repositorios.RepositorioDePagos
import com.seef.checkqr.core.datastore.PreferenciasCheckQr
import com.seef.checkqr.core.network.api.ApiDeCheckQr
import com.seef.checkqr.core.network.api.PeticionPago
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * Sube al backend los pagos que todavia no viajaron.
 *
 * No hay peticion de red en el camino de la captura: el pago se guarda en Room y
 * se anuncia por voz de inmediato, y este trabajo lo sube cuando hay red. Esa
 * separacion es lo que hace que la caja siga funcionando con el celular sin
 * internet, que es el caso normal en un mostrador.
 *
 * La subida es idempotente por `dedup_key`, asi que reintentar es seguro: si la
 * peticion en realidad llego y se corto la respuesta, el servidor responde
 * `nuevo=false` y no duplica nada.
 */
@HiltWorker
class TrabajoDeSubidaDePagos @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val pagos: RepositorioDePagos,
    private val prefs: PreferenciasCheckQr,
    private val api: ApiDeCheckQr,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pendientes = pagos.pendientesDeSubir(MAX_POR_TANDA)
        if (pendientes.isEmpty()) return Result.success()

        val deviceId = prefs.deviceId()
        var hubieronFallos = false

        for (pago in pendientes) {
            val peticion = PeticionPago(
                dedupKey = pago.dedupKey,
                wallet = pago.wallet.id,
                montoCentavos = pago.amountCents,
                moneda = pago.currency,
                pagador = pago.payerName,
                referencia = pago.reference,
                avisoEnMillis = pago.notifPostedAtMillis,
                nivel = pago.level.id,
                confianza = pago.confidence.id,
                turnoId = pago.shiftId,
                deviceId = deviceId,
            )

            val respuesta = runCatching { api.subirPago(peticion) }.getOrNull()

            when {
                respuesta == null -> hubieronFallos = true

                respuesta.isSuccessful -> {
                    // Tambien cuando el servidor dice `nuevo=false`: que el pago
                    // ya estuviera alla es exito, no error.
                    pagos.marcarSincronizado(pago.id)
                }

                // 4xx que no sea de autenticacion: el pago esta mal formado y
                // reintentarlo no lo va a arreglar nunca. Se marca para no
                // quedarse atascado subiendolo en cada ciclo para siempre.
                respuesta.code() in 400..499 && respuesta.code() != 401 -> {
                    pagos.marcarSincronizado(pago.id)
                }

                else -> hubieronFallos = true
            }
        }

        // `retry` y no `failure`: WorkManager lo reintenta con backoff cuando
        // vuelva la red, que es exactamente lo que se quiere.
        return if (hubieronFallos) Result.retry() else Result.success()
    }

    companion object {
        private const val MAX_POR_TANDA = 50
        private const val TRABAJO = "subida-de-pagos"

        /**
         * Encola la subida.
         *
         * `APPEND_OR_REPLACE` con nombre unico: si entran diez pagos seguidos no
         * se encolan diez trabajos, se hace uno que los sube todos.
         */
        fun encolar(context: Context) {
            val peticion = OneTimeWorkRequestBuilder<TrabajoDeSubidaDePagos>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(TRABAJO, ExistingWorkPolicy.APPEND_OR_REPLACE, peticion)
        }
    }
}
