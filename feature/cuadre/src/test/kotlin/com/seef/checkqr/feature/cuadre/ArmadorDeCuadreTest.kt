package com.seef.checkqr.feature.cuadre

import com.seef.checkqr.core.model.ParseConfidence
import com.seef.checkqr.core.model.Payment
import com.seef.checkqr.core.model.PaymentLevel
import com.seef.checkqr.core.model.SyncState
import com.seef.checkqr.core.model.Wallet
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArmadorDeCuadreTest {

    private val dia = LocalDate(2026, 3, 10)

    private fun pago(
        id: String,
        centavos: Long,
        wallet: Wallet = Wallet.YAPE,
        cajero: String? = null,
        turno: String? = null,
    ) = Payment(
        id = id,
        dedupKey = "clave-$id",
        wallet = wallet,
        sourcePackage = "com.banco.x",
        amountCents = centavos,
        payerName = "Cliente $id",
        reference = null,
        notifPostedAtMillis = 1_700_000_000_000L,
        capturedAtMillis = 1_700_000_000_000L,
        level = PaymentLevel.AVISO_BANCO,
        confidence = ParseConfidence.PARCIAL,
        syncState = SyncState.PENDIENTE,
        shiftId = turno,
        cashierId = cajero,
        claimedByReceiptId = null,
        rawText = null,
    )

    // --- Lo que no puede fallar en un cierre de caja --------------------------

    @Test
    fun `el total es la suma exacta de los pagos`() {
        val c = ArmadorDeCuadre.armar(
            dia,
            listOf(pago("a", 5_000L), pago("b", 12_050L), pago("c", 10L)),
        )
        assertEquals(17_060L, c.totalCentavos)
        assertEquals(3, c.cantidad)
    }

    @Test
    fun `las billeteras suman el total, sin perder ni un centavo`() {
        val c = ArmadorDeCuadre.armar(
            dia,
            listOf(
                pago("a", 5_000L, Wallet.YAPE),
                pago("b", 12_050L, Wallet.BCP),
                pago("c", 33L, Wallet.TIGO_MONEY),
            ),
        )
        assertTrue("el cuadre tiene que cuadrar", c.cuadra)
        assertEquals(c.totalCentavos, c.porBilletera.sumOf { it.totalCentavos })
    }

    @Test
    fun `un pago de billetera desconocida tambien suma`() {
        // Wallet.DESCONOCIDA no esta en `soportadas`. Si se lo dejara fuera del
        // agrupado, el cuadre no sumaria y el comerciante dejaria de confiar.
        val c = ArmadorDeCuadre.armar(
            dia,
            listOf(pago("a", 5_000L, Wallet.YAPE), pago("b", 900L, Wallet.DESCONOCIDA)),
        )
        assertTrue(c.cuadra)
        assertEquals(5_900L, c.totalCentavos)
        assertTrue(c.porBilletera.any { it.etiqueta == Wallet.DESCONOCIDA.nombreVisible })
    }

    @Test
    fun `las billeteras sin movimiento no aparecen`() {
        val c = ArmadorDeCuadre.armar(dia, listOf(pago("a", 5_000L, Wallet.YAPE)))
        assertEquals(1, c.porBilletera.size)
        assertEquals("Yape", c.porBilletera.single().etiqueta)
    }

    // --- Turnos ------------------------------------------------------------------

    @Test
    fun `los pagos con la caja cerrada van aparte, no dentro de un turno`() {
        // Colgarlos de un turno cualquiera haria que a ese cajero no le cuadre su
        // propio arqueo, que es justo lo que esta pantalla evita.
        val c = ArmadorDeCuadre.armar(
            dia,
            listOf(
                pago("a", 5_000L, cajero = "c1", turno = "t1"),
                pago("b", 3_000L), // caja cerrada
            ),
        )
        assertEquals(1, c.porTurno.size)
        assertEquals(5_000L, c.porTurno.single().totalCentavos)
        assertEquals(1, c.sinTurno.cantidad)
        assertEquals(3_000L, c.sinTurno.totalCentavos)
        // Y el total sigue siendo el de verdad.
        assertEquals(8_000L, c.totalCentavos)
    }

    @Test
    fun `agrupa por cajero y ordena por lo cobrado`() {
        val c = ArmadorDeCuadre.armar(
            dia,
            listOf(
                pago("a", 1_000L, cajero = "ana"),
                pago("b", 9_000L, cajero = "beto"),
                pago("c", 500L, cajero = "ana"),
            ),
            nombreDeCajero = { if (it == "ana") "Ana" else "Beto" },
        )
        assertEquals(listOf("Beto", "Ana"), c.porCajero.map { it.etiqueta })
        assertEquals(1_500L, c.porCajero.last().totalCentavos)
        assertEquals(2, c.porCajero.last().cantidad)
    }

    // --- Bordes -------------------------------------------------------------------

    @Test
    fun `un dia sin pagos da un cuadre en cero, no un error`() {
        val c = ArmadorDeCuadre.armar(dia, emptyList())
        assertEquals(0L, c.totalCentavos)
        assertEquals(0, c.cantidad)
        assertTrue(c.porBilletera.isEmpty())
        assertTrue(c.cuadra)
    }

    @Test
    fun `el detalle va del mas nuevo al mas viejo`() {
        val base = 1_700_000_000_000L
        val c = ArmadorDeCuadre.armar(
            dia,
            listOf(
                pago("viejo", 100L).copy(notifPostedAtMillis = base),
                pago("nuevo", 200L).copy(notifPostedAtMillis = base + 60_000L),
            ),
        )
        assertEquals(listOf("nuevo", "viejo"), c.pagos.map { it.id })
    }
}
