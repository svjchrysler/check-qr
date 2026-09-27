package com.seef.checkqr.capture.listener.debugtools

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.seef.checkqr.core.common.Reloj
import com.seef.checkqr.core.data.captura.IngestorDePagos
import com.seef.checkqr.core.data.captura.ResultadoDeIngesta
import com.seef.checkqr.core.model.RawNotice
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Inyecta un aviso falso para ejercitar toda la cadena de captura sin hacer una
 * transferencia real.
 *
 * Es lo que hace posible desarrollar el resto de la app: sin esto, cada prueba de
 * la voz, del widget o del cuadre necesitaria que alguien mande dinero de verdad
 * desde otro celular.
 *
 * Solo existe en la variante `debug`.
 *
 * Uso:
 * ```
 * adb shell "am broadcast -a com.seef.checkqr.INYECTAR_AVISO \
 *   -n com.seef.checkqr.debug/com.seef.checkqr.capture.listener.debugtools.InyectorDeAvisos \
 *   --es paquete com.bcp.innovacxion.yapeapp \
 *   --es titulo Yape \
 *   --es texto 'Recibiste Bs 50,00 de Juan Perez'"
 * ```
 *
 * Dos detalles que cuestan un rato si no se saben:
 * - La accion es `com.seef.checkqr.INYECTAR_AVISO`, **sin** el `.debug` que
 *   lleva el applicationId. El componente si lo lleva.
 * - Todo el comando va entre comillas dobles y los valores con espacios entre
 *   simples. Si no, el shell del dispositivo parte el texto y `am` se queda con
 *   la primera palabra.
 *
 * Y para ver que decidio el parser:
 * ```
 * adb logcat -s InyectorDeAvisos
 * ```
 */
@AndroidEntryPoint
class InyectorDeAvisos : BroadcastReceiver() {

    @Inject lateinit var ingestor: IngestorDePagos
    @Inject lateinit var reloj: Reloj

    private val ambito = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACCION) {
            // Devolver en silencio deja al que prueba mirando un `result=0` sin
            // pagos y sin ninguna pista. El error tipico es escribir la accion
            // con el `.debug` del applicationId.
            Log.w(TAG, "Accion '${intent.action}' ignorada; se esperaba '$ACCION'")
            return
        }

        val paquete = intent.getStringExtra("paquete")
        if (paquete.isNullOrBlank()) {
            Log.w(TAG, "Falta --es paquete <nombre.del.paquete>")
            return
        }

        val ahora = reloj.ahoraMillis()
        val notice = RawNotice(
            sourcePackage = paquete,
            titulo = intent.getStringExtra("titulo"),
            texto = intent.getStringExtra("texto"),
            textoLargo = intent.getStringExtra("textoLargo"),
            subtexto = intent.getStringExtra("subtexto"),
            // Permite probar la deduplicacion: dos inyecciones con el mismo
            // postedAt dentro de la ventana deben producir un solo pago.
            postedAtMillis = intent.getLongExtra("postedAt", ahora),
            capturedAtMillis = ahora,
            // `--es clave` simula la identidad que el sistema le da al aviso, y
            // es lo que permite probar aqui los dos caminos: con la misma clave
            // se deduplica, con claves distintas son dos cobros. Sin el extra
            // queda nulo, como cualquier aviso que no venga del sistema.
            claveDelSistema = intent.getStringExtra("clave"),
        )

        val pendiente = goAsync()
        ambito.launch {
            try {
                val r = ingestor.ingerir(notice)
                Log.i(TAG, describir(r))
            } finally {
                pendiente.finish()
            }
        }
    }

    private fun describir(r: ResultadoDeIngesta): String = when (r) {
        is ResultadoDeIngesta.Nuevo ->
            "PAGO NUEVO: ${r.pago.wallet.nombreVisible} ${r.pago.amountCents} centavos " +
                "de ${r.pago.payerName ?: "(sin pagador)"} ref=${r.pago.reference ?: "-"} " +
                "confianza=${r.pago.confidence.id}"
        is ResultadoDeIngesta.Duplicado ->
            "DUPLICADO, no se cuenta dos veces: ${r.dedupKey}"
        is ResultadoDeIngesta.Descartado ->
            "DESCARTADO: ${r.motivo}"
        is ResultadoDeIngesta.ContenidoOculto ->
            "CONTENIDO OCULTO por el sistema: ${r.sourcePackage} " +
                "(la UI debe decir 'Revisa tu app del banco')"
        is ResultadoDeIngesta.NoReconocido ->
            "NO RECONOCIDO: ninguna plantilla caso. Revisar plantillas_base.json"
    }

    companion object {
        /**
         * Sin el sufijo `.debug` del applicationId a proposito: la accion es una
         * constante del receptor, y el `-n` del comando ya apunta al paquete
         * correcto.
         */
        const val ACCION = "com.seef.checkqr.INYECTAR_AVISO"
        private const val TAG = "InyectorDeAvisos"
    }
}
