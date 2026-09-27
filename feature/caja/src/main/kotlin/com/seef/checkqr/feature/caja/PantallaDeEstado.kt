package com.seef.checkqr.feature.caja

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seef.checkqr.core.common.Calendario
import com.seef.checkqr.core.designsystem.componentes.Aviso
import com.seef.checkqr.core.designsystem.componentes.NivelDeAviso
import com.seef.checkqr.core.model.Wallet

/**
 * Estado del sistema.
 *
 * Existe porque el modo de fallo por omision de esta app es el silencio: si el
 * listener se desconecta, si una marca mata el servicio, o si un banco cambia el
 * formato de sus avisos, no hay ningun error visible — simplemente dejan de
 * llegar pagos. Esta pantalla convierte ese silencio en algo que se puede mirar.
 */
@Composable
fun PantallaDeEstado(
    modifier: Modifier = Modifier,
    vm: CajaViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val sistema = ui.estadoDelSistema

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Text("Estado del sistema", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        if (sistema == null) {
            Text("Cargando…")
            return@Column
        }

        if (!sistema.listenerConectado) {
            Aviso(
                titulo = "El celular dejó de escuchar los pagos",
                detalle = "Hay que volver a darle permiso a CheckQr para ver los avisos " +
                    "de las apps del banco.",
                nivel = NivelDeAviso.PROBLEMA,
            )
            Spacer(Modifier.height(12.dp))
        }

        if (sistema.plantillasSinVerificar) {
            Aviso(
                titulo = "Plantillas de banco sin verificar",
                detalle = "Las plantillas que trae esta versión no se probaron todavía " +
                    "contra avisos reales. Haz un pago de Bs 1 con cada billetera.",
                nivel = NivelDeAviso.ATENCION,
            )
            Spacer(Modifier.height(12.dp))
        }

        if (sistema.avisosNoReconocidos > 0) {
            Aviso(
                titulo = "${sistema.avisosNoReconocidos} avisos no reconocidos",
                detalle = "Llegaron avisos de una app de banco que CheckQr no supo leer. " +
                    "Suele significar que el banco cambió el formato de sus mensajes.",
                nivel = NivelDeAviso.ATENCION,
            )
            Spacer(Modifier.height(12.dp))
        }

        Text(
            "Último aviso por banco",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 8.dp),
        )

        Wallet.soportadas.forEach { billetera ->
            val senal = sistema.ultimoAvisoPorBilletera[billetera]
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(billetera.nombreVisible, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = when {
                        senal == null -> "nunca"
                        senal.ultimoOculto -> "oculto por el sistema"
                        else -> {
                            val h = Calendario.horaDe(senal.ultimoAvisoMillis)
                            val d = Calendario.diaDe(senal.ultimoAvisoMillis)
                            "$d %02d:%02d".format(h.hour, h.minute)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (senal == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
            HorizontalDivider()
        }

        if (sistema.billeterasSinSenal.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Aviso(
                titulo = "${sistema.billeterasSinSenal.size} billeteras sin ninguna señal",
                detalle = "Nunca llegó un aviso de: " +
                    sistema.billeterasSinSenal.joinToString { it.nombreVisible } +
                    ". Puede ser que no las uses, o que CheckQr no reconozca su app.",
                nivel = NivelDeAviso.INFORMATIVO,
            )
        }
    }
}
