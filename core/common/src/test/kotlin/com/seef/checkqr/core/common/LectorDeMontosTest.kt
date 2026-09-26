package com.seef.checkqr.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class LectorDeMontosTest {

    // --- El ejemplo del requisito --------------------------------------------

    @Test
    fun `el anuncio del requisito`() {
        assertEquals(
            "Recibiste cincuenta bolivianos de Juan Pérez por Yape",
            LectorDeMontos.anuncioDePago(5_000L, "Juan Pérez", "Yape"),
        )
    }

    @Test
    fun `sin pagador se omite la parte del nombre`() {
        assertEquals(
            "Recibiste cincuenta bolivianos por Tigo Money",
            LectorDeMontos.anuncioDePago(5_000L, null, "Tigo Money"),
        )
        assertEquals(
            "Recibiste cien bolivianos por BNB",
            LectorDeMontos.anuncioDePago(10_000L, "   ", "BNB"),
        )
    }

    // --- Concordancia del espanol -------------------------------------------

    @Test
    fun `singular y plural de boliviano`() {
        assertEquals("un boliviano", LectorDeMontos.enPalabras(100L))
        assertEquals("dos bolivianos", LectorDeMontos.enPalabras(200L))
        assertEquals("cero bolivianos", LectorDeMontos.enPalabras(0L))
    }

    @Test
    fun `apocope de uno delante del sustantivo`() {
        assertEquals("veintiún bolivianos", LectorDeMontos.enPalabras(2_100L))
        assertEquals("treinta y un bolivianos", LectorDeMontos.enPalabras(3_100L))
        assertEquals("ciento un bolivianos", LectorDeMontos.enPalabras(10_100L))
    }

    @Test
    fun `centavos`() {
        assertEquals("un boliviano con cincuenta centavos", LectorDeMontos.enPalabras(150L))
        assertEquals("cero bolivianos con diez centavos", LectorDeMontos.enPalabras(10L))
        assertEquals("cero bolivianos con un centavo", LectorDeMontos.enPalabras(1L))
        assertEquals(
            "cincuenta bolivianos con noventa y nueve centavos",
            LectorDeMontos.enPalabras(5_099L),
        )
        assertEquals(
            "dos bolivianos con veintiún centavos",
            LectorDeMontos.enPalabras(221L),
        )
    }

    // --- Numeros --------------------------------------------------------------

    @Test
    fun `del cero al veintinueve son formas propias`() {
        assertEquals("cero", LectorDeMontos.numero(0))
        assertEquals("quince", LectorDeMontos.numero(15))
        assertEquals("dieciséis", LectorDeMontos.numero(16))
        assertEquals("veinte", LectorDeMontos.numero(20))
        assertEquals("veintiuno", LectorDeMontos.numero(21))
        assertEquals("veintidós", LectorDeMontos.numero(22))
        assertEquals("veintinueve", LectorDeMontos.numero(29))
    }

    @Test
    fun `decenas con y`() {
        assertEquals("treinta", LectorDeMontos.numero(30))
        assertEquals("treinta y uno", LectorDeMontos.numero(31))
        assertEquals("cuarenta y cinco", LectorDeMontos.numero(45))
        assertEquals("noventa y nueve", LectorDeMontos.numero(99))
    }

    @Test
    fun `cien contra ciento`() {
        assertEquals("cien", LectorDeMontos.numero(100))
        assertEquals("ciento uno", LectorDeMontos.numero(101))
        assertEquals("ciento cincuenta", LectorDeMontos.numero(150))
        assertEquals("quinientos", LectorDeMontos.numero(500))
        assertEquals("novecientos noventa y nueve", LectorDeMontos.numero(999))
    }

    @Test
    fun `mil sin uno delante`() {
        assertEquals("mil", LectorDeMontos.numero(1_000))
        assertEquals("mil uno", LectorDeMontos.numero(1_001))
        assertEquals("dos mil", LectorDeMontos.numero(2_000))
        assertEquals("veintiún mil", LectorDeMontos.numero(21_000))
        assertEquals("treinta y un mil quinientos", LectorDeMontos.numero(31_500))
        assertEquals(
            "novecientos noventa y nueve mil novecientos noventa y nueve",
            LectorDeMontos.numero(999_999),
        )
    }

    @Test
    fun `millones`() {
        assertEquals("un millón", LectorDeMontos.numero(1_000_000))
        assertEquals("dos millones", LectorDeMontos.numero(2_000_000))
        assertEquals("un millón quinientos mil", LectorDeMontos.numero(1_500_000))
    }

    @Test
    fun `fuera de rango cae a digitos en lugar de fallar`() {
        assertEquals("1000000000", LectorDeMontos.numero(1_000_000_000L))
    }

    // --- Importes tipicos de un comercio ------------------------------------

    @Test
    fun `importes que se ven en el mostrador`() {
        assertEquals("cinco bolivianos", LectorDeMontos.enPalabras(500L))
        assertEquals("diez bolivianos", LectorDeMontos.enPalabras(1_000L))
        assertEquals("veinte bolivianos", LectorDeMontos.enPalabras(2_000L))
        assertEquals("cien bolivianos", LectorDeMontos.enPalabras(10_000L))
        assertEquals(
            "mil doscientos treinta y cuatro bolivianos con cincuenta y seis centavos",
            LectorDeMontos.enPalabras(123_456L),
        )
    }

    @Test
    fun `todo importe entre cero y diez mil se lee sin excepcion`() {
        for (centavos in 0L..1_000_000L step 37L) {
            val texto = LectorDeMontos.enPalabras(centavos)
            assert(texto.isNotBlank()) { "$centavos quedo sin texto" }
            assert(!texto.contains("null")) { "$centavos produjo '$texto'" }
        }
    }
}
