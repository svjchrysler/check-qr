package com.seef.checkqr.core.designsystem.componentes

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.seef.checkqr.core.designsystem.theme.Espaciado

/**
 * Barra superior de la app.
 *
 * Lleva un punto de estado a la derecha: verde cuando la app esta escuchando los
 * avisos del banco, rojo cuando no. Es el unico indicador que esta presente en
 * todas las pantallas, y esta ahi porque lo peor que puede pasar con esta app es
 * que deje de escuchar sin que nadie se entere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSuperior(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    escuchando: Boolean = true,
    onTocarEstado: (() -> Unit)? = null,
    acciones: @Composable () -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Column {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                )
                subtitulo?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        actions = {
            acciones()
            if (onTocarEstado != null) {
                IconButton(
                    onClick = onTocarEstado,
                    modifier = Modifier.semantics {
                        contentDescription = if (escuchando) {
                            "Escuchando los pagos. Ver estado del sistema"
                        } else {
                            "No está escuchando los pagos. Ver estado del sistema"
                        }
                    },
                ) {
                    PuntoDeEstado(escuchando)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

/**
 * El punto que dice si la app esta escuchando.
 *
 * Con descripcion para lectores de pantalla, porque el color por si solo no
 * comunica nada a quien no lo ve.
 */
@Composable
fun PuntoDeEstado(escuchando: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(
                if (escuchando) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            ),
    )
    // La descripcion la pone el IconButton que lo contiene: asi el lector de
    // pantalla anuncia una sola cosa en lugar de un boton y un punto sueltos.
}

/** Titulo de una seccion dentro de una lista. */
@Composable
fun EncabezadoDeSeccion(
    texto: String,
    modifier: Modifier = Modifier,
    acompanante: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = Espaciado.estandar,
                end = Espaciado.estandar,
                top = Espaciado.amplio,
                bottom = Espaciado.corto,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        acompanante?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Estado vacio.
 *
 * Una pantalla en blanco deja al usuario sin saber si la app esta rota o si
 * simplemente no hay nada. Siempre se dice ademas que hacer a continuacion.
 */
@Composable
fun EstadoVacio(
    icono: ImageVector,
    titulo: String,
    modifier: Modifier = Modifier,
    detalle: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Espaciado.seccion, vertical = Espaciado.seccion),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(Espaciado.estandar))
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        detalle?.let {
            Spacer(Modifier.height(Espaciado.corto))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Separador tenue entre filas de una lista. */
@Composable
fun SeparadorDeLista(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(start = Espaciado.estandar + 44.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )
}
