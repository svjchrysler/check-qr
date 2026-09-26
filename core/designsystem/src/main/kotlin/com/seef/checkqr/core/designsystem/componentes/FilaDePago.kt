package com.seef.checkqr.core.designsystem.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.seef.checkqr.core.common.Dinero

/**
 * Una linea de la lista de pagos.
 *
 * El monto va primero en jerarquia visual porque es lo que el comerciante
 * compara contra lo que le dijo el cliente; el nombre y la billetera son
 * contexto.
 */
@Composable
fun FilaDePago(
    montoCentavos: Long,
    pagador: String?,
    billetera: String,
    hora: String,
    /** Oculta el monto, para el modo discreto. */
    discreto: Boolean = false,
    /** Marca los pagos con datos incompletos, para que el dueno los revise. */
    incompleto: Boolean = false,
    reclamado: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.padding(end = 12.dp)) {
                Text(
                    text = if (discreto) "•• ••" else Dinero.formatear(montoCentavos),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = pagador ?: "Sin nombre del pagador",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = billetera, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = hora,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (incompleto) {
                    Text(
                        text = "revisar",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                if (reclamado) {
                    Text(
                        text = "ya verificado",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        HorizontalDivider()
    }
}
