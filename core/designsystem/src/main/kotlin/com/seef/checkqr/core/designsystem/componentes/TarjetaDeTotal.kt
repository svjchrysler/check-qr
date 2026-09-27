package com.seef.checkqr.core.designsystem.componentes

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.seef.checkqr.core.common.Dinero
import com.seef.checkqr.core.designsystem.theme.Espaciado
import com.seef.checkqr.core.designsystem.theme.Montos

/**
 * La tarjeta del total del dia.
 *
 * Es lo primero que mira el comerciante cada vez que abre la app, asi que se le
 * da un anclaje visual propio en lugar de dejar el numero flotando sobre el
 * fondo. El contenedor coloreado tambien separa "lo que llevo hoy" de "los
 * pagos uno por uno", que son dos preguntas distintas.
 */
@Composable
fun TarjetaDeTotal(
    totalCentavos: Long,
    cantidadDePagos: Int,
    cajaAbierta: Boolean,
    modifier: Modifier = Modifier,
    discreto: Boolean = false,
    etiqueta: String = "Cobrado hoy",
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(Espaciado.amplio)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = etiqueta.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
                IndicadorDeCaja(abierta = cajaAbierta)
            }

            Spacer(Modifier.height(Espaciado.corto))

            Text(
                text = if (discreto) MONTO_OCULTO else Dinero.formatear(totalCentavos),
                style = Montos.destacado,
            )

            Spacer(Modifier.height(Espaciado.minimo))

            Text(
                text = when (cantidadDePagos) {
                    0 -> "Todavía sin pagos"
                    1 -> "1 pago"
                    else -> "$cantidadDePagos pagos"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            )
        }
    }
}

/**
 * Punto de color con la palabra al lado.
 *
 * El punto solo no alcanza: verde y gris se parecen demasiado para alguien con
 * daltonismo, y este indicador dice si la app esta o no anunciando los cobros.
 */
@Composable
private fun IndicadorDeCaja(abierta: Boolean) {
    val color by animateColorAsState(
        targetValue = if (abierta) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f)
        },
        label = "color del indicador de caja",
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.size(Espaciado.corto))
        Text(
            text = if (abierta) "Caja abierta" else "Caja cerrada",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
