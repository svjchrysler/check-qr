package com.seef.checkqr.core.designsystem.componentes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * El boton de abrir y cerrar caja.
 *
 * Grande por dos razones: es la accion principal de la app, y en Android 17 es
 * literalmente lo unico que puede arrancar el servicio de voz, asi que tiene que
 * ser imposible no encontrarlo.
 */
@Composable
fun BotonDeCaja(
    abierta: Boolean,
    onClick: () -> Unit,
    habilitado: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = habilitado,
        colors = if (abierta) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            ButtonDefaults.buttonColors()
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .heightIn(min = 64.dp),
    ) {
        Box {
            Text(
                text = if (abierta) "Cerrar caja" else "Abrir caja",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
