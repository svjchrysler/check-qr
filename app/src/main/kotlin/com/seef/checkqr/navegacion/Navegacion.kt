package com.seef.checkqr.navegacion

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.seef.checkqr.core.designsystem.Iconos
import com.seef.checkqr.feature.caja.PantallaDeCaja
import com.seef.checkqr.feature.caja.PantallaDeMostrador
import com.seef.checkqr.feature.caja.PantallaDeEstado
import com.seef.checkqr.feature.equipo.PantallaDeEquipo
import com.seef.checkqr.feature.cuadre.PantallaDeCuadre
import com.seef.checkqr.feature.verificar.PantallaDeVerificar

/**
 * Las pantallas de la app.
 *
 * Las tres primeras son el uso diario y van primero. Equipo y Estado se usan
 * poco (una se configura al principio, la otra se mira cuando algo va mal), pero
 * estan en la barra igual: esconder "Estado del sistema" en un menu seria
 * esconder justamente lo que el comerciante necesita encontrar rapido el dia que
 * deja de escuchar sus pagos.
 */
enum class Destino(
    val ruta: String,
    val etiqueta: String,
    /** Contorneado cuando la pestana no esta activa. */
    val icono: ImageVector,
    /** Relleno cuando si lo esta: es el patron con el que la gente lee "estoy aqui". */
    val iconoActivo: ImageVector,
) {
    CAJA("caja", "Caja", Iconos.caja, Iconos.cajaActiva),
    VERIFICAR("verificar", "Verificar", Iconos.verificar, Iconos.verificarActiva),
    CUADRE("cuadre", "Cuadre", Iconos.cuadre, Iconos.cuadreActiva),
    EQUIPO("equipo", "Equipo", Iconos.equipo, Iconos.equipoActiva),
    ESTADO("estado", "Estado", Iconos.estado, Iconos.estadoActiva),
}

/**
 * El modo mostrador es un destino propio y no una pestana.
 *
 * Asi la barra inferior desaparece mientras esta activo — el celular queda
 * apoyado y cualquier barra invita a toques accidentales — y el boton atras sale
 * del mostrador en vez de cerrar la app.
 */
const val RUTA_MOSTRADOR: String = "mostrador"

@Composable
fun NavegacionDeCheckQr(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val entrada by navController.currentBackStackEntryAsState()
    val destinoActual = entrada?.destination
    val enMostrador = destinoActual?.route == RUTA_MOSTRADOR

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (enMostrador) return@Scaffold
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Destino.entries.forEach { destino ->
                    val seleccionado = destinoActual?.hierarchy?.any { it.route == destino.ruta } == true
                    NavigationBarItem(
                        selected = seleccionado,
                        onClick = {
                            navController.navigate(destino.ruta) {
                                // Sin apilar pantallas al ir y venir por la barra:
                                // el boton atras tiene que salir de la app, no
                                // recorrer el historial de pestanas.
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (seleccionado) {
                                    destino.iconoActivo
                                } else {
                                    destino.icono
                                },
                                contentDescription = null,
                            )
                        },
                        label = { Text(destino.etiqueta) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        // Solo el relleno de abajo: cada pantalla tiene su propia barra
        // superior y aplica el inset del sistema por su cuenta. Pasarle aqui el
        // relleno completo lo aplicaria dos veces y dejaria un hueco enorme
        // bajo la barra de estado.
        NavHost(
            navController = navController,
            startDestination = Destino.CAJA.ruta,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable(
                route = Destino.CAJA.ruta,
                deepLinks = listOf(navDeepLink { uriPattern = "checkqr://checkqr.app/caja" }),
            ) {
                PantallaDeCaja(
                    onVerEstado = { navController.navigate(Destino.ESTADO.ruta) },
                    onAbrirMostrador = { navController.navigate(RUTA_MOSTRADOR) },
                )
            }

            composable(RUTA_MOSTRADOR) {
                PantallaDeMostrador(onSalir = { navController.popBackStack() })
            }

            composable(
                route = Destino.VERIFICAR.ruta,
                // Enlace profundo desde el boton "Verificar" del widget.
                deepLinks = listOf(navDeepLink { uriPattern = "checkqr://checkqr.app/verificar" }),
            ) { PantallaDeVerificar() }

            composable(Destino.CUADRE.ruta) { PantallaDeCuadre() }
            composable(Destino.EQUIPO.ruta) { PantallaDeEquipo() }
            composable(Destino.ESTADO.ruta) { PantallaDeEstado() }
        }
    }
}
