package com.seef.checkqr.feature.cuadre

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seef.checkqr.core.common.Calendario
import com.seef.checkqr.core.common.Dinero
import com.seef.checkqr.core.designsystem.componentes.Aviso
import com.seef.checkqr.core.designsystem.componentes.FilaDePago
import com.seef.checkqr.core.designsystem.componentes.NivelDeAviso
import com.seef.checkqr.core.designsystem.theme.TipografiaMostrador
import com.seef.checkqr.core.model.Payment

@Composable
fun PantallaDeCuadre(
    modifier: Modifier = Modifier,
    vm: CuadreViewModel = hiltViewModel(),
) {
    val cuadre by vm.cuadre.collectAsStateWithLifecycle()
    val dia by vm.dia.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val compartir: (FormatoDeExportacion) -> Unit = { formato ->
        vm.exportar(formato) { intent ->
            context.startActivity(
                android.content.Intent.createChooser(intent, "Compartir cuadre"),
            )
        }
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = vm::diaAnterior) { Text("‹ Anterior") }
                Text(dia.toString(), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = vm::diaSiguiente) { Text("Siguiente ›") }
            }
        }

        val c = cuadre
        if (c == null) {
            item { Text("Cargando…", modifier = Modifier.padding(16.dp)) }
            return@LazyColumn
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Total del día", style = MaterialTheme.typography.labelLarge)
                Text(
                    Dinero.formatear(c.totalCentavos),
                    style = TipografiaMostrador.total,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text("${c.cantidad} pagos", style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (!c.cuadra) {
            // No deberia pasar nunca; si pasa, es un fallo de agrupacion y el
            // comerciante tiene que verlo en vez de confiar en un total mal sumado.
            item {
                Aviso(
                    titulo = "El desglose no suma el total",
                    detalle = "Avisa del problema: el total es correcto, pero el detalle " +
                        "por billetera no cuadra.",
                    nivel = NivelDeAviso.PROBLEMA,
                )
            }
        }

        bloque("Por billetera", c.porBilletera)
        bloque("Por cajero", c.porCajero)
        bloque("Por turno", c.porTurno)
        if (c.sinTurno.cantidad > 0) {
            bloque("Con la caja cerrada", listOf(c.sinTurno))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { compartir(FormatoDeExportacion.PDF) },
                    modifier = Modifier.weight(1f),
                ) { Text("Compartir PDF") }
                OutlinedButton(
                    onClick = { compartir(FormatoDeExportacion.CSV) },
                    modifier = Modifier.weight(1f),
                ) { Text("Compartir Excel") }
            }
        }

        item {
            Text(
                "Detalle",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        items(c.pagos, key = Payment::id) { p ->
            val hora = Calendario.horaDe(p.notifPostedAtMillis)
            FilaDePago(
                montoCentavos = p.amountCents,
                pagador = p.payerName,
                billetera = p.wallet.nombreVisible,
                hora = "%02d:%02d".format(hora.hour, hora.minute),
                reclamado = p.estaReclamado,
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.bloque(
    titulo: String,
    renglones: List<RenglonDeCuadre>,
) {
    if (renglones.isEmpty()) return
    item {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(
                titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 6.dp),
            )
            renglones.forEach { r ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "${r.etiqueta} (${r.cantidad})",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        Dinero.formatear(r.totalCentavos),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            HorizontalDivider()
        }
    }
}
