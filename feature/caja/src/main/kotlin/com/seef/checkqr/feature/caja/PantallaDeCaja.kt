package com.seef.checkqr.feature.caja

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seef.checkqr.core.common.Calendario
import com.seef.checkqr.core.designsystem.Iconos
import com.seef.checkqr.core.designsystem.componentes.AccionesDeCaja
import com.seef.checkqr.core.designsystem.componentes.AvisoPendiente
import com.seef.checkqr.core.designsystem.componentes.BotonDeCaja
import com.seef.checkqr.core.designsystem.componentes.CabeceraDeMarca
import com.seef.checkqr.core.designsystem.componentes.Duraciones
import com.seef.checkqr.core.designsystem.componentes.EncabezadoDeSeccion
import com.seef.checkqr.core.designsystem.componentes.EstadoVacio
import com.seef.checkqr.core.designsystem.componentes.FilaDePago
import com.seef.checkqr.core.designsystem.componentes.NivelDeAviso
import com.seef.checkqr.core.designsystem.componentes.SeparadorDeLista
import com.seef.checkqr.core.designsystem.componentes.TarjetaDeLista
import com.seef.checkqr.core.designsystem.componentes.TiraDeAvisos
import com.seef.checkqr.core.designsystem.componentes.formaDeFila
import com.seef.checkqr.core.designsystem.componentes.posicionEnTarjeta
import com.seef.checkqr.core.designsystem.theme.Espaciado
import com.seef.checkqr.core.model.Payment
import com.seef.checkqr.voice.LectorDeVoz

@Composable
fun PantallaDeCaja(
    modifier: Modifier = Modifier,
    onVerEstado: () -> Unit = {},
    onAbrirMostrador: () -> Unit = {},
    vm: CajaViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (ui.cargando) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Contenido(
        ui = ui,
        onAbrir = vm::abrirCaja,
        onCerrar = vm::cerrarCaja,
        onMostrador = onAbrirMostrador,
        onDiscreto = vm::alternarDiscreto,
        onDescartarOculto = vm::descartarAvisoOculto,
        onInstalarVoz = { context.startActivity(LectorDeVoz.intentDeInstalarVoz()) },
        onVerEstado = onVerEstado,
        modifier = modifier,
    )
}

@Composable
private fun Contenido(
    ui: UiCaja,
    onAbrir: () -> Unit,
    onCerrar: () -> Unit,
    onMostrador: () -> Unit,
    onDiscreto: () -> Unit,
    onDescartarOculto: () -> Unit,
    onInstalarVoz: () -> Unit,
    onVerEstado: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val caja = ui.caja
    val avisos = construirAvisos(ui, onAbrir, onDescartarOculto, onInstalarVoz, onVerEstado)

    // Los pagos que ya estaban al abrir la pantalla. Todo lo que llegue despues
    // entra animado; lo que ya estaba, no. Sin esto, al abrir la app se animarian
    // de golpe las veinte filas del dia, que no comunica nada.
    val yaEstaban = remember { ui.pagosDeHoy.map(Payment::id).toSet() }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            CabeceraDeMarca(
                titulo = "Caja",
                subtitulo = fechaDeHoy(),
                totalCentavos = ui.totalDeHoyCentavos,
                cantidadDePagos = ui.pagosDeHoy.size,
                cajaAbierta = caja?.cajaAbierta == true,
                discreto = ui.modoDiscreto,
                escuchando = ui.estadoDelSistema?.listenerConectado != false,
                onTocarEstado = onVerEstado,
            )
        }

        item {
            Spacer(Modifier.height(Espaciado.estandar))

            // Las acciones viven en su propia tarjeta blanca. Sin ella, el verde
            // del boton quedaba pegado al verde de la cabecera y los dos bloques
            // se leian como uno solo; el marco blanco los separa.
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Espaciado.estandar),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = 1.dp,
            ) {
                Column(modifier = Modifier.padding(Espaciado.estandar)) {
                    BotonDeCaja(
                        abierta = caja?.cajaAbierta == true,
                        onClick = if (caja?.cajaAbierta == true) onCerrar else onAbrir,
                    )

                    Spacer(Modifier.height(Espaciado.medio))

                    AccionesDeCaja(
                        discreto = ui.modoDiscreto,
                        onMostrador = onMostrador,
                        onAlternarDiscreto = onDiscreto,
                    )
                }
            }

            if (avisos.isNotEmpty()) {
                Spacer(Modifier.height(Espaciado.medio))
                Column(modifier = Modifier.padding(horizontal = Espaciado.estandar)) {
                    TiraDeAvisos(avisos)
                }
            }
        }

        item {
            EncabezadoDeSeccion(
                texto = "Pagos de hoy",
                acompanante = if (ui.pagosDeHoy.isEmpty()) null else "${ui.pagosDeHoy.size}",
            )
        }

        if (ui.pagosDeHoy.isEmpty()) {
            item {
                TarjetaDeLista {
                    EstadoVacio(
                        icono = Iconos.bandejaVacia,
                        titulo = "Todavía no llegó ningún pago",
                        detalle = if (caja?.cajaAbierta == true) {
                            "La caja está abierta. En cuanto entre un cobro lo vas a escuchar."
                        } else {
                            "Abre la caja para que CheckQr anuncie los pagos en voz alta."
                        },
                    )
                }
            }
        } else {
            itemsIndexed(ui.pagosDeHoy, key = { _, p -> p.id }) { indice, pago ->
                // Cada fila lleva su propia superficie con las esquinas que le
                // tocan: una LazyColumn no puede envolver todos sus items en una
                // sola Surface sin perder el reciclado, asi que la tarjeta se
                // consigue componiendo las formas fila a fila.
                val forma = formaDeFila(posicionEnTarjeta(indice, ui.pagosDeHoy.size))

                AnimatedVisibility(
                    visible = true,
                    enter = if (pago.id in yaEstaban) {
                        EnterTransition.None
                    } else {
                        expandVertically(tween(Duraciones.MEDIA)) +
                            fadeIn(tween(Duraciones.MEDIA))
                    },
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Espaciado.estandar)
                            .clip(forma),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        shadowElevation = 1.dp,
                    ) {
                        Column {
                            FilaDePago(
                                montoCentavos = pago.amountCents,
                                pagador = pago.payerName,
                                billetera = pago.wallet,
                                hora = horaDe(pago),
                                discreto = ui.modoDiscreto,
                                // La confianza PARCIAL no se marca aqui a
                                // proposito: casi ningun banco boliviano manda
                                // referencia, asi que es el caso normal, y poner
                                // un aviso en la mayoria de las filas ensena a
                                // ignorarlo. Cuando falta el pagador, la propia
                                // fila ya lo dice.
                                reclamado = pago.estaReclamado,
                            )
                            if (indice < ui.pagosDeHoy.lastIndex) SeparadorDeLista()
                        }
                    }
                }
            }
        }

        // Aire al final para que la ultima fila se pueda desplazar por encima
        // de la barra de navegacion en vez de quedar pegada a ella.
        item { Spacer(Modifier.height(Espaciado.seccion)) }
    }
}

