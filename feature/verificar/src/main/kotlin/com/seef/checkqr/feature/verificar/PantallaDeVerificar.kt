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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seef.checkqr.core.designsystem.componentes.Aviso
import com.seef.checkqr.core.designsystem.theme.Espaciado
import com.seef.checkqr.core.designsystem.componentes.NivelDeAviso
import java.util.concurrent.Executors
import kotlinx.coroutines.delay

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

    var linternaEncendida by remember { mutableStateOf(false) }
    var hayLinterna by remember { mutableStateOf(false) }

    // `cameraInfo` no existe en el instante del bind, la camara se inicializa en
    // otro hilo. Se espera acotado en vez de indefinidamente: si la camara no
    // llega a abrir, el boton de linterna simplemente no aparece.
    LaunchedEffect(controlador) {
        repeat(40) {
            val info = controlador.cameraInfo
            if (info != null) {
                hayLinterna = info.hasFlashUnit()
                return@LaunchedEffect
            }
            delay(50)
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

        GuiaDeEncuadre(modifier = Modifier.fillMaxSize())

        if (hayLinterna) {
            // El comerciante trabaja en una tienda con poca luz, que es
            // justamente donde el OCR falla y donde se pierde la confianza en la
            // verificacion.
            IconButton(
                onClick = {
                    linternaEncendida = !linternaEncendida
                    controlador.enableTorch(linternaEncendida)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Espaciado.estandar),
            ) {
                Icon(
                    imageVector = if (linternaEncendida) {
                        Icons.Outlined.FlashOn
                    } else {
                        Icons.Outlined.FlashOff
                    },
                    contentDescription = if (linternaEncendida) {
                        "Apagar la linterna"
                    } else {
                        "Encender la linterna"
                    },
                    tint = Color.White,
                )
            }
        }

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
            Text("Sacar foto", style = MaterialTheme.typography.titleMedium)
        }
    }
}

/**
 * El marco que dice donde poner el comprobante.
 *
 * Sin el, la pantalla es la camara entera y un boton: nada indica que hay que
 * encuadrar, y el OCR depende justamente de eso. El texto pide lo que el
 * comparador necesita —monto y hora—, no un encuadre bonito.
 *
 * El oscurecido se dibuja como cuatro rectangulos alrededor del marco en lugar
 * de recortar un agujero con `BlendMode.Clear`: el recorte necesita una capa
 * fuera de pantalla y sobre una vista de camara da resultados distintos segun el
 * dispositivo.
 */
@Composable
private fun GuiaDeEncuadre(modifier: Modifier = Modifier) {
    val sombra = Color.Black.copy(alpha = 0.45f)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val anchoMarco = size.width * 0.86f
            val altoMarco = size.height * 0.46f
            val izquierda = (size.width - anchoMarco) / 2f
            // Algo por encima del centro: abajo va el boton de capturar.
            val arriba = (size.height - altoMarco) / 2f - size.height * 0.06f

            drawRect(sombra, size = Size(size.width, arriba))
            drawRect(
                sombra,
                topLeft = Offset(0f, arriba + altoMarco),
                size = Size(size.width, size.height - arriba - altoMarco),
            )
            drawRect(
                sombra,
                topLeft = Offset(0f, arriba),
                size = Size(izquierda, altoMarco),
            )
            drawRect(
                sombra,
                topLeft = Offset(izquierda + anchoMarco, arriba),
                size = Size(size.width - izquierda - anchoMarco, altoMarco),
            )

            drawRoundRect(
                color = Color.White,
                topLeft = Offset(izquierda, arriba),
                size = Size(anchoMarco, altoMarco),
                cornerRadius = CornerRadius(16.dp.toPx()),
                style = Stroke(width = 2.dp.toPx()),
            )
        }

        Text(
            text = "Que se vean el monto y la hora",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 96.dp, start = Espaciado.amplio, end = Espaciado.amplio),
        )
    }
}

private fun imagenFallida(error: ImageCaptureException) {
    android.util.Log.w("PantallaDeVerificar", "no se pudo capturar la imagen", error)
}
