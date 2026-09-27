package com.seef.checkqr.feature.equipo

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * Escaner de QR para aceptar una invitacion.
 *
 * Analiza el flujo de la camara en vez de pedir una foto: el cajero apunta y
 * listo. Se filtra a QR (`FORMAT_QR_CODE`) para no reaccionar a los codigos de
 * barras de los productos que haya en el mostrador.
 */
@OptIn(ExperimentalGetImage::class)
@Composable
fun EscanerDeQr(
    onTexto: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val duenoDeCiclo = LocalLifecycleOwner.current

    val escaner = remember { BarcodeScanning.getClient() }
    val ejecutor = remember { Executors.newSingleThreadExecutor() }
    val controlador = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(LifecycleCameraController.IMAGE_ANALYSIS)
        }
    }

    DisposableEffect(duenoDeCiclo) {
        controlador.setImageAnalysisAnalyzer(ejecutor) { proxy ->
            val imagen = proxy.image
            if (imagen == null) {
                proxy.close()
                return@setImageAnalysisAnalyzer
            }
            val entrada = InputImage.fromMediaImage(imagen, proxy.imageInfo.rotationDegrees)
            escaner.process(entrada)
                .addOnSuccessListener { codigos ->
                    codigos.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                        ?.rawValue
                        ?.let(onTexto)
                }
                // El close va en el complete y no en el success: sin esto, un
                // fallo del reconocedor deja el ImageProxy sin cerrar y la
                // camara se congela tras unos pocos fotogramas.
                .addOnCompleteListener { proxy.close() }
        }
        controlador.bindToLifecycle(duenoDeCiclo)

        onDispose {
            controlador.clearImageAnalysisAnalyzer()
            controlador.unbind()
            escaner.close()
            ejecutor.shutdown()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    controller = controlador
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
