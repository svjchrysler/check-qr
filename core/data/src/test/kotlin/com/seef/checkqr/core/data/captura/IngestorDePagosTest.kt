package com.seef.checkqr.core.data.captura

import com.seef.checkqr.capture.parser.NoticeParser
import com.seef.checkqr.core.common.RelojFijo
import com.seef.checkqr.core.data.plantillas.RepositorioDePlantillas
import com.seef.checkqr.core.database.dao.DiagnosticoDao
import com.seef.checkqr.core.database.dao.PagoDao
import com.seef.checkqr.core.database.dao.TurnoDao
import com.seef.checkqr.core.database.entidades.AvisoNoReconocidoEntity
import com.seef.checkqr.core.database.entidades.PagoEntity
import com.seef.checkqr.core.database.entidades.SenalPorBancoEntity
import com.seef.checkqr.core.database.entidades.TurnoEntity
import com.seef.checkqr.core.datastore.PreferenciasCheckQr
import com.seef.checkqr.core.model.BankTemplate
import com.seef.checkqr.core.model.RawNotice
import com.seef.checkqr.core.model.Wallet
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * El ingestor es el nudo de la cadena: decide si un aviso es un cobro nuevo, un
 * duplicado o basura, y a que turno pertenece. Nada de eso estaba cubierto.
 *
 * Usa el parser de verdad —es Kotlin puro y barato— con una plantilla de prueba,
 * y falsea solo la infraestructura. Asi lo que se prueba es el camino real desde
 * el texto del aviso hasta la fila que se intenta escribir.
 */
class IngestorDePagosTest {

    private val yape = "com.yape.prueba"

    private val plantilla = BankTemplate(
        wallet = Wallet.YAPE,
        packageNames = listOf(yape),
        // El grupo del pagador excluye el salto de linea a proposito: el texto
        // que ve el parser es la concatenacion de cuerpo y titulo, y un `[^.]+`
        // se traga el titulo entero. Es el error que mas cuesta ver al escribir
        // una plantilla nueva.
        bodyPattern = """Recibiste Bs (?<monto>[\d.,]+) de (?<pagador>[^.\n]+)""",
        excludePatterns = listOf("""Enviaste"""),
    )

    private lateinit var pagoDao: PagoDao
    private lateinit var turnoDao: TurnoDao
    private lateinit var diagnosticoDao: DiagnosticoDao
    private lateinit var prefs: PreferenciasCheckQr
    private lateinit var bus: BusDeCaptura
    private lateinit var ingestor: IngestorDePagos

    /** Lo que el ingestor intento escribir, para mirarlo sin una base real. */
    private val filaGuardada = slot<PagoEntity>()

    @Before
    fun preparar() {
        val plantillas = mockk<RepositorioDePlantillas>()
        coEvery { plantillas.parser() } returns NoticeParser(listOf(plantilla))

        pagoDao = mockk(relaxed = true)
        // 1 = la fila entro. El indice unico es quien decide de verdad; aqui se
        // simula su respuesta para poder probar las dos ramas.
        coEvery { pagoDao.insertarSiEsNuevo(capture(filaGuardada)) } returns 1L

        turnoDao = mockk(relaxed = true)
        coEvery { turnoDao.turnoAbiertoAhora(any()) } returns null

        diagnosticoDao = mockk(relaxed = true)

        prefs = mockk(relaxed = true)
        coEvery { prefs.deviceId() } returns "dispositivo-1"
        coEvery { prefs.vozHabilitada } returns flowOf(true)

        bus = BusDeCaptura()

        ingestor = IngestorDePagos(
            plantillas = plantillas,
            pagoDao = pagoDao,
            turnoDao = turnoDao,
            diagnosticoDao = diagnosticoDao,
            prefs = prefs,
            bus = bus,
            reloj = RelojFijo(ahora = 1_700_000_000_000L),
        )
    }

    private fun aviso(
        texto: String = "Recibiste Bs 50,00 de Juan Perez",
        paquete: String = yape,
        postedAt: Long = 1_700_000_000_000L,
        clave: String? = null,
        titulo: String? = "Yape",
    ) = RawNotice(
        sourcePackage = paquete,
        titulo = titulo,
        texto = texto,
        textoLargo = null,
        subtexto = null,
        postedAtMillis = postedAt,
        capturedAtMillis = postedAt,
        claveDelSistema = clave,
    )

    // --- El camino feliz -----------------------------------------------------

    @Test
    fun `un cobro se guarda con el monto en centavos`() = runTest {
        val r = ingestor.ingerir(aviso())

        assertTrue(r is ResultadoDeIngesta.Nuevo)
        assertEquals(5_000L, filaGuardada.captured.amountCents)
        assertEquals("Juan Perez", filaGuardada.captured.payerName)
    }

    @Test
    fun `un aviso que no es cobro se descarta sin tocar la base`() = runTest {
        val r = ingestor.ingerir(aviso(texto = "Enviaste Bs 50,00 a Juan Perez"))

        assertTrue(r is ResultadoDeIngesta.Descartado)
        coVerify(exactly = 0) { pagoDao.insertarSiEsNuevo(any()) }
    }

