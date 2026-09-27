package com.seef.checkqr.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class TiempoRelativoTest {

    private val ahora = 1_700_000_000_000L

    private fun haceMillis(millis: Long) = TiempoRelativo.desde(ahora - millis, ahora)

    @Test
    fun `menos de un minuto es recien`() {
        assertEquals("recién", haceMillis(0))
        assertEquals("recién", haceMillis(59_000))
    }

    @Test
    fun `de un minuto a una hora, en minutos`() {
        assertEquals("hace 1 min", haceMillis(60_000))
        assertEquals("hace 59 min", haceMillis(59 * 60_000L))
    }

    @Test
    fun `de una hora a un dia, en horas`() {
        assertEquals("hace 1 h", haceMillis(60 * 60_000L))
        assertEquals("hace 23 h", haceMillis(23 * 60 * 60_000L))
    }

    @Test
    fun `a partir de un dia, en dias y con plural`() {
        assertEquals("hace 1 día", haceMillis(24 * 60 * 60_000L))
        assertEquals("hace 3 días", haceMillis(3 * 24 * 60 * 60_000L))
    }

    @Test
    fun `un reloj que retrocede no produce tiempos negativos`() {
        // Pasa de verdad: el celular ajusta la hora por red y el postTime del
        // aviso queda por delante del reloj.
        assertEquals("recién", TiempoRelativo.desde(ahora + 5_000, ahora))
    }
}
