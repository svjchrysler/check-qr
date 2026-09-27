package com.seef.checkqr.core.common

import com.seef.checkqr.core.model.AmountFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DineroTest {

    // --- Enteros -------------------------------------------------------------

    @Test
    fun `lee un entero sin separadores`() {
        assertEquals(5_000L, Dinero.aCentavos("50"))
        assertEquals(100L, Dinero.aCentavos("1"))
        assertEquals(0L, Dinero.aCentavos("0"))
    }

    @Test
    fun `quita el prefijo Bs en sus variantes`() {
        assertEquals(5_000L, Dinero.aCentavos("Bs 50"))
        assertEquals(5_000L, Dinero.aCentavos("Bs. 50"))
        assertEquals(5_000L, Dinero.aCentavos("bs50"))
        assertEquals(5_000L, Dinero.aCentavos("BS 50"))
    }

    // --- Los dos formatos de Bolivia ----------------------------------------

    @Test
    fun `formato con coma decimal`() {
        assertEquals(123_456L, Dinero.aCentavos("1.234,56"))
        assertEquals(5_050L, Dinero.aCentavos("50,50"))
        assertEquals(5_000L, Dinero.aCentavos("50,00"))
    }

    @Test
    fun `formato con punto decimal`() {
        assertEquals(123_456L, Dinero.aCentavos("1,234.56"))
        assertEquals(5_050L, Dinero.aCentavos("50.50"))
        assertEquals(123_456L, Dinero.aCentavos("Bs 1,234.56"))
    }

    @Test
    fun `con los dos separadores manda el ultimo`() {
        assertEquals(123_456L, Dinero.aCentavos("1.234,56"))
        assertEquals(123_456L, Dinero.aCentavos("1,234.56"))
        assertEquals(123_456_789L, Dinero.aCentavos("1.234.567,89"))
        assertEquals(123_456_789L, Dinero.aCentavos("1,234,567.89"))
    }

    // --- El caso que mas importa: no equivocarse por mil --------------------

    @Test
    fun `tres digitos detras del separador son miles, no centavos`() {
        assertEquals(123_400L, Dinero.aCentavos("1.234"))
        assertEquals(123_400L, Dinero.aCentavos("1,234"))
        assertEquals(1_234_567_00L, Dinero.aCentavos("1.234.567"))
    }

    @Test
    fun `un decimal suelto se completa a dos`() {
        assertEquals(5_050L, Dinero.aCentavos("50,5"))
        assertEquals(150L, Dinero.aCentavos("1.5"))
    }

    @Test
    fun `no adivina cuando el texto contradice el formato de la plantilla`() {
        // La plantilla dice que el decimal es la coma, pero el texto trae punto.
        assertNull(Dinero.aCentavos("50.50", AmountFormat.COMA_DECIMAL))
        // Y al reves.
        assertNull(Dinero.aCentavos("50,50", AmountFormat.PUNTO_DECIMAL))
    }

    @Test
    fun `respeta el formato declarado cuando coincide`() {
        assertEquals(5_050L, Dinero.aCentavos("50,50", AmountFormat.COMA_DECIMAL))
        assertEquals(5_050L, Dinero.aCentavos("50.50", AmountFormat.PUNTO_DECIMAL))
        // Miles: tres digitos detras siguen siendo miles en los dos formatos.
        assertEquals(123_400L, Dinero.aCentavos("1.234", AmountFormat.COMA_DECIMAL))
        assertEquals(123_400L, Dinero.aCentavos("1,234", AmountFormat.PUNTO_DECIMAL))
    }

    // --- Basura --------------------------------------------------------------

    @Test
    fun `rechaza lo que no es un importe`() {
        assertNull(Dinero.aCentavos(""))
        assertNull(Dinero.aCentavos("   "))
        assertNull(Dinero.aCentavos("Bs"))
        assertNull(Dinero.aCentavos("abc"))
        assertNull(Dinero.aCentavos("50 bolivianos"))
        assertNull(Dinero.aCentavos("1.2345"))
        assertNull(Dinero.aCentavos("-50"))
    }

    // --- Busqueda en texto libre --------------------------------------------

    @Test
    fun `encuentra el importe dentro de un aviso`() {
        assertEquals(
            5_000L,
            Dinero.primerImporte("Recibiste un pago de Bs 50,00 de Juan Perez"),
        )
        assertEquals(
            123_456L,
            Dinero.primerImporte("Pago recibido Bs. 1.234,56 ref 998877"),
        )
    }

    @Test
    fun `lista todos los importes del texto`() {
        assertEquals(
            listOf(5_000L, 12_050L),
            Dinero.importes("Pago de Bs 50,00. Saldo Bs 120,50"),
        )
    }

    @Test
    fun `solo los importes marcados con Bs, para texto poco fiable como el OCR`() {
        // El caso que motivo esta funcion: el ano de la fecha es un numero
        // valido y mayor que el cobro. Sin filtrar por el simbolo, "2026" gana
        // sobre "Bs 50,00" y el comprobante se compara contra Bs 20,26.
        val captura = "Yape\nPago exitoso\nBs 50.00\nPara Tienda Dona Rosa\n10 mar 2026"

        assertEquals(listOf(5_000L), Dinero.importesConSimbolo(captura))
        assertEquals(
            "sin el filtro el ano se cuela",
            202_600L,
            Dinero.importes(captura).max(),
        )
    }

    @Test
    fun `los importes marcados aceptan las dos formas de escribir Bs`() {
        assertEquals(
            listOf(5_000L, 12_050L),
            Dinero.importesConSimbolo("Comision Bs. 50,00 y total Bs 120,50"),
        )
    }

    @Test
    fun `sin ningun importe marcado la lista queda vacia`() {
        assertEquals(emptyList<Long>(), Dinero.importesConSimbolo("10 mar 2026, ref 998877"))
    }

    // --- Formato de salida ---------------------------------------------------

    @Test
    fun `formatea al estilo boliviano`() {
        assertEquals("Bs 50,00", Dinero.formatear(5_000L))
        assertEquals("Bs 1.234,56", Dinero.formatear(123_456L))
        assertEquals("Bs 0,10", Dinero.formatear(10L))
        assertEquals("Bs 1.234.567,89", Dinero.formatear(123_456_789L))
        assertEquals("1.234,56", Dinero.formatear(123_456L, conSimbolo = false))
    }

    @Test
    fun `ida y vuelta entre lectura y formato`() {
        listOf(0L, 1L, 10L, 100L, 5_000L, 123_456L, 123_456_789L).forEach { centavos ->
            val texto = Dinero.formatear(centavos, conSimbolo = false)
            assertEquals("ida y vuelta de $texto", centavos, Dinero.aCentavos(texto))
        }
    }
}
