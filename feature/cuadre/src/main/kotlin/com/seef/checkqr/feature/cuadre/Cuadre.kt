package com.seef.checkqr.feature.cuadre

import com.seef.checkqr.core.model.Payment
import com.seef.checkqr.core.model.Wallet
import kotlinx.datetime.LocalDate

/** Un renglon del cuadre. */
data class RenglonDeCuadre(
    val etiqueta: String,
    val cantidad: Int,
    val totalCentavos: Long,
)

/**
 * El cuadre de un dia, agrupado por las cuatro dimensiones que el comerciante
 * usa para cerrar caja.
 */
data class CuadreDelDia(
    val dia: LocalDate,
    val totalCentavos: Long,
    val cantidad: Int,
    val porBilletera: List<RenglonDeCuadre>,
    val porCajero: List<RenglonDeCuadre>,
    val porTurno: List<RenglonDeCuadre>,
    /**
     * Pagos que llegaron con la caja cerrada.
     *
     * Van aparte y no repartidos en un turno cualquiera: colgarlos del turno
     * equivocado haria que a un cajero no le cuadre su propio arqueo, que es
     * exactamente el problema que esta pantalla existe para evitar.
     */
    val sinTurno: RenglonDeCuadre,
    val pagos: List<Payment>,
) {
    val cuadra: Boolean
        get() = porBilletera.sumOf(RenglonDeCuadre::totalCentavos) == totalCentavos
}

/**
 * Arma el cuadre a partir de los pagos del dia.
 *
 * Kotlin puro: la aritmetica del cierre de caja es lo ultimo que deberia
 * necesitar un emulador para probarse.
 */
object ArmadorDeCuadre {

    fun armar(
        dia: LocalDate,
        pagos: List<Payment>,
        nombreDeCajero: (String) -> String = { it },
        nombreDeTurno: (String) -> String = { "Turno ${it.take(8)}" },
    ): CuadreDelDia {
        val total = pagos.sumOf(Payment::amountCents)

        val porBilletera = Wallet.soportadas
            .map { billetera ->
                val suyos = pagos.filter { it.wallet == billetera }
                RenglonDeCuadre(
                    etiqueta = billetera.nombreVisible,
                    cantidad = suyos.size,
                    totalCentavos = suyos.sumOf(Payment::amountCents),
                )
            }
            // Las billeteras sin movimiento no se listan: alargan el reporte sin
            // decir nada. Si una no aparece nunca, lo reporta Estado del sistema.
            .filter { it.cantidad > 0 }

        // La pseudo-billetera DESCONOCIDA no esta en `soportadas`, pero si hay
        // pagos con ella tienen que aparecer o el cuadre no sumaria.
        val desconocidos = pagos.filter { it.wallet == Wallet.DESCONOCIDA }
        val billeterasCompletas = if (desconocidos.isEmpty()) {
            porBilletera
        } else {
            porBilletera + RenglonDeCuadre(
                etiqueta = Wallet.DESCONOCIDA.nombreVisible,
                cantidad = desconocidos.size,
                totalCentavos = desconocidos.sumOf(Payment::amountCents),
            )
        }

        val porCajero = pagos
            .filter { it.cashierId != null }
            .groupBy { it.cashierId!! }
            .map { (id, suyos) ->
                RenglonDeCuadre(nombreDeCajero(id), suyos.size, suyos.sumOf(Payment::amountCents))
            }
            .sortedByDescending(RenglonDeCuadre::totalCentavos)

        val porTurno = pagos
            .filter { it.shiftId != null }
            .groupBy { it.shiftId!! }
            .map { (id, suyos) ->
                RenglonDeCuadre(nombreDeTurno(id), suyos.size, suyos.sumOf(Payment::amountCents))
            }
            .sortedByDescending(RenglonDeCuadre::totalCentavos)

        val huerfanos = pagos.filter { it.shiftId == null }

        return CuadreDelDia(
            dia = dia,
            totalCentavos = total,
            cantidad = pagos.size,
            porBilletera = billeterasCompletas,
            porCajero = porCajero,
            porTurno = porTurno,
            sinTurno = RenglonDeCuadre(
                etiqueta = "Con la caja cerrada",
                cantidad = huerfanos.size,
                totalCentavos = huerfanos.sumOf(Payment::amountCents),
            ),
            pagos = pagos.sortedByDescending(Payment::notifPostedAtMillis),
        )
    }
}
