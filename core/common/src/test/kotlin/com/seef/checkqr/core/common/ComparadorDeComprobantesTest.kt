package com.seef.checkqr.core.common

import com.seef.checkqr.core.model.MatchResult
import com.seef.checkqr.core.model.ParseConfidence
import com.seef.checkqr.core.model.Payment
import com.seef.checkqr.core.model.PaymentLevel
import com.seef.checkqr.core.model.SyncState
import com.seef.checkqr.core.model.Wallet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComparadorDeComprobantesTest {

    private val ahora = 1_700_000_000_000L

    private fun pago(
        id: String,
        centavos: Long,
        referencia: String? = null,
        haceMillis: Long = 0L,
        pagador: String? = "Juan Perez",
    ) = Payment(
        id = id,
        dedupKey = "clave-$id",
        wallet = Wallet.YAPE,
        sourcePackage = "com.banco.yape",
        amountCents = centavos,
        payerName = pagador,
        reference = referencia,
        notifPostedAtMillis = ahora - haceMillis,
        capturedAtMillis = ahora - haceMillis,
        level = PaymentLevel.AVISO_BANCO,
        confidence = ParseConfidence.PARCIAL,
        syncState = SyncState.PENDIENTE,
        shiftId = null,
        cashierId = null,
        claimedByReceiptId = null,
        rawText = null,
    )

    private fun comparar(texto: String, vararg candidatos: Payment) =
        ComparadorDeComprobantes.comparar(
            ComparadorDeComprobantes.leer(texto),
            candidatos.toList(),
            ahora,
        )

    // --- Lectura del OCR ------------------------------------------------------

    @Test
    fun `lee el monto de una captura tipica`() {
        val l = ComparadorDeComprobantes.leer(
            "Yape\nPago exitoso\nBs 50.00\nPara Tienda Dona Rosa\n10 mar 2026",
        )
        assertEquals(5_000L, l.amountCents)
    }

    @Test
    fun `toma el importe mayor, no el primero`() {
        // Las capturas traen comisiones y saldos: el cobro es casi siempre el
        // numero mas grande de la pantalla.
        val l = ComparadorDeComprobantes.leer(
            "Comision Bs 0,50\nMonto Bs 120,00\nSaldo restante Bs 45,30",
        )
        assertEquals(12_000L, l.amountCents)
    }

    @Test
    fun `lee la referencia solo cuando viene etiquetada`() {
        assertEquals(
            "AB12CD34",
            ComparadorDeComprobantes.leer("Bs 50,00 Ref: AB12CD34").reference,
        )
        assertEquals(
            "998877",
            ComparadorDeComprobantes.leer("Número de operación: 998877").reference,
        )
        // Sin etiqueta no se inventa una referencia: agarraria el telefono o la
        // fecha y produciria coincidencias falsas.
        assertNull(ComparadorDeComprobantes.leer("Bs 50,00 al 71234567").reference)
    }

    // --- Coincidencia por referencia -----------------------------------------

    @Test
    fun `la referencia manda sobre el monto`() {
        val v = comparar(
            "Bs 50,00 Ref: UNICA99",
            pago("p1", 5_000L, referencia = "UNICA99"),
            pago("p2", 5_000L, referencia = "OTRA11"),
        )
        assertEquals(MatchResult.COINCIDE, v.resultado)
        assertEquals("p1", v.pago?.id)
    }

    // --- Coincidencia por monto -----------------------------------------------

    @Test
    fun `un solo pago del monto coincide`() {
        val v = comparar("Bs 50,00", pago("p1", 5_000L), pago("p2", 12_000L))
        assertEquals(MatchResult.COINCIDE, v.resultado)
        assertEquals("p1", v.pago?.id)
    }

    @Test
    fun `varios pagos del mismo monto quedan ambiguos y no se elige por el usuario`() {
        // Es el sesgo deliberado: un falso "si, llego" le cuesta la mercaderia al
        // comerciante, asi que ante duda decide una persona.
        val v = comparar(
            "Bs 50,00",
            pago("p1", 5_000L, pagador = "Juan"),
            pago("p2", 5_000L, pagador = "Ana"),
        )
        assertEquals(MatchResult.AMBIGUO, v.resultado)
        assertNull("no debe elegir uno", v.pago)
        assertEquals(2, v.candidatos.size)
    }

    // --- El caso rojo -----------------------------------------------------------

    @Test
    fun `sin ningun pago del monto el veredicto es que no llego`() {
        val v = comparar("Bs 99,00", pago("p1", 5_000L))
        assertEquals(MatchResult.NO_LLEGO, v.resultado)
        assertTrue(v.explicacion.contains("Bs 99,00"))
    }

    @Test
    fun `sin candidatos tampoco llego`() {
        assertEquals(MatchResult.NO_LLEGO, comparar("Bs 50,00").resultado)
    }

    // --- Ventana de tiempo -------------------------------------------------------

    @Test
    fun `un pago viejo no cuenta`() {
        val v = comparar(
            "Bs 50,00",
            pago("viejo", 5_000L, haceMillis = ComparadorDeComprobantes.VENTANA_MILLIS + 1),
        )
        assertEquals(MatchResult.NO_LLEGO, v.resultado)
    }

    @Test
    fun `un pago justo dentro de la ventana si cuenta`() {
        val v = comparar(
            "Bs 50,00",
            pago("limite", 5_000L, haceMillis = ComparadorDeComprobantes.VENTANA_MILLIS),
        )
        assertEquals(MatchResult.COINCIDE, v.resultado)
    }

    @Test
    fun `un pago con marca de tiempo futura no cuenta`() {
        // Pasa si el reloj del celular del banco va adelantado. Aceptarlo abriria
        // la puerta a que un comprobante case con un pago que todavia no ocurrio.
        val v = comparar("Bs 50,00", pago("futuro", 5_000L, haceMillis = -60_000L))
        assertEquals(MatchResult.NO_LLEGO, v.resultado)
    }

    // --- OCR ilegible --------------------------------------------------------------

    @Test
    fun `si no se lee nada se pide reintentar, no se afirma que no llego`() {
        val v = comparar("captura borrosa sin numeros", pago("p1", 5_000L))
        assertEquals(MatchResult.AMBIGUO, v.resultado)
        assertTrue(v.explicacion.contains("No se pudo leer"))
    }

    // --- Los dos formatos bolivianos ------------------------------------------------

    @Test
    fun `lee las dos formas de escribir el importe`() {
        assertEquals(
            MatchResult.COINCIDE,
            comparar("Monto: Bs 1.234,56", pago("p1", 123_456L)).resultado,
        )
        assertEquals(
            MatchResult.COINCIDE,
            comparar("Monto: Bs 1,234.56", pago("p1", 123_456L)).resultado,
        )
    }
}
