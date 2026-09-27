package com.seef.checkqr.core.designsystem.componentes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import com.seef.checkqr.core.designsystem.Iconos
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.seef.checkqr.core.designsystem.theme.Espaciado

/** Que tan grave es lo que hay que avisar. */
enum class NivelDeAviso { INFORMATIVO, ATENCION, PROBLEMA }

/** Un aviso pendiente, para mostrarlo suelto o dentro de la tira. */
data class AvisoPendiente(
    val titulo: String,
    val detalle: String? = null,
    val nivel: NivelDeAviso = NivelDeAviso.ATENCION,
    val textoDeAccion: String? = null,
    val onAccion: (() -> Unit)? = null,
)

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
    modifier: Modifier = Modifier,
    detalle: String? = null,
    nivel: NivelDeAviso = NivelDeAviso.ATENCION,
    textoDeAccion: String? = null,
    onAccion: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = nivel.contenedor(),
            contentColor = nivel.sobreContenedor(),
        ),
    ) {
        Row(modifier = Modifier.padding(Espaciado.estandar)) {
            Icon(
                imageVector = nivel.icono(),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(Espaciado.medio))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = titulo, style = MaterialTheme.typography.titleSmall)
                if (detalle != null) {
                    Spacer(Modifier.height(Espaciado.minimo))
                    Text(
                        text = detalle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = nivel.sobreContenedor().copy(alpha = 0.85f),
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
}

/**
 * Tira plegable con los avisos que no son urgentes.
 *
 * Nace de un problema concreto: apilar tres tarjetas de aviso empujaba la lista
 * de pagos fuera de la pantalla, y el comerciante abria la app para ver sus
 * cobros, no las advertencias. Ahora los avisos de nivel [NivelDeAviso.PROBLEMA]
 * se muestran enteros — son los que exigen actuar ya — y el resto se resume en
 * una linea que se despliega al tocarla.
 */
@Composable
fun TiraDeAvisos(
    avisos: List<AvisoPendiente>,
    modifier: Modifier = Modifier,
) {
    if (avisos.isEmpty()) return

    val urgentes = avisos.filter { it.nivel == NivelDeAviso.PROBLEMA }
    val secundarios = avisos - urgentes.toSet()

    Column(modifier = modifier.fillMaxWidth()) {
        urgentes.forEach { aviso ->
            Aviso(
                titulo = aviso.titulo,
                detalle = aviso.detalle,
                nivel = aviso.nivel,
                textoDeAccion = aviso.textoDeAccion,
                onAccion = aviso.onAccion,
            )
            Spacer(Modifier.height(Espaciado.corto))
        }

        if (secundarios.isNotEmpty()) {
            ResumenPlegable(secundarios)
        }
    }
}

@Composable
private fun ResumenPlegable(avisos: List<AvisoPendiente>) {
    var abierto by remember { mutableStateOf(false) }
    val giro by animateFloatAsState(
        targetValue = if (abierto) 180f else 0f,
        label = "giro de la flecha",
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        // Tinte ambar y no gris: sobre un fondo gris, una tira gris no se lee
        // como aviso ni como tarjeta, se lee como un hueco.
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { abierto = !abierto }
                    .padding(horizontal = Espaciado.estandar, vertical = Espaciado.medio),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Iconos.atencion,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(Espaciado.medio))
                Text(
                    text = if (avisos.size == 1) {
                        avisos.single().titulo
                    } else {
                        "${avisos.size} cosas para revisar"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (abierto) "Ocultar" else "Ver detalle",
                    modifier = Modifier.size(20.dp).rotate(giro),
                )
            }

            AnimatedVisibility(
                visible = abierto,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = Espaciado.estandar,
                        end = Espaciado.estandar,
                        bottom = Espaciado.estandar,
                    ),
                ) {
                    avisos.forEach { aviso ->
                        Text(
                            text = aviso.titulo,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        aviso.detalle?.let {
                            Text(text = it, style = MaterialTheme.typography.bodySmall)
                        }
                        aviso.textoDeAccion?.let { texto ->
                            aviso.onAccion?.let { accion ->
                                TextButton(
                                    onClick = accion,
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                        horizontal = 0.dp,
                                        vertical = Espaciado.minimo,
                                    ),
                                ) { Text(texto) }
                            }
                        }
                        Spacer(Modifier.height(Espaciado.medio))
                    }
                }
            }
        }
    }
}

@Composable
private fun NivelDeAviso.contenedor(): Color = when (this) {
    NivelDeAviso.INFORMATIVO -> MaterialTheme.colorScheme.surfaceContainerLowest
    NivelDeAviso.ATENCION -> MaterialTheme.colorScheme.tertiaryContainer
    NivelDeAviso.PROBLEMA -> MaterialTheme.colorScheme.errorContainer
}

@Composable
private fun NivelDeAviso.sobreContenedor(): Color = when (this) {
    NivelDeAviso.INFORMATIVO -> MaterialTheme.colorScheme.onSurfaceVariant
    NivelDeAviso.ATENCION -> MaterialTheme.colorScheme.onTertiaryContainer
    NivelDeAviso.PROBLEMA -> MaterialTheme.colorScheme.onErrorContainer
}

private fun NivelDeAviso.icono(): ImageVector = when (this) {
    NivelDeAviso.INFORMATIVO -> Iconos.informacion
    NivelDeAviso.ATENCION -> Iconos.atencion
    NivelDeAviso.PROBLEMA -> Iconos.problema
}

/** Confirmacion breve, para cuando algo salio bien. */
@Composable
fun AvisoDeExito(titulo: String, modifier: Modifier = Modifier, detalle: String? = null) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(Espaciado.estandar),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Iconos.correcto,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(Espaciado.medio))
            Column {
                Text(titulo, style = MaterialTheme.typography.titleSmall)
                detalle?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
