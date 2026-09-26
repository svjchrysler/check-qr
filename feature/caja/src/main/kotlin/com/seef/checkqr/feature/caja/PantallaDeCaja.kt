package com.seef.checkqr.feature.caja

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowWidthSizeClass
import com.seef.checkqr.core.common.Calendario
import com.seef.checkqr.core.common.Dinero
import com.seef.checkqr.core.designsystem.componentes.Aviso
import com.seef.checkqr.core.designsystem.componentes.BotonDeCaja
import com.seef.checkqr.core.designsystem.componentes.FilaDePago
import com.seef.checkqr.core.designsystem.componentes.NivelDeAviso
import com.seef.checkqr.core.designsystem.theme.TipografiaMostrador
import com.seef.checkqr.core.model.ParseConfidence
import com.seef.checkqr.core.model.Payment
import com.seef.checkqr.voice.EstadoDeVoz
import com.seef.checkqr.voice.LectorDeVoz

@Composable
fun PantallaDeCaja(
    modifier: Modifier = Modifier,
    vm: CajaViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // El modo mostrador mantiene la pantalla encendida: el celular queda apoyado
    // y el comerciante no lo toca entre cobro y cobro.
    MantenerPantallaEncendida(activo = ui.modoMostrador)

    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        when {
            ui.cargando -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            ui.modoMostrador -> Mostrador(
                ui = ui,
                onSalir = vm::alternarMostrador,
                modifier = Modifier.padding(padding),
            )

            else -> Caja(
                ui = ui,
                onAbrir = vm::abrirCaja,
                onCerrar = vm::cerrarCaja,
                onMostrador = vm::alternarMostrador,
                onDiscreto = vm::alternarDiscreto,
                onDescartarOculto = vm::descartarAvisoOculto,
                onInstalarVoz = {
                    context.startActivity(LectorDeVoz.intentDeInstalarVoz())
                },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun Caja(
    ui: UiCaja,
    onAbrir: () -> Unit,
    onCerrar: () -> Unit,
    onMostrador: () -> Unit,
    onDiscreto: () -> Unit,
    onDescartarOculto: () -> Unit,
    onInstalarVoz: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val caja = ui.caja
    val sistema = ui.estadoDelSistema

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Cobrado hoy", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = if (ui.modoDiscreto) "•• ••" else Dinero.formatear(ui.totalDeHoyCentavos),
                    style = TipografiaMostrador.total,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "${ui.pagosDeHoy.size} pagos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // --- Avisos, en orden de urgencia ---

        if (caja?.hayQueReactivarVoz == true) {
            item {
                Aviso(
                    titulo = "Toca para reactivar la voz",
                    detalle = "El celular se reinició. La app siguió anotando los pagos, " +
                        "pero para que vuelva a anunciarlos hay que abrir la caja de nuevo.",
                    nivel = NivelDeAviso.PROBLEMA,
                    textoDeAccion = "Abrir caja",
                    onAccion = onAbrir,
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        if (ui.avisoConContenidoOculto != null) {
            item {
                Aviso(
                    titulo = "Revisa tu app del banco",
                    detalle = "Llegó un aviso del banco pero el sistema ocultó su contenido, " +
                        "así que no se pudo leer el monto.",
                    nivel = NivelDeAviso.ATENCION,
                    textoDeAccion = "Entendido",
                    onAccion = onDescartarOculto,
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        if (caja?.faltaInstalarVoz == true) {
            item {
                Aviso(
                    titulo = "Falta la voz en español",
                    detalle = "El celular no tiene instalada la voz en español, " +
                        "así que la app no puede anunciar los pagos.",
                    nivel = NivelDeAviso.PROBLEMA,
                    textoDeAccion = "Instalar",
                    onAccion = onInstalarVoz,
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        if (sistema != null && !sistema.listenerConectado) {
            item {
                Aviso(
                    titulo = "El celular dejó de escuchar los pagos",
                    detalle = "Hay que darle permiso a CheckQr para ver los avisos de las " +
                        "apps del banco desde los ajustes del sistema.",
                    nivel = NivelDeAviso.PROBLEMA,
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        if (sistema?.plantillasSinVerificar == true) {
            item {
                Aviso(
                    titulo = "Plantillas de banco sin verificar",
                    detalle = "Las plantillas que vienen con esta versión no se han " +
                        "comprobado todavía contra avisos reales de los bancos. " +
                        "Haz un pago de prueba de Bs 1 con cada billetera.",
                    nivel = NivelDeAviso.ATENCION,
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        item {
            BotonDeCaja(
                abierta = caja?.cajaAbierta == true,
                onClick = if (caja?.cajaAbierta == true) onCerrar else onAbrir,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TextButton(onClick = onMostrador) { Text("Modo mostrador") }
                TextButton(onClick = onDiscreto) {
                    Text(if (ui.modoDiscreto) "Mostrar montos" else "Ocultar montos")
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (ui.pagosDeHoy.isEmpty()) {
            item {
                Text(
                    text = "Todavía no llegó ningún pago hoy.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                )
            }
        } else {
            items(ui.pagosDeHoy, key = Payment::id) { pago ->
                FilaDePago(
                    montoCentavos = pago.amountCents,
                    pagador = pago.payerName,
                    billetera = pago.wallet.nombreVisible,
                    hora = horaDe(pago),
                    discreto = ui.modoDiscreto,
                    incompleto = pago.confidence == ParseConfidence.PARCIAL,
                    reclamado = pago.estaReclamado,
                )
            }
        }
    }
}

/**
 * Modo mostrador: el ultimo pago en letra enorme.
 *
 * Adaptable desde el primer dia y sin orientacion fija: Android 17 ignora las
 * restricciones de orientacion y tamano en pantallas grandes, asi que fijarla no
 * es una opcion.
 */
@Composable
private fun Mostrador(
    ui: UiCaja,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ancho = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    val estiloMonto = if (ancho == WindowWidthSizeClass.COMPACT) {
        TipografiaMostrador.montoCompacto
    } else {
        TipografiaMostrador.monto
    }

    val ultimo = ui.pagosDeHoy.firstOrNull()

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (ultimo == null) {
            Text(
                text = "Esperando pagos",
                style = TipografiaMostrador.detalle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = if (ui.modoDiscreto) "•• ••" else Dinero.formatear(ultimo.amountCents),
                style = estiloMonto,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = ultimo.payerName ?: "Sin nombre",
                style = TipografiaMostrador.detalle,
            )
            Text(
                text = ultimo.wallet.nombreVisible,
                style = TipografiaMostrador.etiqueta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Hoy: ${if (ui.modoDiscreto) "•• ••" else Dinero.formatear(ui.totalDeHoyCentavos)}" +
                "  ·  ${ui.pagosDeHoy.size} pagos",
            style = TipografiaMostrador.etiqueta,
        )

        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onSalir) { Text("Salir del modo mostrador") }
    }
}

private fun horaDe(pago: Payment): String {
    val hora = Calendario.horaDe(pago.notifPostedAtMillis)
    return "%02d:%02d".format(hora.hour, hora.minute)
}
