package com.seef.checkqr.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Las preferencias del dispositivo y la senal de vida del listener.
 *
 * El heartbeat del listener vive aqui y no en Room porque se escribe en cada
 * conexion y desconexion del servicio, y es un dato de estado, no un registro
 * historico. Es lo que alimenta el aviso de "el celular dejo de escuchar", que
 * es la diferencia entre un comerciante que se entera y uno que pierde pagos sin
 * saberlo.
 */
@Singleton
class PreferenciasCheckQr @Inject constructor(
    private val store: DataStore<Preferences>,
) {

    // --- Configuracion inicial ------------------------------------------------

    val onboardingCompletado: Flow<Boolean> =
        store.data.map { it[Claves.ONBOARDING_COMPLETADO] ?: false }

    suspend fun marcarOnboardingCompletado() =
        store.edit { it[Claves.ONBOARDING_COMPLETADO] = true }.let { }

    /**
     * Estado de la prueba de Bs 1 por billetera, como texto separado por comas
     * con los `Wallet.id` que ya se probaron con exito. Es lo que convierte
     * "creo que funciona" en "se que funciona".
     */
    val billeterasProbadas: Flow<Set<String>> =
        store.data.map { prefs ->
            prefs[Claves.BILLETERAS_PROBADAS]
                ?.split(',')
                ?.filter { it.isNotBlank() }
                ?.toSet()
                .orEmpty()
        }

    suspend fun marcarBilleteraProbada(walletId: String) = store.edit { prefs ->
        val actuales = prefs[Claves.BILLETERAS_PROBADAS]
            ?.split(',')
            ?.filter { it.isNotBlank() }
            .orEmpty()
            .toMutableSet()
        actuales += walletId
        prefs[Claves.BILLETERAS_PROBADAS] = actuales.sorted().joinToString(",")
    }.let { }

    // --- Identidad del dispositivo -------------------------------------------

    /**
     * Identificador propio del dispositivo, generado la primera vez.
     *
     * Deliberadamente no es un id de hardware: no hace falta, Play lo
     * restringe, y este se puede rotar si el comerciante cambia de celular.
     */
    suspend fun deviceId(): String {
        store.data.first()[Claves.DEVICE_ID]?.let { return it }
        val nuevo = UUID.randomUUID().toString()
        store.edit { it[Claves.DEVICE_ID] = nuevo }
        // Otra corrutina pudo haber escrito primero: se devuelve lo que quedo.
        return store.data.first()[Claves.DEVICE_ID] ?: nuevo
    }

    val cajeroActualId: Flow<String?> = store.data.map { it[Claves.CAJERO_ACTUAL_ID] }

    suspend fun fijarCajeroActual(id: String?) = store.edit { prefs ->
        if (id == null) prefs.remove(Claves.CAJERO_ACTUAL_ID) else prefs[Claves.CAJERO_ACTUAL_ID] = id
    }.let { }

    // --- Voz -------------------------------------------------------------------

    val vozHabilitada: Flow<Boolean> = store.data.map { it[Claves.VOZ_HABILITADA] ?: true }

    suspend fun fijarVozHabilitada(valor: Boolean) =
        store.edit { it[Claves.VOZ_HABILITADA] = valor }.let { }

    /**
     * Cuantos pagos llegaron con la voz apagada. Al reabrir la caja se anuncia
     * el resumen en lugar de soltar veinte anuncios seguidos.
     */
    val pagosSinAnunciar: Flow<Int> = store.data.map { (it[Claves.PAGOS_SIN_ANUNCIAR] ?: 0L).toInt() }

    suspend fun sumarPagoSinAnunciar() = store.edit { prefs ->
        prefs[Claves.PAGOS_SIN_ANUNCIAR] = (prefs[Claves.PAGOS_SIN_ANUNCIAR] ?: 0L) + 1L
    }.let { }

    suspend fun limpiarPagosSinAnunciar() =
        store.edit { it[Claves.PAGOS_SIN_ANUNCIAR] = 0L }.let { }

    // --- Widget ----------------------------------------------------------------

    /** Oculta los montos en el widget, para que no se lean sobre el mostrador. */
    val modoDiscreto: Flow<Boolean> = store.data.map { it[Claves.MODO_DISCRETO] ?: false }

    suspend fun fijarModoDiscreto(valor: Boolean) =
        store.edit { it[Claves.MODO_DISCRETO] = valor }.let { }

    // --- Estado del sistema ----------------------------------------------------

    /**
     * Lo ultimo que reporto el listener, o null si nunca reporto nada.
     *
     * El tercer estado es necesario: `onListenerConnected` solo se dispara al
     * conectar, asi que tras un arranque de la app con el servicio ya conectado
     * no llega ningun callback. Si null significara "desconectado", la app
     * avisaria "dejó de escuchar" justo cuando todo funciona.
     */
    val listenerConectado: Flow<Boolean?> =
        store.data.map { it[Claves.LISTENER_CONECTADO] }

    val ultimoLatidoListenerMillis: Flow<Long> =
        store.data.map { it[Claves.ULTIMO_LATIDO_LISTENER] ?: 0L }

    suspend fun registrarLatidoListener(conectado: Boolean, ahoraMillis: Long) =
        store.edit { prefs ->
            prefs[Claves.LISTENER_CONECTADO] = conectado
            prefs[Claves.ULTIMO_LATIDO_LISTENER] = ahoraMillis
        }.let { }

    /**
     * Tiempo de arranque del sistema en el ultimo uso.
     *
     * Si cambia, el celular se reinicio y la voz quedo apagada: Android 17 no
     * permite arrancar el servicio en primer plano sin una accion del usuario.
     * La app tiene que mostrar "Toca para reactivar la voz" en vez de quedarse
     * muda sin explicacion.
     */
    val ultimoArranqueMillis: Flow<Long> = store.data.map { it[Claves.ULTIMO_ARRANQUE] ?: 0L }

    suspend fun registrarArranque(arranqueMillis: Long) =
        store.edit { it[Claves.ULTIMO_ARRANQUE] = arranqueMillis }.let { }

    /** Cierto si hubo un reinicio desde la ultima vez que la app corrio. */
    suspend fun huboReinicio(arranqueActualMillis: Long): Boolean {
        val anterior = store.data.first()[Claves.ULTIMO_ARRANQUE] ?: return false
        return arranqueActualMillis != anterior
    }

    // --- Privacidad ------------------------------------------------------------

    /**
     * Consentimiento explicito para subir al backend los avisos que ninguna
     * plantilla reconocio. Por omision **no**: ese texto puede traer el nombre de
     * una persona, y mejorar nuestras plantillas no es razon suficiente para
     * enviarlo sin permiso.
     */
    val compartirAvisosNoReconocidos: Flow<Boolean> =
        store.data.map { it[Claves.COMPARTIR_NO_RECONOCIDOS] ?: false }

    suspend fun fijarCompartirAvisosNoReconocidos(valor: Boolean) =
        store.edit { it[Claves.COMPARTIR_NO_RECONOCIDOS] = valor }.let { }

    // --- Plantillas ------------------------------------------------------------

    val versionDePlantillas: Flow<Int> =
        store.data.map { (it[Claves.VERSION_PLANTILLAS] ?: 0L).toInt() }

    suspend fun fijarVersionDePlantillas(version: Int) =
        store.edit { it[Claves.VERSION_PLANTILLAS] = version.toLong() }.let { }

    private object Claves {
        val ONBOARDING_COMPLETADO = booleanPreferencesKey("onboarding_completado")
        val BILLETERAS_PROBADAS = stringPreferencesKey("billeteras_probadas")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val CAJERO_ACTUAL_ID = stringPreferencesKey("cajero_actual_id")
        val VOZ_HABILITADA = booleanPreferencesKey("voz_habilitada")
        val PAGOS_SIN_ANUNCIAR = longPreferencesKey("pagos_sin_anunciar")
        val MODO_DISCRETO = booleanPreferencesKey("modo_discreto")
        val LISTENER_CONECTADO = booleanPreferencesKey("listener_conectado")
        val ULTIMO_LATIDO_LISTENER = longPreferencesKey("ultimo_latido_listener")
        val ULTIMO_ARRANQUE = longPreferencesKey("ultimo_arranque")
        val COMPARTIR_NO_RECONOCIDOS = booleanPreferencesKey("compartir_no_reconocidos")
        val VERSION_PLANTILLAS = longPreferencesKey("version_plantillas")
    }
}
