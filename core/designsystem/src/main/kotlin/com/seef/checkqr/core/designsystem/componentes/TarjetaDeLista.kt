package com.seef.checkqr.core.designsystem.componentes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.seef.checkqr.core.designsystem.theme.Espaciado

/**
 * Contenedor redondeado para un grupo de filas.
 *
 * Agrupar la lista dentro de una tarjeta, en vez de dejar las filas sueltas
 * sobre el fondo, hace dos cosas: dice donde empieza y donde termina un bloque
 * de informacion, y le da a la pantalla la profundidad que una lista plana no
 * tiene. Es lo que separa una pantalla que parece un borrador de una que parece
 * un producto.
 *
 * La sombra es minima a proposito. Una sombra marcada sobre fondo claro se ve
 * sucia; lo que se busca es solo despegar la tarjeta del fondo.
 */
@Composable
fun TarjetaDeLista(
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Espaciado.estandar),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 1.dp,
        tonalElevation = 0.dp,
    ) {
        Column(content = contenido)
    }
}

/**
 * Tarjeta para el primer y el ultimo elemento de una lista partida en varios
 * items de LazyColumn.
 *
 * Hace falta porque una LazyColumn no puede meter todos sus items dentro de una
 * misma Surface sin perder el reciclado: cada fila se dibuja por separado y es
 * el fondo con las esquinas correctas el que da la ilusion de una sola tarjeta.
 */
enum class PosicionEnTarjeta { UNICA, PRIMERA, MEDIA, ULTIMA }

/** La forma que le toca a una fila segun donde este dentro del grupo. */
@Composable
fun formaDeFila(posicion: PosicionEnTarjeta) = when (posicion) {
    PosicionEnTarjeta.UNICA -> MaterialTheme.shapes.large
    PosicionEnTarjeta.PRIMERA -> MaterialTheme.shapes.large.copy(
        bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp),
        bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp),
    )
    PosicionEnTarjeta.MEDIA -> androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
    PosicionEnTarjeta.ULTIMA -> MaterialTheme.shapes.large.copy(
        topStart = androidx.compose.foundation.shape.CornerSize(0.dp),
        topEnd = androidx.compose.foundation.shape.CornerSize(0.dp),
    )
}

/** Donde cae un indice dentro de una lista de [total] elementos. */
fun posicionEnTarjeta(indice: Int, total: Int): PosicionEnTarjeta = when {
    total == 1 -> PosicionEnTarjeta.UNICA
    indice == 0 -> PosicionEnTarjeta.PRIMERA
    indice == total - 1 -> PosicionEnTarjeta.ULTIMA
    else -> PosicionEnTarjeta.MEDIA
}
