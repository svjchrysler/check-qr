package com.seef.checkqr.feature.equipo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seef.checkqr.core.designsystem.componentes.Aviso
import com.seef.checkqr.core.designsystem.componentes.CabeceraSimple
import com.seef.checkqr.core.designsystem.componentes.NivelDeAviso
import com.seef.checkqr.core.model.Role
import com.seef.checkqr.core.model.TeamMember

@Composable
fun PantallaDeEquipo(
    modifier: Modifier = Modifier,
    vm: EquipoViewModel = hiltViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val miRol by vm.miRol.collectAsStateWithLifecycle()

    // Sin padding en la raiz: la cabecera de la lista tiene que llegar hasta el
    // borde y aplicar el inset de la barra de estado por dentro, como en las
    // demas pantallas. Cada rama pone el suyo.
    Column(modifier = modifier.fillMaxSize()) {
        when (val e = estado) {
            is EstadoDeEquipo.Lista -> Lista(
                miembros = e.miembros,
                miRol = miRol,
                onInvitar = vm::invitar,
                onEscanear = vm::escanear,
            )

            is EstadoDeEquipo.MostrandoQr -> Column(Modifier.padding(16.dp)) {
                MostrarQr(
                    carga = e.carga,
                    onVolver = vm::volver,
                )
            }

            is EstadoDeEquipo.Escaneando -> Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    "Apunta al QR que te muestre el dueño",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                EscanerDeQr(onTexto = vm::qrEscaneado, modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = vm::volver,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) { Text("Cancelar") }
            }

            is EstadoDeEquipo.Aviso -> Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Aviso(titulo = e.mensaje, nivel = NivelDeAviso.ATENCION)
                Spacer(Modifier.height(16.dp))
                Button(onClick = vm::volver, modifier = Modifier.fillMaxWidth()) {
                    Text("Volver")
                }
            }
        }
    }
}

@Composable
private fun Lista(
    miembros: List<TeamMember>,
    miRol: Role,
    onInvitar: (Role) -> Unit,
    onEscanear: () -> Unit,
) {
    // El mismo bloque de marca que el resto de las pantallas. Ademas de la
    // coherencia visual resuelve el inset: la cabecera aplica el de la barra de
    // estado por dentro, y antes el titulo se dibujaba encima del reloj.
    CabeceraSimple(
        titulo = "Tu equipo",
        subtitulo = "Tú eres ${miRol.nombreVisible}",
    )

    Column(modifier = Modifier.padding(16.dp)) {
        ContenidoDeLaLista(miembros, miRol, onInvitar, onEscanear)
    }
}

@Composable
private fun ContenidoDeLaLista(
    miembros: List<TeamMember>,
    miRol: Role,
    onInvitar: (Role) -> Unit,
    onEscanear: () -> Unit,
) {
    if (miembros.isEmpty()) {
        Text(
            "Todavía no hay nadie más en el equipo.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        miembros.forEach { m ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(m.nombre, style = MaterialTheme.typography.bodyLarge)
                Text(
                    m.role.nombreVisible,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
        }
    }

    Spacer(Modifier.height(24.dp))

    // Solo el dueno invita. La regla vive en Role, no repartida por la UI.
    if (miRol.puedeGestionarEquipo) {
        Button(
            onClick = { onInvitar(Role.CAJERO) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Invitar a un cajero") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { onInvitar(Role.ENCARGADO) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Invitar a un encargado") }
        Spacer(Modifier.height(16.dp))
    }

    OutlinedButton(onClick = onEscanear, modifier = Modifier.fillMaxWidth()) {
        Text("Unirme escaneando un QR")
    }
}

@Composable
private fun MostrarQr(carga: CargaDeInvitacion, onVolver: () -> Unit) {
    val bitmap = remember(carga) { InvitacionQr.aBitmap(carga) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Que lo escanee con su celular",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Código QR de invitación",
            modifier = Modifier.size(280.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Entra como ${carga.rolTipado?.nombreVisible}. El código sirve una sola vez.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        OutlinedButton(onClick = onVolver) { Text("Listo") }
    }
}
