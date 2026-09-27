package com.seef.checkqr.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Los iconos de la app, en un solo sitio.
 *
 * Centralizarlos resuelve dos cosas que antes estaban mal:
 *
 * 1. **Coherencia de trazo.** Se mezclaban iconos rellenos (`Icons.Default`) con
 *    contorneados (`Icons.Outlined`) en la misma pantalla, y eso se nota aunque
 *    no se sepa decir por que. Ahora la regla es una sola: contorneado en todas
 *    partes, y relleno solo para la pestana activa de la barra inferior, que es
 *    el patron con el que la gente ya sabe leer "estoy aqui".
 *
 * 2. **Semantica.** Varios iconos decian otra cosa. El mas claro: "Verificar"
 *    usaba un escaner de QR, pero esa pantalla no escanea un QR — le saca una
 *    foto a un comprobante. `DocumentScanner` es lo que realmente hace.
 */
object Iconos {

    /** Caja: el mostrador del comercio. */
    val caja: ImageVector = Icons.Outlined.Storefront
    val cajaActiva: ImageVector = Icons.Filled.Storefront

    /**
     * Verificar: se fotografia un comprobante y se le lee el texto.
     * No es un escaner de QR, aunque la app se llame CheckQr.
     */
    val verificar: ImageVector = Icons.Outlined.DocumentScanner
    val verificarActiva: ImageVector = Icons.Filled.DocumentScanner

    /** Cuadre: el resumen del dia. */
    val cuadre: ImageVector = Icons.Outlined.Assessment
    val cuadreActiva: ImageVector = Icons.Filled.Assessment

    /** Equipo. */
    val equipo: ImageVector = Icons.Outlined.Group
    val equipoActiva: ImageVector = Icons.Filled.Group

    /** Estado: la app esta a la escucha de los avisos. */
    val estado: ImageVector = Icons.Outlined.Sensors
    val estadoActiva: ImageVector = Icons.Filled.Sensors

    // --- Estados y avisos ---

    val correcto: ImageVector = Icons.Outlined.CheckCircle
    val atencion: ImageVector = Icons.Outlined.WarningAmber
    val problema: ImageVector = Icons.Outlined.ErrorOutline
    val informacion: ImageVector = Icons.Outlined.Info

    /** Sin pagos todavia. */
    val bandejaVacia: ImageVector = Icons.Outlined.Inbox

    /** Esperando que llegue algo. */
    val esperando: ImageVector = Icons.Outlined.HourglassEmpty

    /** Ocultar los montos. */
    val ocultar: ImageVector = Icons.Outlined.VisibilityOff
}
