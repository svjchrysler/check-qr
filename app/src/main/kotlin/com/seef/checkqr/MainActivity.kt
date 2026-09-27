package com.seef.checkqr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.seef.checkqr.core.datastore.PreferenciasCheckQr
import com.seef.checkqr.core.designsystem.theme.CheckQrTheme
import com.seef.checkqr.feature.onboarding.PantallaDeOnboarding
import com.seef.checkqr.navegacion.NavegacionDeCheckQr
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Unica Activity de la app.
 *
 * Importa que la caja se abra desde aqui y no desde ningun otro sitio: con
 * targetSdk 37, el servicio de voz solo puede arrancar por una accion del
 * usuario con la app visible, y esta es la pantalla visible.
 *
 * Sin android:screenOrientation en el manifest: Android 17 ignora los bloqueos
 * de orientacion en pantallas grandes, asi que fijarla no es una opcion.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefs: PreferenciasCheckQr

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CheckQrTheme {
                Raiz(prefs)
            }
        }
    }
}

@Composable
private fun Raiz(prefs: PreferenciasCheckQr) {
    // Se recoge el flujo entero, no un `first()`: cuando el onboarding marca que
    // termino, esta pantalla tiene que reaccionar y pasar a la app. Con una
    // lectura unica se quedaria en el onboarding para siempre.
    //
    // El `null` inicial es un tercer estado deliberado: sin el, la app
    // parpadearia mostrando el onboarding un instante a quien ya lo completo.
    val completado by produceState<Boolean?>(initialValue = null, prefs) {
        prefs.onboardingCompletado.collect { value = it }
    }

    when (completado) {
        null -> Unit // leyendo; la pantalla de arranque sigue visible
        false -> PantallaDeOnboarding(onTerminado = {})
        true -> NavegacionDeCheckQr()
    }
}
