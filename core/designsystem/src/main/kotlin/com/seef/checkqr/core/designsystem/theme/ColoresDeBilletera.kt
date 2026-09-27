package com.seef.checkqr.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.seef.checkqr.core.model.Wallet

/**
 * Un tono y unas iniciales por billetera.
 *
 * Es la mayor ganancia de legibilidad de la lista de pagos: el comerciante
 * reconoce de que billetera vino un cobro por el color, antes de leer nada. En
 * un mostrador con el celular a medio metro, eso es la diferencia entre mirar y
 * tener que enfocar.
 *
 * Los tonos son **deliberadamente apagados**. Seis circulos saturados en una
 * lista compiten entre si y con el verde del total, que es el unico color que
 * tiene que llamar la atencion. Con tintes suaves siguen siendo distinguibles
 * sin gritar.
 *
 * **No son los colores de marca de los bancos.** Es una paleta propia, elegida
 * para que los seis tonos se distingan entre si — incluido para quien tiene
 * daltonismo, por eso ademas van siempre acompanados de las iniciales y del
 * nombre escrito. Usar los colores y logos reales de cada banco daria a entender
 * una relacion con ellos que no existe.
 */
@Immutable
data class IdentidadDeBilletera(
    val fondo: Color,
    val contenido: Color,
    val iniciales: String,
)

private val identidadesClaras = mapOf(
    Wallet.YAPE to IdentidadDeBilletera(Color(0xFFF0ECF9), Color(0xFF5B3FA0), "YP"),
    Wallet.BCP to IdentidadDeBilletera(Color(0xFFFAF0E3), Color(0xFF8A5A15), "BCP"),
    Wallet.BNB to IdentidadDeBilletera(Color(0xFFE9F0FA), Color(0xFF1F5490), "BNB"),
    Wallet.MERCANTIL to IdentidadDeBilletera(Color(0xFFFAEBEA), Color(0xFF9B322C), "MSC"),
    Wallet.UNION to IdentidadDeBilletera(Color(0xFFE7F2EE), Color(0xFF1E6B53), "BU"),
    Wallet.TIGO_MONEY to IdentidadDeBilletera(Color(0xFFE8F1F6), Color(0xFF1D5A77), "TM"),
    Wallet.DESCONOCIDA to IdentidadDeBilletera(Color(0xFFEFF1F0), Color(0xFF6B706D), "?"),
)

private val identidadesOscuras = mapOf(
    Wallet.YAPE to IdentidadDeBilletera(Color(0xFF241E33), Color(0xFFC3B0EC), "YP"),
    Wallet.BCP to IdentidadDeBilletera(Color(0xFF2B2213), Color(0xFFE5BC7E), "BCP"),
    Wallet.BNB to IdentidadDeBilletera(Color(0xFF15202E), Color(0xFF9CC2EE), "BNB"),
    Wallet.MERCANTIL to IdentidadDeBilletera(Color(0xFF2E1B1A), Color(0xFFEBA8A2), "MSC"),
    Wallet.UNION to IdentidadDeBilletera(Color(0xFF14241F), Color(0xFF8CCBB4), "BU"),
    Wallet.TIGO_MONEY to IdentidadDeBilletera(Color(0xFF152329), Color(0xFF93C3DB), "TM"),
    Wallet.DESCONOCIDA to IdentidadDeBilletera(Color(0xFF1D2120), Color(0xFF9AA09D), "?"),
)

/** La identidad visual de una billetera en el tema vigente. */
@Composable
fun Wallet.identidad(): IdentidadDeBilletera {
    val mapa = if (LocalTemaOscuro.current) identidadesOscuras else identidadesClaras
    return mapa[this] ?: mapa.getValue(Wallet.DESCONOCIDA)
}
