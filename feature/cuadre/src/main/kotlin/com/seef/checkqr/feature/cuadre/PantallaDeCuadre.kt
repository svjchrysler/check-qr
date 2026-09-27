package com.seef.checkqr.feature.cuadre

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.TableChart
import com.seef.checkqr.core.designsystem.Iconos
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seef.checkqr.core.common.Calendario
import com.seef.checkqr.core.common.Dinero
import com.seef.checkqr.core.common.Plural
import com.seef.checkqr.core.designsystem.componentes.Aviso
import com.seef.checkqr.core.designsystem.componentes.CabeceraDeMarca
import com.seef.checkqr.core.designsystem.componentes.EncabezadoDeSeccion
import com.seef.checkqr.core.designsystem.componentes.EstadoVacio
import com.seef.checkqr.core.designsystem.componentes.FilaDePago
import com.seef.checkqr.core.designsystem.componentes.NivelDeAviso
import com.seef.checkqr.core.designsystem.componentes.SeparadorDeLista
import com.seef.checkqr.core.designsystem.theme.Espaciado
import com.seef.checkqr.core.designsystem.theme.Medidas
import com.seef.checkqr.core.designsystem.theme.Montos
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
        val c = cuadre

        item {
            CabeceraDeMarca(
                titulo = "Cuadre de caja",
                totalCentavos = c?.totalCentavos ?: 0L,
                cantidadDePagos = c?.cantidad ?: 0,
                etiquetaDelTotal = "Total del día",
                // Un dia que ya paso no "acaba de entrar": la cifra aparece
                // puesta, sin contar.
                animar = false,
            )
        }

        item {
            SelectorDeDia(
                etiqueta = etiquetaDeDia(dia),
                onAnterior = vm::diaAnterior,
                onSiguiente = vm::diaSiguiente,
            )
        }

        if (c == null) {
            item {
                Text(
                    "Cargando…",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(Espaciado.estandar),
                )
            }
            return@LazyColumn
        }

        if (!c.cuadra) {
            // No deberia pasar nunca; si pasa, es un fallo de agrupacion y el
            // comerciante tiene que verlo en vez de confiar en un total mal
            // sumado.
            item {
                Column(modifier = Modifier.padding(Espaciado.estandar)) {
                    Aviso(
                        titulo = "El desglose no suma el total",
                        detalle = "Avísanos del problema: el total es correcto, " +
                            "pero el detalle por billetera no cuadra.",
                        nivel = NivelDeAviso.PROBLEMA,
                    )
                }
            }
        }

        if (c.cantidad == 0) {
            item {
                EstadoVacio(
                    icono = Iconos.cuadre,
                    titulo = "Sin pagos este día",
                    detalle = "Usa las flechas de arriba para ver otro día.",
                )
            }
            return@LazyColumn
        }

        bloque("Por billetera", c.porBilletera)
        bloque("Por cajero", c.porCajero)
        bloque("Por turno", c.porTurno)
        if (c.sinTurno.cantidad > 0) {
            // El titulo dice donde encaja y la fila dice que son: repetir el
            // mismo texto en los dos sitios se lee como un error.
            bloque("Fuera de turno", listOf(c.sinTurno))
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Espaciado.estandar,
                        vertical = Espaciado.amplio,
                    ),
                horizontalArrangement = Arrangement.spacedBy(Espaciado.medio),
            ) {
                OutlinedButton(
                    onClick = { compartir(FormatoDeExportacion.PDF) },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f).height(Medidas.objetivoTactil),
                ) {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(Espaciado.corto))
                    Text("PDF")
                }
                OutlinedButton(
                    onClick = { compartir(FormatoDeExportacion.CSV) },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f).height(Medidas.objetivoTactil),
                ) {
                    Icon(
                        Icons.Outlined.TableChart,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(Espaciado.corto))
                    Text("Excel")
                }
            }
        }

        item { EncabezadoDeSeccion(texto = "Detalle", acompanante = "${c.cantidad}") }

        itemsIndexed(c.pagos) { indice, p ->
            Column {
                FilaDePago(
                    montoCentavos = p.amountCents,
                    pagador = p.payerName,
                    billetera = p.wallet,
                    hora = horaDe(p),
                    reclamado = p.estaReclamado,
                )
                if (indice < c.pagos.lastIndex) SeparadorDeLista()
            }
        }

        item { Spacer(Modifier.height(Espaciado.seccion)) }
    }
}

@Composable
private fun SelectorDeDia(
    etiqueta: String,
    onAnterior: () -> Unit,
    onSiguiente: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Espaciado.corto, vertical = Espaciado.corto),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onAnterior) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Día anterior",
            )
        }
        Text(etiqueta, style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onSiguiente) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Día siguiente",
            )
        }
    }
}

/**
 * Un bloque del desglose.
 *
 * Va sobre una superficie propia y con esquinas: separa visualmente "cómo se
 * reparte" de "la lista de pagos", que son dos lecturas distintas del mismo día.
 */
private fun LazyListScope.bloque(titulo: String, renglones: List<RenglonDeCuadre>) {
    if (renglones.isEmpty()) return
    item {
        Column(modifier = Modifier.padding(horizontal = Espaciado.estandar)) {
            Spacer(Modifier.height(Espaciado.estandar))
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = Espaciado.corto),
            )
            Surface(
                shape = MaterialTheme.shapes.large,
                // Blanco sobre el fondo gris: con surfaceContainer quedaba gris
                // sobre gris y el bloque no se leia como una tarjeta.
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = 1.dp,
            ) {
                Column(modifier = Modifier.padding(Espaciado.estandar)) {
                    renglones.forEachIndexed { i, r ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = Espaciado.corto),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(r.etiqueta, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = Plural.pagos(r.cantidad),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = Dinero.formatear(r.totalCentavos),
                                style = Montos.renglon,
                            )
                        }
                        if (i < renglones.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun horaDe(pago: Payment): String {
    val hora = Calendario.horaDe(pago.notifPostedAtMillis)
    return "%02d:%02d".format(hora.hour, hora.minute)
}

private fun etiquetaDeDia(dia: kotlinx.datetime.LocalDate): String {
    val meses = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
    )
    val hoy = Calendario.diaDe(System.currentTimeMillis())
    return when (dia) {
        hoy -> "Hoy"
        else -> "${dia.day} de ${meses[dia.month.ordinal]}"
    }
}
