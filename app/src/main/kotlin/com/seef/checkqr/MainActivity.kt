package com.seef.checkqr

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.seef.checkqr.core.designsystem.theme.CheckQrTheme
import com.seef.checkqr.feature.caja.PantallaDeCaja
import dagger.hilt.android.AndroidEntryPoint

/**
 * Unica Activity de la app.
 *
 * Importa que la caja se abra desde aqui y no desde ningun otro sitio: con
 * targetSdk 37, el servicio de voz solo puede arrancar por una accion del usuario
 * con la app visible, y esta es la pantalla visible.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CheckQrTheme {
                PedirPermisoDeNotificaciones()
                PantallaDeCaja()
            }
        }
    }
}

/**
 * El permiso de notificaciones hace falta para la notificacion fija de "Caja
 * abierta", que es lo que sostiene el servicio en primer plano. Sin ella no hay
 * voz.
 */
@androidx.compose.runtime.Composable
private fun PedirPermisoDeNotificaciones() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val lanzador = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* el estado real se lee del sistema, no de aqui */ }

    LaunchedEffect(Unit) {
        lanzador.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
