package com.seef.checkqr.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.seef.checkqr.core.database.dao.PagoDao
import com.seef.checkqr.core.database.entidades.PagoEntity
import com.seef.checkqr.core.model.ParseConfidence
import com.seef.checkqr.core.model.Payment
import com.seef.checkqr.core.model.PaymentLevel
import com.seef.checkqr.core.model.SyncState
import com.seef.checkqr.core.model.Wallet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Las dos garantias que la base tiene que dar, y que no se pueden dar desde
 * Kotlin: que un aviso repetido no entre dos veces, y que un pago se reclame una
 * sola vez.
 */
@RunWith(RobolectricTestRunner::class)
class PagoDaoTest {

    private lateinit var db: CheckQrDatabase
    private lateinit var dao: PagoDao

    @Before
    fun abrir() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CheckQrDatabase::class.java,
        ).build()
        dao = db.pagoDao()
    }

    @After
    fun cerrar() = db.close()

    private fun pago(
        id: String = "p1",
        paquete: String = "com.banco.yape",
        centavos: Long = 5_000L,
        referencia: String? = "REF123",
        postedAt: Long = 1_700_000_000_000L,
        ventana: Long = Payment.VENTANA_DEDUP_MILLIS,
    ) = PagoEntity(
        id = id,
        dedupKey = Payment.dedupKeyDe(paquete, centavos, referencia, postedAt, ventana),
        walletId = Wallet.YAPE.id,
        sourcePackage = paquete,
        amountCents = centavos,
        currency = Payment.MONEDA_BOLIVIA,
        payerName = "Juan Perez",
        reference = referencia,
        notifPostedAtMillis = postedAt,
        capturedAtMillis = postedAt + 100L,
        levelId = PaymentLevel.AVISO_BANCO.id,
        confidenceId = ParseConfidence.COMPLETA.id,
        syncStateId = SyncState.PENDIENTE.id,
        shiftId = null,
        cashierId = null,
        claimedByReceiptId = null,
        rawText = "Recibiste Bs 50,00 de Juan Perez",
    )

    // --- Deduplicacion --------------------------------------------------------

    @Test
    fun `un pago nuevo entra`() = runTest {
        assertTrue(dao.insertarSiEsNuevo(pago()) > 0L)
        assertNotNull(dao.porId("p1"))
    }

    @Test
    fun `el mismo aviso reemitido no entra dos veces`() = runTest {
        assertTrue(dao.insertarSiEsNuevo(pago(id = "p1")) > 0L)
        // Mismo paquete, monto, referencia y bloque de tiempo: es el mismo cobro
        // aunque el banco le haya puesto otro postTime dentro de la ventana.
        val repetido = pago(id = "p2", postedAt = 1_700_000_000_000L + 30_000L)
        assertEquals(-1L, dao.insertarSiEsNuevo(repetido))

        assertEquals(1, dao.enRango(0L, Long.MAX_VALUE).first().size)
        assertNull("no debe existir el segundo", dao.porId("p2"))
    }

    @Test
    fun `dos cobros distintos del mismo monto en la misma ventana se distinguen por referencia`() =
        runTest {
            assertTrue(dao.insertarSiEsNuevo(pago(id = "p1", referencia = "REF111")) > 0L)
            assertTrue(dao.insertarSiEsNuevo(pago(id = "p2", referencia = "REF222")) > 0L)
            assertEquals(2, dao.enRango(0L, Long.MAX_VALUE).first().size)
        }

    @Test
    fun `dos cobros del mismo monto sin referencia en ventanas distintas si entran`() = runTest {
        val base = 1_700_000_000_000L
        assertTrue(dao.insertarSiEsNuevo(pago(id = "p1", referencia = null, postedAt = base)) > 0L)
        // Dos minutos despues: otro bloque de tiempo, otro cobro.
        assertTrue(
            dao.insertarSiEsNuevo(
                pago(id = "p2", referencia = null, postedAt = base + 120_000L),
            ) > 0L,
        )
        assertEquals(2, dao.enRango(0L, Long.MAX_VALUE).first().size)
    }

    @Test
    fun `sin referencia dos cobros iguales en la misma ventana se colapsan`() = runTest {
        // Es el precio conocido de la deduplicacion: dos clientes que pagan el
        // mismo monto dentro del mismo minuto y cuyo banco no manda referencia
        // se cuentan como uno. Queda documentado aqui para que se note si alguien
        // cambia la ventana sin darse cuenta.
        val base = 1_700_000_000_000L
        assertTrue(dao.insertarSiEsNuevo(pago(id = "p1", referencia = null, postedAt = base)) > 0L)
        assertEquals(
            -1L,
            dao.insertarSiEsNuevo(pago(id = "p2", referencia = null, postedAt = base + 5_000L)),
        )
    }

    @Test
    fun `la referencia se normaliza para que el caso no genere un duplicado`() = runTest {
        assertTrue(dao.insertarSiEsNuevo(pago(id = "p1", referencia = "abc123")) > 0L)
        assertEquals(-1L, dao.insertarSiEsNuevo(pago(id = "p2", referencia = "ABC123")))
    }

    // --- Reclamo unico de comprobante ----------------------------------------

    @Test
    fun `un pago se reclama una sola vez`() = runTest {
        dao.insertarSiEsNuevo(pago())

        assertEquals("primer reclamo", 1, dao.reclamarSiEstaLibre("p1", "comprobante-A"))
        assertEquals("segundo reclamo", 0, dao.reclamarSiEstaLibre("p1", "comprobante-B"))

        assertEquals("comprobante-A", dao.porId("p1")?.claimedByReceiptId)
    }

    @Test
    fun `reclamar un pago que no existe no hace nada`() = runTest {
        assertEquals(0, dao.reclamarSiEstaLibre("no-existe", "comprobante-A"))
    }

    @Test
    fun `los pagos ya reclamados no aparecen como candidatos`() = runTest {
        dao.insertarSiEsNuevo(pago(id = "p1", referencia = "R1"))
        dao.insertarSiEsNuevo(pago(id = "p2", referencia = "R2"))

        assertEquals(2, dao.candidatosPorMonto(5_000L, 0L, Long.MAX_VALUE).size)
        dao.reclamarSiEstaLibre("p1", "comprobante-A")
        val quedan = dao.candidatosPorMonto(5_000L, 0L, Long.MAX_VALUE)
        assertEquals(1, quedan.size)
        assertEquals("p2", quedan.single().id)
    }

    @Test
    fun `busca candidatos por referencia`() = runTest {
        dao.insertarSiEsNuevo(pago(id = "p1", referencia = "UNICA99"))
        assertEquals("p1", dao.candidatosPorReferencia("UNICA99").single().id)
        dao.reclamarSiEstaLibre("p1", "comprobante-A")
        assertTrue(dao.candidatosPorReferencia("UNICA99").isEmpty())
    }

    // --- Consultas del cuadre -------------------------------------------------

    @Test
    fun `el rango es semiabierto para que dos dias no se solapen`() = runTest {
        val limite = 1_700_000_000_000L
        dao.insertarSiEsNuevo(pago(id = "antes", referencia = "A", postedAt = limite - 1))
        dao.insertarSiEsNuevo(pago(id = "justo", referencia = "B", postedAt = limite))

        val primerDia = dao.enRango(0L, limite).first().map { it.id }
        val segundoDia = dao.enRango(limite, Long.MAX_VALUE).first().map { it.id }

        assertEquals(listOf("antes"), primerDia)
        assertEquals(listOf("justo"), segundoDia)
    }

    @Test
    fun `suma el total del rango`() = runTest {
        dao.insertarSiEsNuevo(pago(id = "p1", centavos = 5_000L, referencia = "A"))
        dao.insertarSiEsNuevo(pago(id = "p2", centavos = 12_050L, referencia = "B"))
        assertEquals(17_050L, dao.totalEnRango(0L, Long.MAX_VALUE).first())
    }

    @Test
    fun `el total de un rango vacio es cero, no nulo`() = runTest {
        assertEquals(0L, dao.totalEnRango(0L, Long.MAX_VALUE).first())
    }

    @Test
    fun `ordena del mas nuevo al mas viejo`() = runTest {
        val base = 1_700_000_000_000L
        dao.insertarSiEsNuevo(pago(id = "viejo", referencia = "A", postedAt = base))
        dao.insertarSiEsNuevo(pago(id = "nuevo", referencia = "B", postedAt = base + 300_000L))
        assertEquals(listOf("nuevo", "viejo"), dao.ultimos(10).first().map { it.id })
    }

    // --- Sincronizado ---------------------------------------------------------

    @Test
    fun `lista y marca los pagos pendientes de subir`() = runTest {
        dao.insertarSiEsNuevo(pago(id = "p1", referencia = "A"))
        assertEquals(1, dao.porEstadoDeSync(SyncState.PENDIENTE.id, 10).size)

        dao.marcarEstadoDeSync("p1", SyncState.SINCRONIZADO.id)
        assertTrue(dao.porEstadoDeSync(SyncState.PENDIENTE.id, 10).isEmpty())
        assertEquals(1, dao.porEstadoDeSync(SyncState.SINCRONIZADO.id, 10).size)
    }

    // --- Retencion ------------------------------------------------------------

    @Test
    fun `purga el texto crudo viejo sin borrar el pago`() = runTest {
        val base = 1_700_000_000_000L
        dao.insertarSiEsNuevo(pago(id = "viejo", referencia = "A", postedAt = base))
        dao.insertarSiEsNuevo(pago(id = "nuevo", referencia = "B", postedAt = base + 600_000L))

        assertEquals(1, dao.purgarTextoCrudoAnteriorA(base + 1))

        assertNull(dao.porId("viejo")?.rawText)
        assertNotNull("el pago en si no se toca", dao.porId("viejo"))
        assertNotNull("el reciente conserva su texto", dao.porId("nuevo")?.rawText)
    }

    // --- Los enums se guardan como texto estable ------------------------------

    @Test
    fun `los enums viajan como su id de texto, no como ordinal`() = runTest {
        dao.insertarSiEsNuevo(pago())
        val guardado = dao.porId("p1")!!
        assertEquals("yape", guardado.walletId)
        assertEquals("aviso_banco", guardado.levelId)
        assertEquals("completa", guardado.confidenceId)
        assertEquals("pendiente", guardado.syncStateId)
    }
}
