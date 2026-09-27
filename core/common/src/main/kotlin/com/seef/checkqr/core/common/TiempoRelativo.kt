package com.seef.checkqr.core.common

/**
 * Cuanto hace que paso algo, en palabras.
 *
 * Existe para el modo mostrador. Ahi la hora absoluta no sirve: la pantalla
 * queda apoyada mostrando el ultimo cobro, y dos horas despues muestra
 * exactamente lo mismo. Quien la mira de reojo no puede distinguir un pago que
 * acaba de entrar de uno del mediodia, que es justo lo que esa pantalla existe
 * para responder.
 *
 * Kotlin puro y con el "ahora" recibido por parametro, para que se pruebe sin
 * reloj de verdad.
 */
object TiempoRelativo {

    /** Por debajo de esto no se dan numeros: un minuto largo sigue siendo "recien". */
    private const val UN_MINUTO = 60_000L
    private const val UNA_HORA = 60 * UN_MINUTO
    private const val UN_DIA = 24 * UNA_HORA

    fun desde(entoncesMillis: Long, ahoraMillis: Long): String {
        // Un reloj que retrocede (cambio de hora, ajuste por red) no deberia
        // producir "hace -3 min". Se trata como recien llegado.
        val transcurrido = (ahoraMillis - entoncesMillis).coerceAtLeast(0L)

        return when {
            transcurrido < UN_MINUTO -> "recién"
            transcurrido < UNA_HORA -> "hace ${transcurrido / UN_MINUTO} min"
            transcurrido < UN_DIA -> "hace ${transcurrido / UNA_HORA} h"
            else -> "hace ${Plural.contar((transcurrido / UN_DIA).toInt(), "día", "días")}"
        }
    }
}