/**
 * Arma la lista de avisos.
 *
 * Solo los de nivel PROBLEMA se muestran enteros; el resto se pliega en una
 * linea. Antes, tres tarjetas apiladas empujaban la lista de pagos fuera de la
 * pantalla, y el comerciante abre la app para ver sus cobros, no advertencias.
 */
@Composable
private fun construirAvisos(
    ui: UiCaja,
    onAbrir: () -> Unit,
    onDescartarOculto: () -> Unit,
    onInstalarVoz: () -> Unit,
    onVerEstado: () -> Unit,
): List<AvisoPendiente> = buildList {
    val caja = ui.caja
    val sistema = ui.estadoDelSistema

    if (caja?.hayQueReactivarVoz == true) {
        add(
            AvisoPendiente(
                titulo = "Toca para reactivar la voz",
                detalle = "El celular se reinició. La app siguió anotando los pagos, " +
                    "pero para que vuelva a anunciarlos hay que abrir la caja de nuevo.",
                nivel = NivelDeAviso.PROBLEMA,
                textoDeAccion = "Abrir caja",
                onAccion = onAbrir,
            ),
        )
    }

    if (sistema != null && !sistema.listenerConectado) {
        add(
            AvisoPendiente(
                titulo = "El celular dejó de escuchar los pagos",
                detalle = "Hay que darle permiso a CheckQr para ver los avisos de las " +
                    "apps del banco desde los ajustes del sistema.",
                nivel = NivelDeAviso.PROBLEMA,
                textoDeAccion = "Ver estado",
                onAccion = onVerEstado,
            ),
        )
    }

    if (caja?.faltaInstalarVoz == true) {
        add(
            AvisoPendiente(
                titulo = "Falta la voz en español",
                detalle = "El celular no tiene instalada la voz en español, así que la " +
                    "app no puede anunciar los pagos.",
                nivel = NivelDeAviso.PROBLEMA,
                textoDeAccion = "Instalar",
                onAccion = onInstalarVoz,
            ),
        )
    }

    if (ui.avisoConContenidoOculto != null) {
        add(
            AvisoPendiente(
                titulo = "Revisa tu app del banco",
                detalle = "Llegó un aviso del banco pero el sistema ocultó su contenido, " +
                    "así que no se pudo leer el monto.",
                nivel = NivelDeAviso.ATENCION,
                textoDeAccion = "Entendido",
                onAccion = onDescartarOculto,
            ),
        )
    }

    if (sistema?.plantillasSinVerificar == true) {
        add(
            AvisoPendiente(
                titulo = "Plantillas de banco sin verificar",
                detalle = "Haz un pago de prueba de Bs 1 con cada billetera para " +
                    "comprobar que CheckQr entiende sus avisos.",
                nivel = NivelDeAviso.ATENCION,
                textoDeAccion = "Ver estado",
                onAccion = onVerEstado,
            ),
        )
    }
}

private fun horaDe(pago: Payment): String {
    val hora = Calendario.horaDe(pago.notifPostedAtMillis)
    return "%02d:%02d".format(hora.hour, hora.minute)
}

private fun fechaDeHoy(): String {
    val dia = Calendario.diaDe(System.currentTimeMillis())
    val meses = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
    )
    return "${dia.day} de ${meses[dia.month.ordinal]}"
}
