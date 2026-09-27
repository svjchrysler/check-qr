package com.seef.checkqr.feature.verificar

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.seef.checkqr.core.common.Dinero
import com.seef.checkqr.core.designsystem.componentes.AvatarDeBilletera
import com.seef.checkqr.core.designsystem.theme.Espaciado
import com.seef.checkqr.core.designsystem.theme.Medidas
import com.seef.checkqr.core.model.MatchResult
import com.seef.checkqr.core.model.Payment

/**
 * El veredicto, en la pantalla.
 *
 * El cajero mira esto con el cliente enfrente esperando: la respuesta tiene que
 * leerse en menos de un segundo. Por eso el color de fondo ocupa toda la
 * pantalla y hay un icono grande — verde con tilde, rojo con cruz — antes que
 * cualquier texto. La explicacion viene despues, para quien quiera el detalle.
 *
 * El color nunca va solo: siempre hay icono y palabra, porque este es justamente
 * el momento en que confundir "sí" con "no" cuesta dinero.
 */
@Composable
internal fun Resultado(
    estado: EstadoDeVerificacion.Resuelto,
    onElegir: (Payment) -> Unit,
    onReiniciar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val v = estado.veredicto
    val aspecto = aspectoDe(v.resultado)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = aspecto.fondo,
        contentColor = aspecto.contenido,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Espaciado.amplio),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(aspecto.contenido.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = aspecto.icono,
                    contentDescription = null,
                    tint = aspecto.contenido,
                    modifier = Modifier.size(64.dp),
                )
            }

            Spacer(Modifier.height(Espaciado.amplio))

            Text(
                text = aspecto.titulo,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )

            v.pago?.let { p ->
                Spacer(Modifier.height(Espaciado.amplio))
                TarjetaDelPago(p, aspecto.contenido)
            }

            Spacer(Modifier.height(Espaciado.estandar))

            Text(
                text = v.explicacion,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = aspecto.contenido.copy(alpha = 0.8f),
            )

            // Con varios candidatos elige la persona: aqui no se adivina.
            if (v.resultado == MatchResult.AMBIGUO && v.candidatos.isNotEmpty()) {
                Spacer(Modifier.height(Espaciado.amplio))
                v.candidatos.forEach { p ->
                    OutlinedButton(
                        onClick = { onElegir(p) },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Espaciado.minimo)
                            .height(Medidas.objetivoTactil + 8.dp),
                    ) {
                        AvatarDeBilletera(p.wallet, tamano = 28.dp)
                        Spacer(Modifier.size(Espaciado.medio))
                        Text(
                            text = "${Dinero.formatear(p.amountCents)} · " +
                                (p.payerName ?: "sin nombre"),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Spacer(Modifier.height(Espaciado.seccion))

            Button(
                onClick = onReiniciar,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth().height(Medidas.botonPrincipal),
            ) {
                Text("Verificar otro", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun TarjetaDelPago(pago: Payment, contenido: Color) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = contenido.copy(alpha = 0.08f),
        contentColor = contenido,
    ) {
        Row(
            modifier = Modifier.padding(Espaciado.estandar),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarDeBilletera(pago.wallet)
            Spacer(Modifier.size(Espaciado.medio))
            Column {
                Text(
                    text = Dinero.formatear(pago.amountCents),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "${pago.payerName ?: "Sin nombre"} · ${pago.wallet.nombreVisible}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = contenido.copy(alpha = 0.8f),
                )
            }
        }
    }
}

private data class AspectoDeVeredicto(
    val icono: ImageVector,
    val titulo: String,
    val fondo: Color,
    val contenido: Color,
)

@Composable
private fun aspectoDe(resultado: MatchResult): AspectoDeVeredicto = when (resultado) {
    MatchResult.COINCIDE -> AspectoDeVeredicto(
        icono = Icons.Rounded.Check,
        titulo = "Sí llegó",
        fondo = MaterialTheme.colorScheme.primaryContainer,
        contenido = MaterialTheme.colorScheme.onPrimaryContainer,
    )
    MatchResult.NO_LLEGO -> AspectoDeVeredicto(
        icono = Icons.Rounded.Close,
        titulo = "No llegó",
        fondo = MaterialTheme.colorScheme.errorContainer,
        contenido = MaterialTheme.colorScheme.onErrorContainer,
    )
    MatchResult.AMBIGUO -> AspectoDeVeredicto(
        icono = Icons.Rounded.QuestionMark,
        titulo = "Hay que revisar",
        fondo = MaterialTheme.colorScheme.tertiaryContainer,
        contenido = MaterialTheme.colorScheme.onTertiaryContainer,
    )
    MatchResult.YA_RECLAMADO -> AspectoDeVeredicto(
        icono = Icons.Rounded.PriorityHigh,
        titulo = "Ya verificado antes",
        fondo = MaterialTheme.colorScheme.tertiaryContainer,
        contenido = MaterialTheme.colorScheme.onTertiaryContainer,
    )
}
