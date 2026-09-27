package com.seef.checkqr.core.designsystem.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.seef.checkqr.core.designsystem.theme.Espaciado
import com.seef.checkqr.core.designsystem.theme.Medidas

/**
 * Una accion secundaria: disponible, pero que no compite con la principal.
 *
 * Existe porque el `OutlinedButton` de Material por omision se pierde sobre una
 * tarjeta blanca — su borde es tan claro que el boton parece desactivado, y un
 * boton que parece desactivado no se toca. Aqui el borde va en `outline`, que es
 * un gris medio y se ve, y la etiqueta en `onSurface`.
 *
 * La etiqueta no va en verde aunque sea el color de marca: en esta app el verde
 * significa dinero que entro, y gastarlo en los atajos le quita el significado
 * justo donde mas hace falta. Un boton contorneado ya se lee como secundario sin
 * necesidad de color.
 */
@Composable
fun BotonSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    habilitado: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = habilitado,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = modifier.height(Medidas.objetivoTactil),
    ) {
        if (icono != null) {
            Icon(imageVector = icono, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(Espaciado.corto))
        }
        Text(texto, style = MaterialTheme.typography.labelLarge)
    }
}
