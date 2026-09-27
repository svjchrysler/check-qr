package com.seef.checkqr.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Esquinas del sistema.
 *
 * Mas redondeadas que el valor por omision de Material: le da a la app un aire
 * cercano, que es lo que corresponde a una herramienta que un comerciante tiene
 * en el mostrador todo el dia, y no a un panel de control bancario.
 */
val formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
