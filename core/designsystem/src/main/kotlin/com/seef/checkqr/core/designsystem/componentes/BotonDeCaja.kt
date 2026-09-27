package com.seef.checkqr.core.designsystem.componentes

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.seef.checkqr.core.designsystem.theme.Espaciado
import com.seef.checkqr.core.designsystem.theme.Medidas

/**
 * El boton de abrir y cerrar caja.
 *
 * Grande y con icono por dos razones: es la accion principal de la app, y en
 * Android 17 es literalmente lo unico que puede arrancar el servicio de voz, asi
 * que tiene que ser imposible no encontrarlo.
 *
 * Cambia de color al abrirse: lleno cuando invita a abrir, tonal cuando ya esta
 * abierta y la accion pasa a ser cerrar, que es menos frecuente y no conviene
 * que compita visualmente con el resto.
 */
@Composable
fun BotonDeCaja(
    abierta: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
) {
    val contenedor by animateColorAsState(
        targetValue = if (abierta) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.primary
        },
        label = "color del boton de caja",
    )
    val contenido by animateColorAsState(
        targetValue = if (abierta) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onPrimary
        },
        label = "color del texto del boton de caja",
    )

    Button(
        onClick = onClick,
        enabled = habilitado,
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = contenedor,
            contentColor = contenido,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(Medidas.botonPrincipal),
    ) {
        Icon(
            imageVector = if (abierta) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.size(Espaciado.corto))
        Text(
            text = if (abierta) "Cerrar caja" else "Abrir caja",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/**
 * Las dos acciones secundarias de la caja.
 *
 * Antes eran dos botones de solo texto, que se leian como enlaces sueltos y no
 * decian que hacian. Con icono y fondo tonal quedan claramente como acciones y
 * se distinguen del boton principal sin competir con el.
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
        FilledTonalButton(
            onClick = onMostrador,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.weight(1f).height(Medidas.objetivoTactil),
        ) {
            Icon(
                imageVector = Icons.Outlined.Fullscreen,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(Espaciado.corto))
            Text("Mostrador", style = MaterialTheme.typography.labelLarge)
        }

        FilledTonalButton(
            onClick = onAlternarDiscreto,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.weight(1f).height(Medidas.objetivoTactil),
        ) {
            Icon(
                imageVector = if (discreto) {
                    Icons.Outlined.Visibility
                } else {
                    Icons.Outlined.VisibilityOff
                },
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(Espaciado.corto))
            Text(
                text = if (discreto) "Mostrar" else "Ocultar",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
