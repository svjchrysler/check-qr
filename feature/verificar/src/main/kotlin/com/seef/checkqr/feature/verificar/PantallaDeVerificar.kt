package com.seef.checkqr.feature.verificar

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seef.checkqr.core.common.Dinero
import com.seef.checkqr.core.designsystem.componentes.Aviso
import com.seef.checkqr.core.designsystem.componentes.NivelDeAviso
import com.seef.checkqr.core.designsystem.theme.TipografiaMostrador
import com.seef.checkqr.core.model.MatchResult
import java.util.concurrent.Executors

/**
 * Verificar el comprobante que muestra el cliente.
 *
 * El OCR corre en el dispositivo con ML Kit empaquetado: la foto no sale del
 * celular ni se guarda, solo se conserva el texto acotado.
 */
@Composable
fun PantallaDeVerificar(
    modifier: Modifier = Modifier,
    vm: VerificarViewModel = hiltViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var permisoConcedido by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val pedirPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedido -> permisoConcedido = concedido }

    LaunchedEffect(Unit) {
        if (!permisoConcedido) pedirPermiso.launch(Manifest.permission.CAMERA)
    }

    Column(modifier = modifier.fillMaxSize()) {
        when (val e = estado) {
            is EstadoDeVerificacion.Esperando ->
                if (permisoConcedido) {
                    Camara(onCaptura = vm::verificar, modifier = Modifier.weight(1f))
                } else {
                    Aviso(
                        titulo = "Falta permiso de cámara",
                        detalle = "Para verificar el comprobante hay que sacarle una foto " +
                            "a la pantalla del cliente.",
                        nivel = NivelDeAviso.ATENCION,
                        textoDeAccion = "Permitir",
                        onAccion = { pedirPermiso.launch(Manifest.permission.CAMERA) },
                    )
                }

            is EstadoDeVerificacion.Leyendo -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Leyendo el comprobante…")
                }
            }

            is EstadoDeVerificacion.Resuelto -> Resultado(
                estado = e,
                onElegir = vm::elegir,
                onReiniciar = vm::reiniciar,
            )

            is EstadoDeVerificacion.Fallo -> Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Aviso(titulo = "No se pudo leer", detalle = e.motivo, nivel = NivelDeAviso.ATENCION)
                Spacer(Modifier.height(16.dp))
                Button(onClick = vm::reiniciar) { Text("Intentar de nuevo") }
            }
        }
    }
}

@Composable
private fun Resultado(
    estado: EstadoDeVerificacion.Resuelto,
    onElegir: (com.seef.checkqr.core.model.Payment) -> Unit,
    onReiniciar: () -> Unit,
) {
    val v = estado.veredicto

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val (simbolo, titulo, nivel) = when (v.resultado) {
            MatchResult.COINCIDE -> Triple("✓", "Sí llegó", NivelDeAviso.INFORMATIVO)
            MatchResult.NO_LLEGO -> Triple("✕", "No llegó", NivelDeAviso.PROBLEMA)
            MatchResult.AMBIGUO -> Triple("?", "Hay que revisar", NivelDeAviso.ATENCION)
            MatchResult.YA_RECLAMADO -> Triple("!", "Ya verificado antes", NivelDeAviso.ATENCION)
        }

        Text(
            text = simbolo,
            style = TipografiaMostrador.monto,
            color = when (v.resultado) {
                MatchResult.COINCIDE -> MaterialTheme.colorScheme.primary
                MatchResult.NO_LLEGO -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.tertiary
            },
        )
        Text(titulo, style = TipografiaMostrador.detalle)

        v.pago?.let { p ->
            Text(
                Dinero.formatear(p.amountCents),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(p.payerName ?: "Sin nombre", style = MaterialTheme.typography.bodyLarge)
            Text(p.wallet.nombreVisible, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(12.dp))
        Text(
            v.explicacion,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Con varios candidatos, elige la persona: aqui no se adivina.
        if (v.resultado == MatchResult.AMBIGUO && v.candidatos.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            v.candidatos.forEach { p ->
                OutlinedButton(
                    onClick = { onElegir(p) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) {
                    Text("${Dinero.formatear(p.amountCents)} · ${p.payerName ?: "sin nombre"}")
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(onClick = onReiniciar) { Text("Verificar otro") }
    }
}

@Composable
private fun Camara(
    onCaptura: (android.graphics.Bitmap) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val duenoDeCiclo = LocalLifecycleOwner.current

    val controlador = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(LifecycleCameraController.IMAGE_CAPTURE)
        }
    }
    val ejecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(duenoDeCiclo) {
        controlador.bindToLifecycle(duenoDeCiclo)
        onDispose {
            controlador.unbind()
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

        Button(
            onClick = {
                controlador.takePicture(
                    ejecutor,
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(imagen: ImageProxy) {
                            // toBitmap() ya aplica la rotacion que reporta el
                            // sensor: sin eso el OCR lee una imagen de lado.
                            val bitmap = imagen.toBitmap()
                            imagen.close()
                            onCaptura(bitmap)
                        }

                        override fun onError(error: ImageCaptureException) {
                            imagenFallida(error)
                        }
                    },
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp)
                .height(64.dp),
        ) {
            Text("Sacar foto al comprobante", style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun imagenFallida(error: ImageCaptureException) {
    android.util.Log.w("PantallaDeVerificar", "no se pudo capturar la imagen", error)
}
