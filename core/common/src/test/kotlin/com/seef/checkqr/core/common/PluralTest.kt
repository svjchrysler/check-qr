package com.seef.checkqr.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class PluralTest {

    @Test
    fun `uno va en singular`() {
        assertEquals("1 pago", Plural.pagos(1))
        assertEquals("1 billetera", Plural.contar(1, "billetera", "billeteras"))
    }

    @Test
    fun `cero y varios van en plural`() {
        // El cero en espanol lleva plural: "cero pagos", no "cero pago".
        assertEquals("0 pagos", Plural.pagos(0))
        assertEquals("2 pagos", Plural.pagos(2))
        assertEquals("21 pagos", Plural.pagos(21))
    }

    @Test
    fun `devuelve solo la palabra cuando el numero va en otro sitio`() {
        assertEquals("billetera", Plural.palabra(1, "billetera", "billeteras"))
        assertEquals("billeteras", Plural.palabra(3, "billetera", "billeteras"))
    }
}
