package com.seef.checkqr.core.designsystem.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Que tan grave es lo que hay que avisar. */
enum class NivelDeAviso { INFORMATIVO, ATENCION, PROBLEMA }

/**
 * Aviso dentro de la pantalla.
 *
 * Existe porque el modo de fallo por omision de esta app es el silencio: el
 * sistema oculta el texto de un aviso, el listener se desconecta, el celular se
 * reinicia y la voz queda apagada. Nada de eso produce un error visible, asi que
 * la app tiene que decirlo de forma explicita.
 */
@Composable
fun Aviso(
    titulo: String,
    detalle: String? = null,
    nivel: NivelDeAviso = NivelDeAviso.ATENCION,
    textoDeAccion: String? = null,
    onAccion: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colores = when (nivel) {
        NivelDeAviso.INFORMATIVO -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NivelDeAviso.ATENCION -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
        NivelDeAviso.PROBLEMA -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
    }

    Card(colors = colores, modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (detalle != null) {
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (textoDeAccion != null && onAccion != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onAccion) { Text(textoDeAccion) }
                }
            }
        }
    }
}
