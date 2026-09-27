package com.seef.checkqr.core.designsystem.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.seef.checkqr.core.common.Dinero
import com.seef.checkqr.core.designsystem.theme.Espaciado
import com.seef.checkqr.core.designsystem.theme.Medidas
import com.seef.checkqr.core.designsystem.theme.Montos
import com.seef.checkqr.core.designsystem.theme.identidad
import com.seef.checkqr.core.model.Wallet

/**
 * Avatar circular de una billetera.
 *
 * El color permite reconocer el origen del cobro antes de leer nada. Las
 * iniciales van siempre: el color solo no sirve a quien tiene daltonismo, y
 * ademas el nombre completo esta en la fila.
 */
@Composable
fun AvatarDeBilletera(
    wallet: Wallet,
    modifier: Modifier = Modifier,
    tamano: androidx.compose.ui.unit.Dp = Medidas.avatar,
) {
    val identidad = wallet.identidad()
    Box(
        modifier = modifier
            .size(tamano)
            .background(identidad.fondo, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = identidad.iniciales,
            style = MaterialTheme.typography.labelMedium,
            color = identidad.contenido,
            maxLines = 1,
        )
    }
}

/**
 * Una linea de la lista de pagos.
 *
 * El orden de lectura es deliberado: avatar (de donde vino) → monto (cuanto) →
 * quien → cuando. Eso es el orden en que el comerciante hace las preguntas.
 *
 * El monto va en el color normal del texto y no en verde. El verde esta
 * reservado para el total del dia y para el "sí llegó" de la verificacion: si se
 * usa en todos lados deja de significar nada.
 */
@Composable
fun FilaDePago(
    montoCentavos: Long,
    pagador: String?,
    billetera: Wallet,
    hora: String,
    modifier: Modifier = Modifier,
    /** Oculta el monto, para el modo discreto. */
    discreto: Boolean = false,
    /** El parser no saco todos los datos; el dueno quiza quiera mirarlo. */
    incompleto: Boolean = false,
    /** Ya lo reclamo un comprobante verificado. */
    reclamado: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Medidas.filaDePago)
            .padding(horizontal = Espaciado.estandar, vertical = Espaciado.medio),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarDeBilletera(billetera)

        Spacer(Modifier.width(Espaciado.medio))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (discreto) MONTO_OCULTO else Dinero.formatear(montoCentavos),
                style = Montos.fila,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = pagador ?: "Sin nombre del pagador",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(Espaciado.corto))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = hora,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(Espaciado.minimo))
            Text(
                text = billetera.nombreVisible,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            if (reclamado || incompleto) {
                Spacer(Modifier.size(Espaciado.minimo))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (reclamado) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verificado con un comprobante",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    if (incompleto) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = "Faltan datos de este pago",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}

const val MONTO_OCULTO: String = "•• ••"

/** Separador muy tenue entre filas. Una linea fuerte parte la lista de mas. */
@Composable
internal fun colorDeSeparador(): Color =
    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
