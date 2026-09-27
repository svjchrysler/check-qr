package com.seef.checkqr.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Si el tema vigente es oscuro.
 *
 * Hace falta porque la identidad de cada billetera tiene variante clara y
 * oscura, y esos colores no viven en el ColorScheme de Material.
 */
val LocalTemaOscuro = staticCompositionLocalOf { false }

/**
 * Tema de CheckQr.
 *
 * Deliberadamente **sin color dinamico**. En una app de cobros el verde del pago
 * recibido y el rojo del "no llegó" tienen que significar siempre lo mismo, en
 * todos los celulares del equipo: si el sistema los tine con el fondo de
 * pantalla del dueno, dos cajeros ven colores distintos para el mismo estado.
 */
@Composable
fun CheckQrTheme(
    oscuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalTemaOscuro provides oscuro) {
        MaterialTheme(
            colorScheme = if (oscuro) esquemaOscuro else esquemaClaro,
            typography = tipografia,
            shapes = formas,
            content = content,
        )
    }
}
