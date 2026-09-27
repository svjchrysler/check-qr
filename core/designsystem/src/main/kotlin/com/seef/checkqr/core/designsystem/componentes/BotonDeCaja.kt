package com.seef.checkqr.core.designsystem.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.seef.checkqr.core.designsystem.Iconos
import com.seef.checkqr.core.designsystem.theme.Espaciado
import com.seef.checkqr.core.designsystem.theme.Medidas

/**
 * El boton de abrir y cerrar caja.
 *
 * Es la accion principal de la app y, en Android 17, literalmente lo unico que
 * puede arrancar el servicio de voz: tiene que ser imposible no encontrarlo.
 *
 * Cambia de forma al abrirse. Lleno y verde cuando invita a abrir; contorneado y
 * neutro cuando ya esta abierta, porque entonces la accion es cerrar, que es
 * menos frecuente y no conviene que siga tirando del ojo todo el dia. El cambio
 * se anima para que se lea como el mismo boton cambiando de estado y no como dos
 * botones distintos.
 */
@Composable
fun BotonDeCaja(
    abierta: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
) {
    // Vibracion corta al abrir y al cerrar. Confirma el cambio de estado sin
    // obligar a mirar la pantalla, que es justo lo que el comerciante hace: toca
    // el boton mientras sigue atendiendo al cliente.
    val haptica = LocalHapticFeedback.current

    // Con la caja abierta el boton se vacia y le sale un borde, en vez de
    // quedarse relleno de gris. Un relleno claro con texto oscuro es
    // exactamente el aspecto que Material le da a un boton desactivado, asi que
    // "Cerrar caja" parecia no se podia tocar. El borde es lo que lo desmiente:
    // un boton desactivado nunca lo lleva.
    val contenedor by animateColorAsState(
        targetValue = if (abierta) {
            Color.Transparent
        } else {
            MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(Duraciones.CORTA),
        label = "fondo del boton de caja",
    )
    val contenido by animateColorAsState(
        targetValue = if (abierta) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onPrimary
        },
        animationSpec = tween(Duraciones.CORTA),
        label = "texto del boton de caja",
    )
    val borde by animateColorAsState(
        targetValue = if (abierta) {
            MaterialTheme.colorScheme.outline
        } else {
            Color.Transparent
        },
        animationSpec = tween(Duraciones.CORTA),
        label = "borde del boton de caja",
    )

    Button(
        onClick = {
            haptica.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        enabled = habilitado,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, borde),
        colors = ButtonDefaults.buttonColors(
            containerColor = contenedor,
            contentColor = contenido,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(Medidas.botonPrincipal),
    ) {
        AnimatedContent(
            targetState = abierta,
            transitionSpec = {
                fadeIn(tween(Duraciones.CORTA)) togetherWith fadeOut(tween(Duraciones.CORTA))
            },
            label = "contenido del boton de caja",
        ) { estaAbierta ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (estaAbierta) {
                        Icons.Rounded.Stop
                    } else {
                        Icons.Rounded.PlayArrow
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(Espaciado.corto))
                Text(
                    text = if (estaAbierta) "Cerrar caja" else "Abrir caja",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

/**
 * Las dos acciones secundarias de la caja.
 *
 * Contorneadas y no rellenas: son atajos, no acciones que haya que hacer. Un
 * boton con fondo al lado del principal compite por la mirada sin merecerlo.
 */
@Composable
fun AccionesDeCaja(
    discreto: Boolean,
    onMostrador: () -> Unit,
    onAlternarDiscreto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Espaciado.medio),
    ) {
        BotonSecundario(
            texto = "Mostrador",
            icono = Icons.Outlined.Fullscreen,
            onClick = onMostrador,
            modifier = Modifier.weight(1f),
        )
        BotonSecundario(
            texto = if (discreto) "Mostrar" else "Ocultar",
            icono = if (discreto) Icons.Outlined.Visibility else Iconos.ocultar,
            onClick = onAlternarDiscreto,
            modifier = Modifier.weight(1f),
        )
    }
}
