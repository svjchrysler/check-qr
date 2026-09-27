package com.seef.checkqr.core.designsystem.componentes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Animaciones del sistema.
 *
 * Criterio: una animacion entra solo si **comunica algo que el cambio
 * instantaneo no comunica**. En una app que un comerciante mira cien veces al
 * dia mientras atiende, el movimiento decorativo cansa y estorba.
 *
 * Las tres que sobreviven a ese criterio:
 *  - el total sube contando, porque asi se ve que **entro** dinero y cuanto;
 *  - un pago nuevo entra deslizandose, porque el ojo lo capta aunque estuvieras
 *    mirando a otro lado;
 *  - el indicador de escucha late despacio, porque responde a la pregunta que el
 *    comerciante se hace sin decirla: "¿esto sigue funcionando?".
 */
object Duraciones {
    /** Transiciones de estado corrientes: color de un boton, aparecer/desaparecer. */
    const val CORTA = 180

    /** Entrada de un elemento nuevo en una lista. */
    const val MEDIA = 260

    /** La cuenta del total. Lo bastante lenta para verla, sin hacer esperar. */
    const val CONTEO = 650

    /** Un ciclo del latido del indicador de escucha. */
    const val LATIDO = 2200
}

/**
 * Un importe que cuenta hasta su nuevo valor.
 *
 * Interpola entre dos enteros largos y no en coma flotante: un importe es
 * dinero, y el valor que queda en pantalla tiene que ser **exactamente** el
 * pedido. Con el progreso en 1f la cuenta devuelve el destino sin redondeos.
 *
 * El punto de partida es lo que se esta viendo en ese momento, no el ultimo
 * destino: si entra un segundo pago mientras el primero todavia cuenta, la
 * cifra sigue desde donde iba en lugar de saltar.
 *
 * La primera composicion no anima. Al abrir la app el total ya estaba ahi, y
 * verlo contar desde cero seria contar algo que no acaba de pasar.
 */
@Composable
fun montoAnimado(objetivoCentavos: Long): Long {
    var desde by remember { mutableLongStateOf(objetivoCentavos) }
    var hasta by remember { mutableLongStateOf(objetivoCentavos) }
    val progreso = remember { Animatable(1f) }

    LaunchedEffect(objetivoCentavos) {
        if (hasta == objetivoCentavos) return@LaunchedEffect
        desde += ((hasta - desde) * progreso.value).toLong()
        hasta = objetivoCentavos
        progreso.snapTo(0f)
        progreso.animateTo(
            targetValue = 1f,
            animationSpec = tween(Duraciones.CONTEO, easing = FastOutSlowInEasing),
        )
    }

    return desde + ((hasta - desde) * progreso.value).toLong()
}
