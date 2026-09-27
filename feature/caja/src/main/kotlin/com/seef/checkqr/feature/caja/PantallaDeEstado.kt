package com.seef.checkqr.feature.caja

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seef.checkqr.core.common.Calendario
import com.seef.checkqr.core.common.Plural
import com.seef.checkqr.core.data.repositorios.SenalDeBanco
import com.seef.checkqr.core.designsystem.Iconos
import com.seef.checkqr.core.designsystem.componentes.AvatarDeBilletera
import com.seef.checkqr.core.designsystem.componentes.Aviso
import com.seef.checkqr.core.designsystem.componentes.AvisoDeExito
import com.seef.checkqr.core.designsystem.componentes.CabeceraSimple
import com.seef.checkqr.core.designsystem.componentes.EncabezadoDeSeccion
import com.seef.checkqr.core.designsystem.componentes.NivelDeAviso
import com.seef.checkqr.core.designsystem.theme.Espaciado
import com.seef.checkqr.core.model.Wallet

/**
 * Estado del sistema.
 *
 * Existe porque el modo de fallo por omision de esta app es el silencio: si el
 * listener se desconecta, si una marca mata el servicio, o si un banco cambia el
 * formato de sus avisos, no hay ningun error visible — simplemente dejan de
 * llegar pagos. Esta pantalla convierte ese silencio en algo que se puede mirar.
 *
 * Por eso lo primero que se ve es un veredicto en una linea: "Todo en orden" o
 * el problema concreto. El detalle viene despues, para quien quiera mirarlo.
 */
@Composable
fun PantallaDeEstado(
    modifier: Modifier = Modifier,
    vm: CajaViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val sistema = ui.estadoDelSistema

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        CabeceraSimple(
            titulo = "Estado",
            subtitulo = "Si CheckQr deja de escuchar, aquí se ve",
            escuchando = sistema?.listenerConectado != false,
        )

        if (sistema == null) {
            Text("Cargando…", modifier = Modifier.padding(Espaciado.estandar))
            return@Column
        }

        Column(modifier = Modifier.padding(horizontal = Espaciado.estandar)) {
            Spacer(Modifier.height(Espaciado.corto))

            // Si esta escuchando o no va siempre arriba y sin condicionar a lo
            // demas. Es la pregunta que trae al comerciante a esta pantalla
            // ("Si CheckQr deja de escuchar, aqui se ve"), y antes se la comia
            // cualquier otra advertencia: con las plantillas sin verificar
            // —hoy, siempre— la pantalla mostraba un aviso ambar y ni una
            // palabra sobre si la captura estaba funcionando.
            if (sistema.listenerConectado) {
                AvisoDeExito(
                    titulo = "CheckQr está escuchando",
                    detalle = "Los avisos de tus bancos llegan a la app.",
                )
            } else {
                Aviso(
                    titulo = "El celular dejó de escuchar los pagos",
                    detalle = "Hay que volver a darle permiso a CheckQr para ver " +
                        "los avisos de las apps del banco.",
                    nivel = NivelDeAviso.PROBLEMA,
                )
            }
            Spacer(Modifier.height(Espaciado.corto))

            if (sistema.requiereAtencion) {
                if (sistema.plantillasSinVerificar) {
                    Aviso(
                        titulo = "Plantillas de banco sin verificar",
                        detalle = "Las plantillas que trae esta versión no se probaron " +
                            "todavía contra avisos reales. Haz un pago de Bs 1 con cada " +
                            "billetera.",
                        nivel = NivelDeAviso.ATENCION,
                    )
                    Spacer(Modifier.height(Espaciado.corto))
                }
                if (sistema.avisosNoReconocidos > 0) {
                    Aviso(
                        titulo = Plural.contar(
                            sistema.avisosNoReconocidos,
                            "aviso no reconocido",
                            "avisos no reconocidos",
                        ),
                        detalle = "Llegaron avisos de una app de banco que CheckQr no " +
                            "supo leer. Suele significar que el banco cambió el formato " +
                            "de sus mensajes.",
                        nivel = NivelDeAviso.ATENCION,
                    )
                }
            }
        }

        EncabezadoDeSeccion(
            texto = "Último aviso por banco",
            acompanante = "${sistema.ultimoAvisoPorBilletera.size} de ${Wallet.soportadas.size}",
        )

        Surface(
            modifier = Modifier.padding(horizontal = Espaciado.estandar),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shadowElevation = 1.dp,
        ) {
            Column(modifier = Modifier.padding(vertical = Espaciado.corto)) {
                Wallet.soportadas.forEachIndexed { i, billetera ->
                    FilaDeBanco(
                        billetera = billetera,
                        senal = sistema.ultimoAvisoPorBilletera[billetera],
                    )
                    if (i < Wallet.soportadas.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                                .copy(alpha = 0.4f),
                        )
                    }
                }
            }
        }

        if (sistema.billeterasSinSenal.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(
                    horizontal = Espaciado.estandar,
                    vertical = Espaciado.estandar,
                ),
            ) {
                Aviso(
                    titulo = Plural.contar(
                        sistema.billeterasSinSenal.size,
                        "billetera sin ninguna señal",
                        "billeteras sin ninguna señal",
                    ),
                    detalle = "Nunca llegó un aviso de " +
                        sistema.billeterasSinSenal.joinToString { it.nombreVisible } +
                        ". Puede ser que no " +
                        Plural.palabra(sistema.billeterasSinSenal.size, "la uses", "las uses") +
                        ", o que CheckQr no reconozca su app.",
                    nivel = NivelDeAviso.INFORMATIVO,
                )
            }
        }

        Spacer(Modifier.height(Espaciado.seccion))
    }
}

@Composable
private fun FilaDeBanco(billetera: Wallet, senal: SenalDeBanco?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Espaciado.estandar, vertical = Espaciado.medio),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarDeBilletera(billetera, tamano = 36.dp)
            Spacer(Modifier.size(Espaciado.medio))
            Text(billetera.nombreVisible, style = MaterialTheme.typography.bodyLarge)
        }

        when {
            senal == null -> Text(
                text = "Sin señal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            senal.ultimoOculto -> Text(
                text = "Contenido oculto",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Medium,
            )

            else -> {
                val hora = Calendario.horaDe(senal.ultimoAvisoMillis)
                val dia = Calendario.diaDe(senal.ultimoAvisoMillis)
                val hoy = Calendario.diaDe(System.currentTimeMillis())
                Text(
                    text = if (dia == hoy) {
                        "Hoy %02d:%02d".format(hora.hour, hora.minute)
                    } else {
                        dia.toString()
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
