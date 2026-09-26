package com.seef.checkqr.core.data.repositorios

import com.seef.checkqr.capture.parser.PlantillasBase
import com.seef.checkqr.core.common.Reloj
import com.seef.checkqr.core.database.dao.DiagnosticoDao
import com.seef.checkqr.core.datastore.PreferenciasCheckQr
import com.seef.checkqr.core.model.Wallet
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Lo que muestra la pantalla "Estado del sistema". */
data class EstadoDelSistema(
    val listenerConectado: Boolean,
    val ultimoLatidoMillis: Long,
    val vozHabilitada: Boolean,
    /** Ultimo aviso recibido por billetera. Ausente = nunca llego nada. */
    val ultimoAvisoPorBilletera: Map<Wallet, SenalDeBanco>,
    /** Billeteras de las que nunca se recibio un aviso. */
    val billeterasSinSenal: List<Wallet>,
    /** Cierto mientras las plantillas empaquetadas sigan sin verificar. */
    val plantillasSinVerificar: Boolean,
    val avisosNoReconocidos: Int,
) {
    /** Si esto es cierto, la app puede estar perdiendo pagos ahora mismo. */
    val requiereAtencion: Boolean
        get() = !listenerConectado || billeterasSinSenal.isNotEmpty() || avisosNoReconocidos > 0
}

data class SenalDeBanco(
    val sourcePackage: String,
    val ultimoAvisoMillis: Long,
    /** El ultimo aviso de este banco llego sin contenido legible. */
    val ultimoOculto: Boolean,
)

@Singleton
class RepositorioDeEstado @Inject constructor(
    private val diagnosticoDao: DiagnosticoDao,
    private val prefs: PreferenciasCheckQr,
    private val reloj: Reloj,
) {

    fun estado(): Flow<EstadoDelSistema> = combine(
        diagnosticoDao.senales(),
        prefs.listenerConectado,
        prefs.ultimoLatidoListenerMillis,
        prefs.vozHabilitada,
        diagnosticoDao.noReconocidos(MAX_NO_RECONOCIDOS),
    ) { senales, conectado, latido, voz, noReconocidos ->
        val porBilletera = senales.mapNotNull { s ->
            Wallet.porId(s.walletId)?.let { w ->
                w to SenalDeBanco(s.sourcePackage, s.ultimoAvisoMillis, s.ultimoOculto)
            }
        }.toMap()

        EstadoDelSistema(
            listenerConectado = conectado,
            ultimoLatidoMillis = latido,
            vozHabilitada = voz,
            ultimoAvisoPorBilletera = porBilletera,
            billeterasSinSenal = Wallet.soportadas.filter { it !in porBilletera },
            plantillasSinVerificar = PlantillasBase.sinVerificar,
            avisosNoReconocidos = noReconocidos.size,
        )
    }

    /** Detecta un reinicio del celular, que apaga la voz sin avisar. */
    suspend fun detectarReinicio(): Boolean {
        val arranque = reloj.ahoraMillis() - reloj.desdeElArranqueMillis()
        val hubo = prefs.huboReinicio(arranque)
        prefs.registrarArranque(arranque)
        return hubo
    }

    suspend fun registrarLatidoListener(conectado: Boolean) =
        prefs.registrarLatidoListener(conectado, reloj.ahoraMillis())

    private companion object {
        const val MAX_NO_RECONOCIDOS = 50
    }
}
