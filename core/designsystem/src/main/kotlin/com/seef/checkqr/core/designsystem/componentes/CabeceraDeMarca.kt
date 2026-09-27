package com.seef.checkqr.core.designsystem.componentes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.seef.checkqr.core.common.Dinero
import com.seef.checkqr.core.common.Plural
import com.seef.checkqr.core.designsystem.theme.Espaciado
import com.seef.checkqr.core.designsystem.theme.LocalTemaOscuro
import com.seef.checkqr.core.designsystem.theme.Montos

/**
 * Colores del bloque de marca.
 *
 * En claro es el verde fuerte con texto blanco. En oscuro **no** se usa ese
 * mismo verde: un bloque de menta brillante ocupando un tercio de la pantalla
 * deslumbra de noche, que es justo cuando el comerciante tiene el celular en una
 * tienda con poca luz. En oscuro va el verde profundo con texto claro.
 */
private data class ColoresDeCabecera(val fondo: Color, val contenido: Color)

@Composable
private fun coloresDeCabecera(): ColoresDeCabecera = if (LocalTemaOscuro.current) {
    ColoresDeCabecera(
        fondo = MaterialTheme.colorScheme.primaryContainer,
        contenido = MaterialTheme.colorScheme.onPrimaryContainer,
    )
} else {
    ColoresDeCabecera(
        fondo = MaterialTheme.colorScheme.primary,
        contenido = MaterialTheme.colorScheme.onPrimary,
    )
}

/**
 * La cabecera de marca: un bloque verde con el total del dia.
 *
 * Sigue el patron que usan los neobancos latinoamericanos, y funciona por una
 * razon concreta: el dato que el comerciante viene a ver — cuanto lleva hoy —
 * ocupa el sitio donde el ojo cae primero, y el color lo separa del resto de la
 * pantalla sin necesidad de bordes ni lineas.
 *
 * El bloque se extiende por debajo de la barra de estado y aplica el inset por
 * dentro. Asi el verde llega hasta el borde superior de la pantalla en lugar de
 * dejar una franja blanca encima, que es lo que delata a una app sin terminar.
 *
 * Las esquinas inferiores redondeadas hacen que el contenido de abajo se lea
 * como algo que sube por delante, no como una segunda seccion pegada.
 */
@Composable
fun CabeceraDeMarca(
    titulo: String,
    totalCentavos: Long,
    cantidadDePagos: Int,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    cajaAbierta: Boolean = false,
    discreto: Boolean = false,
    escuchando: Boolean = true,
    etiquetaDelTotal: String = "Cobrado hoy",
    onTocarEstado: (() -> Unit)? = null,
    /** Con false el importe aparece ya puesto, sin contar. */
    animar: Boolean = true,
) {
    val mostrado = if (animar) montoAnimado(totalCentavos) else totalCentavos
    val colores = coloresDeCabecera()
    val sobreVerde = colores.contenido

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(colores.fondo),
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(
                    start = Espaciado.estandar,
                    end = Espaciado.estandar,
                    top = Espaciado.corto,
                    bottom = Espaciado.amplio,
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleLarge,
                        color = sobreVerde,
                    )
                    subtitulo?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = sobreVerde.copy(alpha = 0.7f),
                        )
                    }
                }

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
                        PuntoDeEscucha(
                            escuchando = escuchando,
                            // Sobre el verde, el punto va en blanco: el verde de
                            // "escuchando" desapareceria contra su propio fondo.
                            colorActivo = sobreVerde,
                        )
                    }
                }
            }

            Spacer(Modifier.height(Espaciado.amplio))

            Text(
                text = etiquetaDelTotal,
                style = MaterialTheme.typography.bodyMedium,
                color = sobreVerde.copy(alpha = 0.75f),
            )

            Spacer(Modifier.height(Espaciado.minimo))

            Text(
                text = if (discreto) MONTO_OCULTO else Dinero.formatear(mostrado),
                style = Montos.destacado,
                color = sobreVerde,
            )

            Spacer(Modifier.height(Espaciado.corto))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (cantidadDePagos == 0) {
                        "Sin pagos todavía"
                    } else {
                        Plural.pagos(cantidadDePagos)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = sobreVerde.copy(alpha = 0.75f),
                )

                AnimatedVisibility(
                    visible = cajaAbierta,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "  ·  ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = sobreVerde.copy(alpha = 0.5f),
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(sobreVerde),
                        )
                        Spacer(Modifier.size(Espaciado.corto))
                        Text(
                            text = "Caja abierta",
                            style = MaterialTheme.typography.bodyMedium,
                            color = sobreVerde,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Cabecera sencilla para las pantallas que no llevan un total.
 *
 * Mismo bloque de marca pero sin cifra, para que todas las pantallas compartan
 * el mismo remate superior en lugar de alternar entre una barra blanca y un
 * bloque de color.
 */
@Composable
fun CabeceraSimple(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    escuchando: Boolean = true,
    onTocarEstado: (() -> Unit)? = null,
    accion: (@Composable () -> Unit)? = null,
) {
    val colores = coloresDeCabecera()
    val sobreVerde = colores.contenido

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(colores.fondo),
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(
                    start = Espaciado.estandar,
                    end = Espaciado.estandar,
                    top = Espaciado.medio,
                    bottom = Espaciado.amplio,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.headlineSmall,
                    color = sobreVerde,
                )
                subtitulo?.let {
                    Spacer(Modifier.height(Espaciado.minimo))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = sobreVerde.copy(alpha = 0.75f),
                    )
                }
            }

            accion?.invoke()

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
                    PuntoDeEscucha(escuchando = escuchando, colorActivo = sobreVerde)
                }
            }
        }
    }
}

/** Icono sobre la cabecera, en blanco. */
@Composable
fun AccionDeCabecera(
    icono: ImageVector,
    descripcion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tinte: Color = MaterialTheme.colorScheme.onPrimary,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(imageVector = icono, contentDescription = descripcion, tint = tinte)
    }
}
