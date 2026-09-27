package com.seef.checkqr.feature.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
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
import com.seef.checkqr.core.designsystem.componentes.Aviso
import com.seef.checkqr.core.designsystem.componentes.NivelDeAviso
import com.seef.checkqr.core.model.Wallet
import com.seef.checkqr.voice.LectorDeVoz

@Composable
fun PantallaDeOnboarding(
    onTerminado: () -> Unit,
    modifier: Modifier = Modifier,
    vm: OnboardingViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (ui.paso == PasoDelOnboarding.LISTO) {
        onTerminado()
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        val total = PasoDelOnboarding.entries.size - 1
        val actual = PasoDelOnboarding.entries.indexOf(ui.paso) + 1
        LinearProgressIndicator(
            progress = { actual.toFloat() / total },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Paso $actual de $total",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )

        when (ui.paso) {
            PasoDelOnboarding.POR_QUE -> PorQue(onSiguiente = vm::siguiente)

            PasoDelOnboarding.PERMISO_DE_AVISOS -> PermisoDeAvisos(onSiguiente = vm::siguiente)

            PasoDelOnboarding.ACCESO_A_NOTIFICACIONES -> AccesoANotificaciones(
                concedido = ui.accesoAAvisosConcedido,
                onAbrirAjustes = { context.startActivity(vm.intentDeAjustesDeAvisos()) },
                onComprobar = vm::refrescarAcceso,
                onSiguiente = vm::siguiente,
            )

            PasoDelOnboarding.BATERIA -> Bateria(
                guia = ui.guiaDeBateria,
                onAbrir = {
                    val intent = ui.guiaDeBateria?.intentEspecifico
                        ?: AjustesDeBateria.intentDeAjustesDeLaApp(context)
                    context.startActivity(intent)
                },
                onSiguiente = vm::siguiente,
            )

            PasoDelOnboarding.VOZ -> Voz(
                ui = ui,
                onProbar = vm::probarVoz,
                onInstalar = { context.startActivity(LectorDeVoz.intentDeInstalarVoz()) },
                onSiguiente = vm::siguiente,
            )

            PasoDelOnboarding.PRUEBA -> Prueba(
                probadas = ui.billeterasProbadas,
                completo = ui.todasLasBilleterasProbadas,
                onTerminar = vm::terminar,
                onSaltar = vm::saltarPrueba,
            )

            PasoDelOnboarding.LISTO -> Unit
        }
    }
}

@Composable
private fun PorQue(onSiguiente: () -> Unit) {
    Titulo("CheckQr te avisa por voz cuando te pagan")
    Parrafo(
        "Cuando un cliente te transfiere, la app de tu banco te manda un aviso. " +
            "CheckQr lee ese aviso y lo dice en voz alta, para que no tengas que " +
            "sacar el celular ni revisar la pantalla mientras atiendes.",
    )
    Spacer(Modifier.height(16.dp))
    Titulo("Por eso te va a pedir ver tus avisos", pequeno = true)
    Parrafo(
        "En el siguiente paso Android te va a preguntar si CheckQr puede ver tus " +
            "notificaciones. Es lo que hace posible todo lo demás.",
    )
    Spacer(Modifier.height(8.dp))
    Parrafo(
        "CheckQr solo mira los avisos de las apps de los 6 bancos de la lista. " +
            "Todo lo demás — tus mensajes, tus fotos, tus otras apps — se descarta " +
            "al instante y nunca se guarda ni se envía a ningún lado.",
    )
    Siguiente(onSiguiente, "Entendido")
}

@Composable
private fun PermisoDeAvisos(onSiguiente: () -> Unit) {
    val lanzador = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { onSiguiente() }

    Titulo("Permite que CheckQr te muestre avisos")
    Parrafo(
        "Mientras la caja está abierta, CheckQr deja un aviso fijo para que Android " +
            "no la apague. Sin ese aviso, la voz no puede funcionar.",
    )
    Siguiente(
        onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                lanzador.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onSiguiente()
            }
        },
        texto = "Permitir",
    )
}

@Composable
private fun AccesoANotificaciones(
    concedido: Boolean,
    onAbrirAjustes: () -> Unit,
    onComprobar: () -> Unit,
    onSiguiente: () -> Unit,
) {
    Titulo("Deja que CheckQr vea los avisos del banco")
    Parrafo(
        "Android no permite pedir esto con un botón: hay que activarlo a mano en " +
            "los ajustes. Toca «Abrir ajustes», busca CheckQr en la lista y actívalo.",
    )
    Spacer(Modifier.height(16.dp))

    if (concedido) {
        Aviso(
            titulo = "Listo, ya puede ver los avisos",
            nivel = NivelDeAviso.INFORMATIVO,
        )
        Siguiente(onSiguiente, "Continuar")
    } else {
        Button(onClick = onAbrirAjustes, modifier = Modifier.fillMaxWidth()) {
            Text("Abrir ajustes")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onComprobar, modifier = Modifier.fillMaxWidth()) {
            Text("Ya lo activé, comprobar")
        }
        Spacer(Modifier.height(16.dp))
        Aviso(
            titulo = "Sin esto la app no puede capturar ningún pago",
            detalle = "Es el permiso central: todo lo demás depende de él.",
            nivel = NivelDeAviso.ATENCION,
        )
    }
}

