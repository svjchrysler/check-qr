package com.seef.checkqr.core.designsystem.componentes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.seef.checkqr.core.common.Dinero
import com.seef.checkqr.core.common.Plural
import com.seef.checkqr.core.designsystem.theme.Espaciado
import com.seef.checkqr.core.designsystem.theme.Montos

/**
 * El total del dia.
 *
 * Sin tarjeta ni fondo de color: solo la cifra, grande, sobre el blanco de la
 * pantalla. Encerrarla en un rectangulo verde la hacia competir con el boton de
 * abrir caja, que es lo unico que el comerciante tiene que tocar. Asi la cifra
 * manda por tamano, que es como manda de verdad.
 *
 * El importe cuenta al subir. Es la unica forma de que se vea que **entro**
 * dinero y no solo que el numero es otro.
 */
@Composable
fun TotalDelDia(
    totalCentavos: Long,
    cantidadDePagos: Int,
    modifier: Modifier = Modifier,
    cajaAbierta: Boolean = false,
    discreto: Boolean = false,
    etiqueta: String = "Cobrado hoy",
    /** Con false el importe aparece ya puesto, sin contar. Para el cuadre. */
    animar: Boolean = true,
) {
    val mostrado = if (animar) montoAnimado(totalCentavos) else totalCentavos

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Espaciado.minimo))

        Text(
            text = if (discreto) MONTO_OCULTO else Dinero.formatear(mostrado),
            style = Montos.destacado,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(Espaciado.corto))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espaciado.corto),
        ) {
            Text(
                text = if (cantidadDePagos == 0) {
                    "Sin pagos todavía"
                } else {
                    Plural.pagos(cantidadDePagos)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AnimatedVisibility(
                visible = cajaAbierta,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.size(Espaciado.corto))
                    Text(
                        text = "Caja abierta",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
