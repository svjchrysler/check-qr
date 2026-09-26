package com.seef.checkqr.feature.caja

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Mantiene la pantalla encendida mientras [activo].
 *
 * Se usa el flag de la ventana y no un WakeLock: no requiere permiso, el sistema
 * lo suelta solo si la app pasa a segundo plano, y es lo apropiado para una
 * pantalla que el usuario esta mirando. Un WakeLock aqui seria pedir mas de lo
 * necesario y un riesgo de dejar el celular sin bateria.
 */
@Composable
internal fun MantenerPantallaEncendida(activo: Boolean) {
    val context = LocalContext.current
    DisposableEffect(activo, context) {
        val ventana = (context as? Activity)?.window
        if (activo) {
            ventana?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            ventana?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
