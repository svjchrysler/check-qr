package com.seef.checkqr.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/**
 * Tipografia del sistema.
 *
 * Se ajustan los estilos que la app usa para montos y etiquetas: los pesos por
 * omision de Material son demasiado ligeros para leer una cifra de reojo desde
 * el otro lado de un mostrador.
 */
val tipografia = Typography().let { base ->
    base.copy(
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        // Las etiquetas llevan algo de espaciado: se usan en chips y encabezados
        // en mayuscula, donde el texto apretado se lee peor.
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.copy(
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
        ),
    )
}

/**
 * Estilos para los importes.
 *
 * Los montos se escriben con `FontFamily.Default` pero en tabular: sin eso, las
 * cifras de una lista no quedan alineadas entre filas y la columna se ve
 * temblorosa.
 */
object Montos {
    /**
     * El total del dia.
     *
     * Grande de verdad: es lo unico de la pantalla que se tiene que poder leer
     * sin acercarse, y ya no esta dentro de una tarjeta de color que lo separe
     * del resto, asi que el tamano es lo que le da la jerarquia.
     */
    val destacado = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 46.sp,
        lineHeight = 52.sp,
        letterSpacing = (-1).sp,
    )

    /**
     * El monto de una fila de la lista.
     *
     * SemiBold y no Bold: en una lista de veinte filas, el negrita completo se
     * vuelve una mancha y deja de destacar nada.
     */
    val fila = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
    )

    /** Totales de los renglones del cuadre. */
    val renglon = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    )
}

/**
 * Escala del modo mostrador.
 *
 * Son tamanos deliberadamente enormes porque el caso de uso es real y concreto:
 * el celular apoyado en el mostrador y el comerciante mirandolo de pie, a un
 * metro, con las manos ocupadas. Los tamanos de Material no alcanzan para eso.
 *
 * Las medidas van en sp y no en dp para que respeten el tamano de fuente del
 * sistema: muchos comerciantes ya lo tienen subido.
 */
object TipografiaMostrador {

    /** El monto del ultimo pago. Es lo unico que se lee de lejos. */
    val monto = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 80.sp,
        lineHeight = 86.sp,
        textAlign = TextAlign.Center,
    )

    /** Version para pantalla chica, cuando 80sp no entra. */
    val montoCompacto = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 52.sp,
        lineHeight = 58.sp,
        textAlign = TextAlign.Center,
    )

    /** Nombre del pagador. */
    val detalle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        textAlign = TextAlign.Center,
    )

    /** Total del dia. */
    val total = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
    )

    /** Etiquetas del mostrador. */
    val etiqueta = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    )
}
