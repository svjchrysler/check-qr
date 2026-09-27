package com.seef.checkqr.core.designsystem.componentes

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

/**
 * El punto que dice si la app esta escuchando los avisos del banco.
 *
 * Cuando escucha, late: un halo se expande y se desvanece cada dos segundos. Es
 * la unica animacion en bucle de la app y responde a la pregunta que el
 * comerciante se hace sin decirla — "¿esto sigue funcionando?" — que un punto
 * verde quieto no contesta, porque un punto quieto tambien es lo que se ve
 * cuando la app se colgo.
 *
 * Cuando **no** escucha se queda inmovil, en rojo. Un punto rojo parpadeando
 * pide atencion de forma agresiva, y para ese caso la app ya tiene un cartel que
 * explica el problema con palabras.
 */
@Composable
fun PuntoDeEscucha(
    escuchando: Boolean,
    modifier: Modifier = Modifier,
    tamano: androidx.compose.ui.unit.Dp = 10.dp,
) {
    val color = if (escuchando) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    Box(modifier = modifier.size(tamano * 3), contentAlignment = Alignment.Center) {
        if (escuchando) {
            // Un solo transition para las dos propiedades: asi el halo crece y se
            // apaga acompasado en lugar de ir cada uno por su lado.
            val transicion = rememberInfiniteTransition(label = "latido de escucha")
            val pulso by transicion.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(Duraciones.LATIDO, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "pulso",
            )

            Box(
                modifier = Modifier
                    .size(tamano)
                    .scale(1f + pulso * 1.1f)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.22f * (1f - pulso))),
            )
        }

        Box(
            modifier = Modifier
                .size(tamano)
                .clip(CircleShape)
                .background(color),
        )
    }
}
