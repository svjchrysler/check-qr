package com.seef.checkqr.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.seef.checkqr.core.common.Dinero
import com.seef.checkqr.core.common.Plural
import com.seef.checkqr.core.model.Payment

/**
 * El widget de la pantalla de inicio.
 *
 * Dos tamanos, resueltos con `SizeMode.Responsive` y no con dos widgets
 * distintos: el sistema elige el diseno segun el hueco que le de el usuario, y
 * no hay que mantener dos entradas en el manifest.
 *
 * El **modo discreto** oculta los montos. No es un adorno: el widget se ve en
 * la pantalla de inicio de un celular que esta sobre el mostrador, a la vista
 * de cualquiera que pase.
 */
class WidgetDeCaja : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(CHICO, MEDIANO))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val datos = DatosDelWidget.leer(context)

        provideContent {
            GlanceTheme {
                val tamano = LocalSize.current
                if (tamano.height < MEDIANO.height) {
                    Chico(datos)
                } else {
                    Mediano(datos)
                }
            }
        }
    }

    /** Chico: total del dia y estado. Es lo que se mira de reojo. */
    @Composable
    private fun Chico(datos: DatosDelWidget) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .padding(12.dp)
                .clickable(actionStartActivity(intentDeApertura(LocalContext.current))),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Hoy",
                style = TextStyle(fontSize = 12.sp(), color = GlanceTheme.colors.onSurfaceVariant),
            )
            Text(
                text = datos.totalVisible(),
                style = TextStyle(
                    fontSize = 22.sp(),
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.primary,
                ),
            )
            Text(
                text = datos.etiquetaDeEstado(),
                style = TextStyle(
                    fontSize = 11.sp(),
                    // Del tema y no de un hex fijo: asi el widget respeta el
                    // modo oscuro. Rojo solo si dejo de escuchar, que es el
                    // unico caso donde el comerciante tiene que actuar ya.
                    color = if (datos.listenerConectado) {
                        GlanceTheme.colors.onSurfaceVariant
                    } else {
                        GlanceTheme.colors.error
                    },
                ),
            )
        }
    }

    /** Mediano: los ultimos 3 pagos y el boton "Verificar". */
    @Composable
    private fun Mediano(datos: DatosDelWidget) {
        val context = LocalContext.current
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .padding(12.dp),
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionStartActivity(intentDeApertura(context))),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Hoy · " + Plural.pagos(datos.cantidad),
                        style = TextStyle(fontSize = 12.sp(), color = GlanceTheme.colors.onSurfaceVariant),
                    )
                    Text(
                        text = datos.totalVisible(),
                        style = TextStyle(
                            fontSize = 20.sp(),
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.primary,
                        ),
                    )
                }
                Text(
                    text = "Verificar",
                    style = TextStyle(
                        fontSize = 14.sp(),
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.primary,
                    ),
                    modifier = GlanceModifier
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .clickable(actionStartActivity(intentDeVerificar(context))),
                )
            }

            Spacer(GlanceModifier.height(8.dp))

            if (datos.ultimos.isEmpty()) {
                Text(
                    text = "Todavía no llegó ningún pago hoy",
                    style = TextStyle(fontSize = 12.sp(), color = GlanceTheme.colors.onSurfaceVariant),
                )
            } else {
                datos.ultimos.take(3).forEach { pago ->
                    FilaCompacta(pago, datos.discreto)
                }
            }
        }
    }

    @Composable
    private fun FilaCompacta(pago: Payment, discreto: Boolean) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (discreto) MONTO_OCULTO else Dinero.formatear(pago.amountCents),
                style = TextStyle(fontSize = 13.sp(), fontWeight = FontWeight.Medium),
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = pago.wallet.nombreVisible,
                style = TextStyle(fontSize = 11.sp(), color = GlanceTheme.colors.onSurfaceVariant),
            )
        }
    }

    private fun intentDeApertura(context: Context): Intent =
        Intent(Intent.ACTION_VIEW, android.net.Uri.parse("checkqr://checkqr.app/caja")).apply {
            `package` = context.packageName
        }

    private fun intentDeVerificar(context: Context): Intent =
        Intent(Intent.ACTION_VIEW, android.net.Uri.parse("checkqr://checkqr.app/verificar")).apply {
            `package` = context.packageName
        }

    internal companion object {
        const val MONTO_OCULTO = "•• ••"

        /** Los dos cortes de tamano. Glance elige el diseno segun el hueco real. */
        val CHICO = DpSize(120.dp, 60.dp)
        val MEDIANO = DpSize(250.dp, 140.dp)
    }
}

/** Receptor del widget. */
class ReceptorDelWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WidgetDeCaja()
}

private fun Int.sp() = androidx.compose.ui.unit.TextUnit(
    this.toFloat(),
    androidx.compose.ui.unit.TextUnitType.Sp,
)
