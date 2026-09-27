package com.seef.checkqr.core.designsystem.theme

import androidx.compose.ui.unit.dp

/**
 * Escala de espaciado.
 *
 * Una escala fija y no numeros sueltos por pantalla: la sensacion de que una app
 * esta "bien hecha" viene mas de que los margenes sean consistentes que de
 * cualquier otra cosa. Todo es multiplo de 4.
 */
object Espaciado {
    /** Entre elementos muy relacionados: un monto y su etiqueta. */
    val minimo = 4.dp

    /** Dentro de un grupo: las lineas de una fila de pago. */
    val corto = 8.dp

    /** Separacion interna de las tarjetas. */
    val medio = 12.dp

    /** Margen lateral de la pantalla y relleno de las tarjetas. */
    val estandar = 16.dp

    /** Entre bloques distintos de una misma pantalla. */
    val amplio = 24.dp

    /** Separacion de secciones mayores. */
    val seccion = 32.dp
}

object Medidas {
    /**
     * Alto minimo de una fila tocable.
     *
     * 48dp es el minimo de accesibilidad, pero las filas de pago usan 72: el
     * comerciante las lee de pie, con el celular en el mostrador, y a veces con
     * una mano ocupada.
     */
    val objetivoTactil = 48.dp
    val filaDePago = 72.dp

    /** El avatar de billetera de la lista. */
    val avatar = 44.dp

    /** El boton principal de la caja. Grande porque es la accion de la app. */
    val botonPrincipal = 60.dp

    /** Grosor del borde de las tarjetas con contorno. */
    val borde = 1.dp
}
