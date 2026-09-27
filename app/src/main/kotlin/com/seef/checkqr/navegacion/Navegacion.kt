package com.seef.checkqr.navegacion

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
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
import com.seef.checkqr.feature.caja.PantallaDeCaja
import com.seef.checkqr.feature.cuadre.PantallaDeCuadre
import com.seef.checkqr.feature.verificar.PantallaDeVerificar

/**
 * Las tres pantallas del uso diario.
 *
 * Equipo y Estado del sistema no estan en la barra inferior a proposito: son
 * cosas que se configuran una vez o se miran cuando algo va mal, y ocupar un
 * quinto del ancho con ellas le quitaria sitio a lo que se usa todo el dia.
 */
enum class Destino(val ruta: String, val etiqueta: String, val icono: ImageVector) {
    CAJA("caja", "Caja", Icons.Default.PointOfSale),
    VERIFICAR("verificar", "Verificar", Icons.Default.QrCodeScanner),
    CUADRE("cuadre", "Cuadre", Icons.Default.Receipt),
}

@Composable
fun NavegacionDeCheckQr(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val entrada by navController.currentBackStackEntryAsState()
    val destinoActual = entrada?.destination

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
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
                        icon = { Icon(destino.icono, contentDescription = null) },
                        label = { Text(destino.etiqueta) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destino.CAJA.ruta,
            modifier = Modifier.padding(padding),
        ) {
            composable(
                route = Destino.CAJA.ruta,
                deepLinks = listOf(navDeepLink { uriPattern = "checkqr://checkqr.app/caja" }),
            ) { PantallaDeCaja() }

            composable(
                route = Destino.VERIFICAR.ruta,
                // Enlace profundo desde el boton "Verificar" del widget.
                deepLinks = listOf(navDeepLink { uriPattern = "checkqr://checkqr.app/verificar" }),
            ) { PantallaDeVerificar() }

            composable(Destino.CUADRE.ruta) { PantallaDeCuadre() }
        }
    }
}