@Composable
private fun Bateria(
    guia: AjustesDeBateria.Guia?,
    onAbrir: () -> Unit,
    onSiguiente: () -> Unit,
) {
    Titulo("Evita que tu ${guia?.marca ?: "celular"} apague CheckQr")
    Parrafo(
        "Muchos celulares cierran las apps que llevan rato sin usarse para ahorrar " +
            "batería. Si le pasa a CheckQr, dejarías de recibir los avisos sin " +
            "enterarte. Estos pasos lo evitan.",
    )
    Spacer(Modifier.height(16.dp))

    guia?.pasos?.forEachIndexed { i, paso ->
        Row(modifier = Modifier.padding(vertical = 4.dp)) {
            Text("${i + 1}.  ", fontWeight = FontWeight.Bold)
            Text(paso, style = MaterialTheme.typography.bodyMedium)
        }
    }

    Spacer(Modifier.height(16.dp))
    Button(onClick = onAbrir, modifier = Modifier.fillMaxWidth()) { Text("Abrir ajustes") }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = onSiguiente, modifier = Modifier.fillMaxWidth()) {
        Text("Ya está, continuar")
    }
}

@Composable
private fun Voz(
    ui: UiOnboarding,
    onProbar: () -> Unit,
    onInstalar: () -> Unit,
    onSiguiente: () -> Unit,
) {
    Titulo("Escucha cómo suena")
    Parrafo(
        "CheckQr habla en español y funciona sin internet, siempre que el celular " +
            "tenga la voz en español instalada.",
    )
    Spacer(Modifier.height(16.dp))

    when {
        ui.faltaInstalarVoz -> {
            Aviso(
                titulo = "Falta la voz en español",
                detalle = "Tu celular no la tiene instalada. Sin ella, CheckQr no " +
                    "puede anunciar los pagos.",
                nivel = NivelDeAviso.PROBLEMA,
                textoDeAccion = "Instalar",
                onAccion = onInstalar,
            )
        }
        ui.vozLista -> {
            Button(onClick = onProbar, modifier = Modifier.fillMaxWidth()) {
                Text("Probar la voz")
            }
            Spacer(Modifier.height(8.dp))
            Siguiente(onSiguiente, "Se escucha bien, continuar")
        }
        else -> Text("Preparando la voz…", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Prueba(
    probadas: Set<Wallet>,
    completo: Boolean,
    onTerminar: () -> Unit,
    onSaltar: () -> Unit,
) {
    Titulo("Haz una prueba de Bs 1 con cada billetera")
    Parrafo(
        "Es el único modo de saber con certeza que CheckQr entiende los avisos de " +
            "tu banco en tu celular. Pídele a alguien que te mande Bs 1 desde cada " +
            "una, o hazlo tú desde otra cuenta.",
    )
    Spacer(Modifier.height(8.dp))
    Parrafo("El tilde aparece solo en cuanto llegue el pago. No tienes que tocar nada.")
    Spacer(Modifier.height(16.dp))

    Wallet.soportadas.forEach { billetera ->
        val ok = billetera in probadas
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(billetera.nombreVisible, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (ok) "✓ probado" else "sin probar",
                style = MaterialTheme.typography.labelLarge,
                color = if (ok) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    if (completo) {
        Aviso(
            titulo = "Las 6 billeteras funcionan en este celular",
            nivel = NivelDeAviso.INFORMATIVO,
        )
        Spacer(Modifier.height(8.dp))
        Siguiente(onTerminar, "Empezar a usar CheckQr")
    } else {
        Siguiente(onTerminar, "Empezar a usar CheckQr")
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSaltar, modifier = Modifier.fillMaxWidth()) {
            Text("Probar más tarde")
        }
        Spacer(Modifier.height(8.dp))
        Aviso(
            titulo = "Las billeteras sin probar pueden no funcionar",
            detalle = "Si un banco cambió el formato de sus avisos, CheckQr no lo " +
                "sabrá hasta que llegue el primero. Puedes ver esto cuando quieras " +
                "en Estado del sistema.",
            nivel = NivelDeAviso.ATENCION,
        )
    }
}

// --- Piezas comunes -----------------------------------------------------------

@Composable
private fun Titulo(texto: String, pequeno: Boolean = false) {
    Text(
        text = texto,
        style = if (pequeno) {
            MaterialTheme.typography.titleMedium
        } else {
            MaterialTheme.typography.headlineSmall
        },
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

@Composable
private fun Parrafo(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun Siguiente(onClick: () -> Unit, texto: String) {
    Spacer(Modifier.height(24.dp))
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Text(texto, style = MaterialTheme.typography.titleMedium)
    }
}
