package com.seef.checkqr.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PreferenciasCheckQrTest {

    @get:Rule val carpeta = TemporaryFolder()

    private lateinit var ambito: CoroutineScope
    private lateinit var store: DataStore<Preferences>
    private lateinit var prefs: PreferenciasCheckQr

    @Before
    fun abrir() {
        ambito = CoroutineScope(UnconfinedTestDispatcher())
        store = PreferenceDataStoreFactory.create(scope = ambito) {
            File(carpeta.newFolder(), "test.preferences_pb")
        }
        prefs = PreferenciasCheckQr(store)
    }

    @After
    fun cerrar() = ambito.cancel()

    // --- Valores por omision --------------------------------------------------

    @Test
    fun `el latido distingue desconocido de desconectado`() = runTest {
        // Es la diferencia entre avisar "dejó de escuchar" y no avisarlo. Con
        // null (nunca reporto) la app NO debe dar la alarma: el callback
        // onListenerConnected solo llega al conectar, y si el servicio ya
        // estaba conectado al arrancar la app, nunca llega.
        assertNull(prefs.listenerConectado.first())

        prefs.registrarLatidoListener(conectado = false, ahoraMillis = 1L)
        assertEquals(false, prefs.listenerConectado.first())
    }

    @Test
    fun `valores por omision seguros`() = runTest {
        assertFalse("el onboarding arranca sin completar", prefs.onboardingCompletado.first())
        assertFalse("el modo discreto arranca apagado", prefs.modoDiscreto.first())
        assertNull("el listener arranca en desconocido, no en desconectado", prefs.listenerConectado.first())
        assertEquals(0, prefs.versionDePlantillas.first())
        assertEquals(0, prefs.pagosSinAnunciar.first())
        assertNull(prefs.cajeroActualId.first())
    }

    @Test
    fun `compartir avisos no reconocidos esta apagado por omision`() = runTest {
        // Ese texto puede traer el nombre de una persona: mejorar nuestras
        // plantillas no alcanza como razon para enviarlo sin permiso explicito.
        assertFalse(prefs.compartirAvisosNoReconocidos.first())
    }

    // --- Identidad del dispositivo -------------------------------------------

    @Test
    fun `el deviceId se genera una vez y no cambia`() = runTest {
        val primero = prefs.deviceId()
        assertTrue(primero.isNotBlank())
        assertEquals("pedirlo de nuevo devuelve el mismo", primero, prefs.deviceId())
        assertEquals(primero, prefs.deviceId())
    }

    // --- Prueba de Bs 1 por billetera ----------------------------------------

    @Test
    fun `las billeteras probadas se acumulan sin duplicar`() = runTest {
        assertTrue(prefs.billeterasProbadas.first().isEmpty())

        prefs.marcarBilleteraProbada("yape")
        prefs.marcarBilleteraProbada("bnb")
        prefs.marcarBilleteraProbada("yape")

        assertEquals(setOf("yape", "bnb"), prefs.billeterasProbadas.first())
    }

    // --- Voz -------------------------------------------------------------------

    @Test
    fun `cuenta y limpia los pagos que llegaron con la voz apagada`() = runTest {
        prefs.sumarPagoSinAnunciar()
        prefs.sumarPagoSinAnunciar()
        prefs.sumarPagoSinAnunciar()
        assertEquals(3, prefs.pagosSinAnunciar.first())

        prefs.limpiarPagosSinAnunciar()
        assertEquals(0, prefs.pagosSinAnunciar.first())
    }

    @Test
    fun `la voz se puede apagar y volver a prender`() = runTest {
        assertTrue("por omision la voz esta habilitada", prefs.vozHabilitada.first())
        prefs.fijarVozHabilitada(false)
        assertFalse(prefs.vozHabilitada.first())
        prefs.fijarVozHabilitada(true)
        assertTrue(prefs.vozHabilitada.first())
    }

    // --- Estado del sistema ----------------------------------------------------

    @Test
    fun `registra el latido del listener`() = runTest {
        prefs.registrarLatidoListener(conectado = true, ahoraMillis = 1_234L)
        assertEquals(true, prefs.listenerConectado.first())
        assertEquals(1_234L, prefs.ultimoLatidoListenerMillis.first())

        prefs.registrarLatidoListener(conectado = false, ahoraMillis = 5_678L)
        assertEquals(false, prefs.listenerConectado.first())
        assertEquals(5_678L, prefs.ultimoLatidoListenerMillis.first())
    }

    @Test
    fun `detecta el reinicio del celular por el cambio de tiempo de arranque`() = runTest {
        // La primera vez no hay nada guardado: no se puede afirmar que hubo
        // reinicio, asi que se responde que no.
        assertFalse("sin dato previo no se afirma un reinicio", prefs.huboReinicio(1_000L))

        prefs.registrarArranque(1_000L)
        assertFalse("mismo arranque, no hubo reinicio", prefs.huboReinicio(1_000L))

        // Tras un reinicio el tiempo de arranque del sistema es otro. Es lo que
        // permite avisar "Toca para reactivar la voz": Android 17 no deja
        // arrancar el servicio en primer plano sin una accion del usuario.
        assertTrue("arranque distinto, hubo reinicio", prefs.huboReinicio(9_000L))
    }

    // --- Cajero y plantillas ---------------------------------------------------

    @Test
    fun `fija y limpia el cajero actual`() = runTest {
        prefs.fijarCajeroActual("cajero-1")
        assertEquals("cajero-1", prefs.cajeroActualId.first())
        prefs.fijarCajeroActual(null)
        assertNull(prefs.cajeroActualId.first())
    }

    @Test
    fun `guarda la version de plantillas instalada`() = runTest {
        prefs.fijarVersionDePlantillas(7)
        assertEquals(7, prefs.versionDePlantillas.first())
    }
}
