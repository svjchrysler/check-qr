package com.seef.checkqr.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.seef.checkqr.core.model.Wallet

/**
 * Un color y unas iniciales por billetera.
 *
 * Es la mayor ganancia de legibilidad de la lista de pagos: el comerciante
 * reconoce de que billetera vino un cobro por el color, antes de leer nada. En
 * un mostrador con el celular a medio metro, eso es la diferencia entre mirar y
 * tener que enfocar.
 *
 * **No son los colores de marca de los bancos.** Son una paleta propia,
 * elegida para que los seis tonos se distingan entre si — incluido para quien
 * tiene daltonismo, por eso ademas van siempre acompanados de las iniciales y
 * del nombre escrito. Usar los colores y logos reales de cada banco daria a
 * entender una relacion con ellos que no existe.
 */
@Immutable
data class IdentidadDeBilletera(
    val fondo: Color,
    val contenido: Color,
    val iniciales: String,
)

private val identidadesClaras = mapOf(
    Wallet.YAPE to IdentidadDeBilletera(Color(0xFFEADDFF), Color(0xFF4A2B78), "YP"),
    Wallet.BCP to IdentidadDeBilletera(Color(0xFFFFE0B2), Color(0xFF7A4100), "BCP"),
    Wallet.BNB to IdentidadDeBilletera(Color(0xFFD3E3FD), Color(0xFF0B4A87), "BNB"),
    Wallet.MERCANTIL to IdentidadDeBilletera(Color(0xFFFFDAD6), Color(0xFF8C1D18), "MSC"),
    Wallet.UNION to IdentidadDeBilletera(Color(0xFFCDE9E2), Color(0xFF005044), "BU"),
    Wallet.TIGO_MONEY to IdentidadDeBilletera(Color(0xFFD5E8F7), Color(0xFF0D4E6B), "TM"),
    Wallet.DESCONOCIDA to IdentidadDeBilletera(Color(0xFFE3E3E3), Color(0xFF444746), "?"),
)

private val identidadesOscuras = mapOf(
    Wallet.YAPE to IdentidadDeBilletera(Color(0xFF4A2B78), Color(0xFFEADDFF), "YP"),
    Wallet.BCP to IdentidadDeBilletera(Color(0xFF6B3A00), Color(0xFFFFE0B2), "BCP"),
    Wallet.BNB to IdentidadDeBilletera(Color(0xFF0B3C6B), Color(0xFFD3E3FD), "BNB"),
    Wallet.MERCANTIL to IdentidadDeBilletera(Color(0xFF701D18), Color(0xFFFFDAD6), "MSC"),
    Wallet.UNION to IdentidadDeBilletera(Color(0xFF004138), Color(0xFFCDE9E2), "BU"),
    Wallet.TIGO_MONEY to IdentidadDeBilletera(Color(0xFF0B3D54), Color(0xFFD5E8F7), "TM"),
    Wallet.DESCONOCIDA to IdentidadDeBilletera(Color(0xFF3A3A3A), Color(0xFFCCCCCC), "?"),
)

/** La identidad visual de una billetera en el tema vigente. */
@Composable
fun Wallet.identidad(): IdentidadDeBilletera {
    val mapa = if (LocalTemaOscuro.current) identidadesOscuras else identidadesClaras
    return mapa[this] ?: mapa.getValue(Wallet.DESCONOCIDA)
}
