package com.seef.checkqr.feature.caja

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloseFullscreen
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import com.seef.checkqr.core.common.Calendario
import com.seef.checkqr.core.common.Dinero
import com.seef.checkqr.core.designsystem.componentes.AvatarDeBilletera
import com.seef.checkqr.core.designsystem.componentes.MONTO_OCULTO
import com.seef.checkqr.core.designsystem.theme.Espaciado
import com.seef.checkqr.core.designsystem.theme.TipografiaMostrador
import com.seef.checkqr.core.model.Payment

/**
 * Modo mostrador: el ultimo pago en letra enorme.
 *
 * Es la pantalla que el comerciante deja encendida sobre la barra mientras
 * atiende, asi que el diseno resuelve un problema muy concreto: que el ultimo
 * cobro se lea de pie, a un metro, de reojo. Todo lo demas se sale del medio.
 *
 * Adaptable y sin orientacion fija: Android 17 ignora las restricciones de
 * orientacion y tamano en pantallas grandes, asi que fijarla no es una opcion.
 *
 * El monto entra con una animacion breve. No es adorno: si el comerciante estaba
 * mirando hacia otro lado, el movimiento le dice que el numero cambio, cosa que
 * un reemplazo instantaneo no comunica.
 */
@Composable
fun PantallaDeMostrador(
    onSalir: () -> Unit,
    modifier: Modifier = Modifier,
    vm: CajaViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    // La pantalla se queda encendida: el celular queda apoyado en el mostrador y
    // el comerciante no lo toca entre cobro y cobro.
    MantenerPantallaEncendida(activo = true)

    Mostrador(ui = ui, onSalir = onSalir, modifier = modifier)
}

@Composable
internal fun Mostrador(
    ui: UiCaja,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val compacto = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass ==
        WindowWidthSizeClass.COMPACT
    val estiloMonto = if (compacto) {
        TipografiaMostrador.montoCompacto
    } else {
        TipografiaMostrador.monto
    }

    val ultimo = ui.pagosDeHoy.firstOrNull()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        // El inset lo aplica el mostrador y no un Scaffold: esta pantalla ocupa
        // todo el alto a proposito, sin barra superior ni inferior, porque el
        // celular queda apoyado y cualquier barra invita a toques accidentales.
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            // Barra de resumen arriba: el total del dia siempre visible, pero
            // pequeno, para que no le compita al ultimo cobro.
            ResumenSuperior(ui = ui, onSalir = onSalir)

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = ultimo,
                    transitionSpec = {
                        (slideInVertically { it / 4 } + fadeIn(tween(220)))
                            .togetherWith(fadeOut(tween(120)) + slideOutVertically { -it / 6 })
                    },
                    label = "ultimo pago del mostrador",
                ) { pago ->
                    if (pago == null) {
                        Esperando()
                    } else {
                        UltimoPago(
                            pago = pago,
                            discreto = ui.modoDiscreto,
                            estiloMonto = estiloMonto,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResumenSuperior(ui: UiCaja, onSalir: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Espaciado.amplio, vertical = Espaciado.estandar),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "HOY",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (ui.modoDiscreto) {
                    MONTO_OCULTO
                } else {
                    Dinero.formatear(ui.totalDeHoyCentavos)
                },
                style = TipografiaMostrador.total,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = when (ui.pagosDeHoy.size) {
                    1 -> "1 pago"
                    else -> "${ui.pagosDeHoy.size} pagos"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        TextButton(onClick = onSalir) {
            Icon(
                imageVector = Icons.Outlined.CloseFullscreen,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(Espaciado.corto))
            Text("Salir")
        }
    }
}

@Composable
private fun UltimoPago(
    pago: Payment,
    discreto: Boolean,
    estiloMonto: androidx.compose.ui.text.TextStyle,
) {
    Column(
        modifier = Modifier.padding(horizontal = Espaciado.amplio),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AvatarDeBilletera(wallet = pago.wallet, tamano = 64.dp)

        Spacer(Modifier.height(Espaciado.estandar))

        Text(
            text = if (discreto) MONTO_OCULTO else Dinero.formatear(pago.amountCents),
            style = estiloMonto,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(Espaciado.corto))

        Text(
            text = pago.payerName ?: "Sin nombre",
            style = TipografiaMostrador.detalle,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(Espaciado.corto))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = pago.wallet.nombreVisible,
                style = TipografiaMostrador.etiqueta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = Espaciado.medio)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant),
            )
            val hora = Calendario.horaDe(pago.notifPostedAtMillis)
            Text(
                text = "%02d:%02d".format(hora.hour, hora.minute),
                style = TipografiaMostrador.etiqueta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Esperando() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.HourglassEmpty,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(Espaciado.estandar))
        Text(
            text = "Esperando pagos",
            style = TipografiaMostrador.detalle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
