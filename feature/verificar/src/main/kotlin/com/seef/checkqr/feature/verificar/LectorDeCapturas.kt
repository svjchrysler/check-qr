package com.seef.checkqr.feature.verificar

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Lee el texto de la captura de pantalla que muestra el cliente.
 *
 * ML Kit empaquetado, no el que se baja de Play Services: el reconocimiento
 * tiene que funcionar en el mostrador aunque no haya internet, que es el caso
 * habitual. Pesa mas en el APK y es el precio correcto.
 *
 * La foto no sale nunca del celular ni se guarda: se queda el texto.
 */
@Singleton
class LectorDeCapturas @Inject constructor() {

    private val reconocedor by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun leer(imagen: Bitmap): String = suspendCancellableCoroutine { cont ->
        val entrada = InputImage.fromBitmap(imagen, 0)
        reconocedor.process(entrada)
            .addOnSuccessListener { resultado -> cont.resume(resultado.text) }
            .addOnFailureListener { error -> cont.resumeWithException(error) }
        cont.invokeOnCancellation { /* ML Kit no expone cancelacion del proceso */ }
    }

    fun liberar() = reconocedor.close()
}