    @Test
    fun `un aviso de una app ajena no llega nunca a la base`() = runTest {
        // La garantia de privacidad: el parser corta por paquete y el ingestor
        // no escribe ni el pago ni el registro de "no reconocido".
        val r = ingestor.ingerir(aviso(paquete = "com.alguna.otra.app"))

        assertTrue(r is ResultadoDeIngesta.Descartado)
        coVerify(exactly = 0) { pagoDao.insertarSiEsNuevo(any()) }
        coVerify(exactly = 0) { diagnosticoDao.guardarNoReconocido(any<AvisoNoReconocidoEntity>()) }
    }

    @Test
    fun `un aviso del banco que ninguna plantilla reconoce se guarda para corregirla`() = runTest {
        val r = ingestor.ingerir(aviso(texto = "Tu saldo disponible es Bs 300"))

        assertTrue(r is ResultadoDeIngesta.NoReconocido)
        coVerify(exactly = 1) { diagnosticoDao.guardarNoReconocido(any()) }
        coVerify(exactly = 0) { pagoDao.insertarSiEsNuevo(any()) }
    }

    @Test
    fun `un aviso sin contenido legible se reporta en vez de fallar callado`() = runTest {
        // Android 15+ puede ocultar el texto a un listener no confiable: no
        // llega ni cuerpo ni titulo, solo el paquete.
        val r = ingestor.ingerir(aviso(texto = "", titulo = null))

        assertTrue(r is ResultadoDeIngesta.ContenidoOculto)
        coVerify(exactly = 0) { pagoDao.insertarSiEsNuevo(any()) }
    }

    // --- Deduplicacion -------------------------------------------------------

    @Test
    fun `si el indice unico rechaza la fila el resultado es duplicado`() = runTest {
        coEvery { pagoDao.insertarSiEsNuevo(any()) } returns -1L

        val r = ingestor.ingerir(aviso())

        assertTrue(r is ResultadoDeIngesta.Duplicado)
    }

    @Test
    fun `dos cobros iguales con claves de sistema distintas no comparten dedupKey`() = runTest {
        // Dos clientes pagan lo mismo en el mismo minuto. Son notificaciones
        // distintas, y tienen que producir dos filas: fundirlas perderia plata.
        ingestor.ingerir(aviso(clave = "0|yape|1|null|10"))
        val primera = filaGuardada.captured.dedupKey

        ingestor.ingerir(aviso(clave = "0|yape|2|null|10"))
        val segunda = filaGuardada.captured.dedupKey

        assertNotEquals(primera, segunda)
    }

    @Test
    fun `el mismo aviso reemitido conserva la dedupKey`() = runTest {
        ingestor.ingerir(aviso(clave = "0|yape|1|null|10", postedAt = 1_700_000_000_000L))
        val primera = filaGuardada.captured.dedupKey

        // El banco actualiza su notificacion: misma clave, postTime corrido.
        ingestor.ingerir(aviso(clave = "0|yape|1|null|10", postedAt = 1_700_000_000_900L))
        val reemision = filaGuardada.captured.dedupKey

        assertEquals(primera, reemision)
    }

    // --- Turno ---------------------------------------------------------------

    @Test
    fun `un pago con la caja cerrada queda sin turno`() = runTest {
        ingestor.ingerir(aviso())

        assertNull(filaGuardada.captured.shiftId)
        assertNull(filaGuardada.captured.cashierId)
    }

    @Test
    fun `un pago con la caja abierta se cuelga del turno y del cajero`() = runTest {
        coEvery { turnoDao.turnoAbiertoAhora("dispositivo-1") } returns TurnoEntity(
            id = "turno-1",
            cashierId = "cajero-1",
            deviceId = "dispositivo-1",
            openedAtMillis = 1_699_999_000_000L,
            closedAtMillis = null,
        )

        ingestor.ingerir(aviso())

        assertEquals("turno-1", filaGuardada.captured.shiftId)
        assertEquals("cajero-1", filaGuardada.captured.cashierId)
    }

    // --- Voz apagada ---------------------------------------------------------

    @Test
    fun `con la voz apagada el pago se guarda igual y se cuenta para el resumen`() = runTest {
        coEvery { prefs.vozHabilitada } returns flowOf(false)

        val r = ingestor.ingerir(aviso())

        assertTrue(r is ResultadoDeIngesta.Nuevo)
        coVerify(exactly = 1) { prefs.sumarPagoSinAnunciar() }
    }

    @Test
    fun `con la voz encendida no se acumula nada para el resumen`() = runTest {
        ingestor.ingerir(aviso())

        coVerify(exactly = 0) { prefs.sumarPagoSinAnunciar() }
    }

    // --- Estado del sistema --------------------------------------------------

    @Test
    fun `cada cobro deja senal de que el banco sigue dando avisos`() = runTest {
        ingestor.ingerir(aviso())

        coVerify(exactly = 1) { diagnosticoDao.registrarSenal(any<SenalPorBancoEntity>()) }
    }
}
