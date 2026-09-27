package com.seef.checkqr

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.seef.checkqr.sync.Sincronizador
import com.seef.checkqr.widget.RefrescoDelWidget
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Arranque de la app.
 *
 * Lo que se pone en marcha aqui son escuchas de eventos, no trabajo: el
 * sincronizado y el refresco del widget se suscriben al bus de captura y
 * reaccionan. Nada de esto esta en el camino critico de capturar un pago, que es
 * lo que permite que la caja funcione sin backend y sin red.
 */
@HiltAndroidApp
class CheckQrApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var sincronizador: Sincronizador
    @Inject lateinit var refrescoDelWidget: RefrescoDelWidget

    /**
     * WorkManager se inicializa a mano porque los workers se inyectan con Hilt.
     * El proveedor por omision esta desactivado en el manifest.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        sincronizador.empezar()
        refrescoDelWidget.empezar()
    }
}
