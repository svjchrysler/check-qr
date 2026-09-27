package com.seef.checkqr.core.data.repositorios

import com.seef.checkqr.core.common.ComparadorDeComprobantes
import com.seef.checkqr.core.common.Veredicto
import com.seef.checkqr.core.database.dao.ComprobanteDao
import com.seef.checkqr.core.database.dao.PagoDao
import com.seef.checkqr.core.database.entidades.aDominio
import com.seef.checkqr.core.database.entidades.aEntity
import com.seef.checkqr.core.model.MatchResult
import com.seef.checkqr.core.model.Payment
import com.seef.checkqr.core.model.Receipt
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Lo que devuelve verificar un comprobante. */
data class ResultadoDeVerificacion(
    val veredicto: Veredicto,
    /**
     * Cierto solo si este comprobante quedo efectivamente asociado al pago.
     *
     * Puede ser falso aunque el veredicto sea COINCIDE: entre que se busco el
     * candidato y se intento reclamarlo, otro cajero pudo haberlo reclamado.
     */
    val reclamado: Boolean,
)

/**
 * Verificar el comprobante que muestra el cliente.
 *
 * La regla del producto es que **un pago se reclama una sola vez**, y aqui esta
 * el unico sitio donde eso se decide. La condicion la impone la base con un
 * UPDATE que solo escribe si el pago sigue libre: comprobarlo en Kotlin y
 * despues escribir dejaria una ventana en la que dos cajeros cobran el mismo
 * aviso.
 */
@Singleton
class RepositorioDeComprobantes @Inject constructor(
    private val pagoDao: PagoDao,
    private val comprobanteDao: ComprobanteDao,
) {

    suspend fun verificar(textoOcr: String, ahoraMillis: Long): ResultadoDeVerificacion {
        val lectura = ComparadorDeComprobantes.leer(textoOcr)

        // Los candidatos los filtra la base por "sin reclamar": es lo que evita
        // que un pago ya cobrado vuelva a aparecer como disponible.
        val candidatos = buildList {
            lectura.reference?.let { addAll(pagoDao.candidatosPorReferencia(it)) }
            lectura.amountCents?.let { monto ->
                addAll(
                    pagoDao.candidatosPorMonto(
                        amountCents = monto,
                        desdeMillis = ahoraMillis - ComparadorDeComprobantes.VENTANA_MILLIS,
                        hastaMillis = ahoraMillis + 1,
                    ),
                )
            }
        }.distinctBy { it.id }.map { it.aDominio() }

        val veredicto = ComparadorDeComprobantes.comparar(lectura, candidatos, ahoraMillis)

        val comprobanteId = UUID.randomUUID().toString()
        val pago = veredicto.pago

        // Solo se reclama cuando hay un unico candidato. Con ambiguedad decide una
        // persona: un falso "si, llego" le cuesta la mercaderia al comerciante.
        val reclamado = if (veredicto.resultado == MatchResult.COINCIDE && pago != null) {
            pagoDao.reclamarSiEstaLibre(pago.id, comprobanteId) == 1
        } else {
            false
        }

        val resultadoFinal = when {
            veredicto.resultado == MatchResult.COINCIDE && !reclamado ->
                // Otro cajero lo reclamo en el medio.
                veredicto.copy(
                    resultado = MatchResult.YA_RECLAMADO,
                    explicacion = "Ese pago ya fue verificado por otro comprobante.",
                )
            else -> veredicto
        }

        guardar(comprobanteId, textoOcr, lectura.amountCents, lectura.reference, resultadoFinal, reclamado, ahoraMillis)

        return ResultadoDeVerificacion(resultadoFinal, reclamado)
    }

    /**
     * Reclama el pago que eligio la persona cuando habia varios candidatos.
     *
     * Sigue pasando por la condicion de la base: entre que se mostro la lista y
     * se toco la opcion, otro cajero pudo reclamarlo.
     */
    suspend fun reclamarElegido(pago: Payment, ahoraMillis: Long): ResultadoDeVerificacion {
        val comprobanteId = UUID.randomUUID().toString()
        val reclamado = pagoDao.reclamarSiEstaLibre(pago.id, comprobanteId) == 1

        val veredicto = if (reclamado) {
            Veredicto(
                resultado = MatchResult.COINCIDE,
                pago = pago,
                explicacion = "Verificado a mano por el cajero.",
            )
        } else {
            Veredicto(
                resultado = MatchResult.YA_RECLAMADO,
                pago = pago,
                explicacion = "Ese pago ya fue verificado por otro comprobante.",
            )
        }

        guardar(comprobanteId, "", pago.amountCents, pago.reference, veredicto, reclamado, ahoraMillis)
        return ResultadoDeVerificacion(veredicto, reclamado)
    }

    fun ultimos(cuantos: Int): Flow<List<Receipt>> =
        comprobanteDao.ultimos(cuantos).map { lista -> lista.map { it.aDominio() } }

    private suspend fun guardar(
        id: String,
        textoOcr: String,
        monto: Long?,
        referencia: String?,
        veredicto: Veredicto,
        reclamado: Boolean,
        ahoraMillis: Long,
    ) {
        comprobanteDao.guardar(
            Receipt(
                id = id,
                capturedAtMillis = ahoraMillis,
                // El texto del OCR se guarda acotado: sirve para entender un caso
                // dudoso, pero puede traer el nombre del cliente.
                textoOcr = textoOcr.take(MAX_TEXTO),
                amountCents = monto,
                reference = referencia,
                matchedPaymentId = veredicto.pago?.id?.takeIf { reclamado },
                resultado = veredicto.resultado,
            ).aEntity(),
        )
    }

    private companion object {
        const val MAX_TEXTO = 2_000
    }
}
