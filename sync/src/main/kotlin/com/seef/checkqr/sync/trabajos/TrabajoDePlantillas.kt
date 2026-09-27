package com.seef.checkqr.sync.trabajos

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.seef.checkqr.core.data.plantillas.RepositorioDePlantillas
import com.seef.checkqr.core.datastore.PreferenciasCheckQr
import com.seef.checkqr.core.network.api.ApiDeCheckQr
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * Baja el paquete de plantillas firmado.
 *
 * Es la pieza que permite corregir un cambio de formato de un banco sin publicar
 * una version nueva de la app: se publica el paquete corregido y los celulares
 * lo recogen en el siguiente ciclo.
 *
 * La firma se verifica dentro de [RepositorioDePlantillas] **antes** de escribir
 * nada en disco. Aqui no se decide nada sobre confianza.
 */
@HiltWorker
class TrabajoDePlantillas @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val plantillas: RepositorioDePlantillas,
    private val prefs: PreferenciasCheckQr,
    private val api: ApiDeCheckQr,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val versionActual = prefs.versionDePlantillas.first()

        val respuesta = runCatching { api.plantillas(versionActual) }.getOrNull()
            ?: return Result.retry()

        // 204: el celular ya esta al dia. Es la respuesta normal casi siempre.
        if (respuesta.code() == 204) return Result.success()
        if (!respuesta.isSuccessful) return Result.retry()

        val firmado = respuesta.body() ?: return Result.success()

        // Si la firma no verifica se devuelve success, no retry: reintentar un
        // paquete mal firmado no lo va a arreglar, y la app se queda con las
        // plantillas que ya tiene, que es el comportamiento seguro.
        plantillas.guardarSiLaFirmaEsValida(firmado)
        return Result.success()
    }

    companion object {
        private const val TRABAJO = "sync-de-plantillas"

        /**
         * Una vez al dia y solo con red: las plantillas cambian cuando un banco
         * cambia su formato, que pasa unas pocas veces al ano. Consultar mas
         * seguido gastaria bateria sin ganar nada.
         */
        fun programar(context: Context) {
            val peticion = PeriodicWorkRequestBuilder<TrabajoDePlantillas>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                TRABAJO,
                ExistingPeriodicWorkPolicy.KEEP,
                peticion,
            )
        }
    }
}
