package com.seef.checkqr.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

internal val tipografia = Typography()

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
        fontSize = 72.sp,
        lineHeight = 78.sp,
        textAlign = TextAlign.Center,
    )

    /** Version para pantalla chica, cuando 72sp no entra. */
    val montoCompacto = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 54.sp,
        textAlign = TextAlign.Center,
    )

    /** Nombre del pagador y billetera. */
    val detalle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        textAlign = TextAlign.Center,
    )

    /** Total del dia. */
    val total = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
    )

    /** Etiquetas del mostrador. */
    val etiqueta = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    )
}
