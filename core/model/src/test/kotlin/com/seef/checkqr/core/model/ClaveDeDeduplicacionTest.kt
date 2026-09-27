package com.seef.checkqr.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * La clave de deduplicacion decide si un aviso es un cobro nuevo o una copia.
 *
 * Los dos errores no cuestan lo mismo, y por eso se prueban por separado:
 * contar de mas deja un pago repetido que el comerciante ve en la lista y
 * corrige; contar de menos borra un cobro sin dejar rastro. Los casos que
 * importan son los del segundo grupo.
 */
class ClaveDeDeduplicacionTest {

    private val yape = "com.bcp.innovacxion.yapeapp"

    private fun clave(
        paquete: String = yape,
        centavos: Long = 5_000L,
        referencia: String? = null,
        postedAt: Long = 1_700_000_000_000L,
        claveDelSistema: String? = null,
    ) = Payment.dedupKeyDe(
        sourcePackage = paquete,
        amountCents = centavos,
        reference = referencia,
        postedAtMillis = postedAt,
        claveDelSistema = claveDelSistema,
    )

    // --- Con referencia: es la identidad real del cobro ----------------------

    @Test
    fun `con referencia la clave no depende del reloj`() {
        val original = clave(referencia = "000123456", postedAt = 1_700_000_000_000L)
        val mediaHoraDespues = clave(referencia = "000123456", postedAt = 1_700_001_800_000L)

        // Un banco que reemite el mismo aviso mucho despues sigue hablando del
        // mismo cobro. Con referencia no hace falta adivinar por tiempo.
        assertEquals(original, mediaHoraDespues)
    }

    @Test
    fun `la referencia se normaliza antes de comparar`() {
        assertEquals(clave(referencia = "ab12"), clave(referencia = "  AB12  "))
    }

    @Test
    fun `referencias distintas son cobros distintos`() {
        assertNotEquals(clave(referencia = "A1"), clave(referencia = "A2"))
    }

    // --- Sin referencia: la clave del sistema evita perder plata -------------

    @Test
    fun `mismo aviso reemitido con la misma clave es un duplicado`() {
        val primera = clave(claveDelSistema = "0|com.yape|42|null|10230")
        val reemision = clave(claveDelSistema = "0|com.yape|42|null|10230")

        assertEquals(primera, reemision)
    }

    @Test
    fun `dos cobros del mismo monto en el mismo minuto no se funden`() {
        // El caso que motiva todo esto: dos clientes pagan Bs 50 con la misma
        // billetera dentro del mismo minuto. Son dos notificaciones distintas,
        // asi que el sistema les da claves distintas. Si se fundieran, el
        // comerciante cobraria una sola y no habria forma de notarlo.
        val primerCliente = clave(claveDelSistema = "0|com.yape|42|null|10230")
        val segundoCliente = clave(claveDelSistema = "0|com.yape|43|null|10230")

        assertNotEquals(primerCliente, segundoCliente)
    }

    @Test
    fun `la clave del sistema no cruza el bloque de tiempo`() {
        // Misma notificacion actualizada mucho despues: el banco reutiliza una
        // sola notificacion para cobros sucesivos, asi que pasado el bloque se
        // trata como un cobro nuevo en lugar de descartarlo.
        val enElBloque = clave(claveDelSistema = "k", postedAt = 1_700_000_000_000L)
        val bloqueSiguiente = clave(claveDelSistema = "k", postedAt = 1_700_000_120_000L)

        assertNotEquals(enElBloque, bloqueSiguiente)
    }

    // --- Sin referencia ni clave: el caso degradado --------------------------

    @Test
    fun `sin referencia ni clave solo queda el bloque de tiempo`() {
        // Este es el unico camino que puede fundir dos cobros distintos. Se
        // prueba para dejarlo explicito, no porque este bien: es lo que hay
        // cuando el aviso no trae referencia y no viene del sistema.
        assertEquals(clave(postedAt = 1_700_000_000_000L), clave(postedAt = 1_700_000_030_000L))
    }

    @Test
    fun `sin referencia ni clave dos bloques distintos son cobros distintos`() {
        assertNotEquals(clave(postedAt = 1_700_000_000_000L), clave(postedAt = 1_700_000_120_000L))
    }

    // --- Lo que siempre separa ----------------------------------------------

    @Test
    fun `montos distintos son cobros distintos`() {
        assertNotEquals(clave(centavos = 5_000L), clave(centavos = 5_001L))
    }

    @Test
    fun `billeteras distintas son cobros distintos`() {
        // Mismo monto y mismo minuto en dos apps: son dos cobros.
        assertNotEquals(clave(paquete = yape), clave(paquete = "com.tigo.money.bo"))
    }

    @Test
    fun `una referencia vacia no se confunde con tener referencia`() {
        // "" y "   " tienen que caer al camino sin referencia, no producir una
        // clave `ref=` que fundiria todos los cobros del mismo monto.
        val sinReferencia = clave(referencia = null, claveDelSistema = "k")
        assertEquals(sinReferencia, clave(referencia = "   ", claveDelSistema = "k"))
    }
}
