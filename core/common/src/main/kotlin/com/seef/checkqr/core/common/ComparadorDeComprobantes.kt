package com.seef.checkqr.core.common

import com.seef.checkqr.core.model.MatchResult
import com.seef.checkqr.core.model.Payment

/**
 * Lo que se logro leer de la captura de pantalla del cliente.
 */
data class LecturaDeComprobante(
    val textoCompleto: String,
    val amountCents: Long?,
    val reference: String?,
) {
    val sirve: Boolean get() = amountCents != null || reference != null
}

/** Resultado de comparar la lectura contra los pagos recibidos. */
data class Veredicto(
    val resultado: MatchResult,
    /** El pago que casa, si hay exactamente uno. */
    val pago: Payment?,
    /** Todos los que casaban, para poder mostrarlos si hay ambiguedad. */
    val candidatos: List<Payment> = emptyList(),
    val explicacion: String,
)

/**
 * Compara el comprobante que muestra el cliente contra los pagos que si
 * llegaron.
 *
 * Es la respuesta a la pregunta del mostrador: "me dice que ya pagó, ¿es
 * cierto?". Por eso el sesgo del diseno es no afirmar de mas: ante duda, se
 * devuelve [MatchResult.AMBIGUO] y decide una persona. Un falso "sí, llegó" le
 * cuesta la mercaderia al comerciante.
 *
 * Kotlin puro y sin dependencias de Android para que se pueda probar entero en
 * la JVM.
 */
object ComparadorDeComprobantes {

    /**
     * Ventana de tiempo para buscar el pago.
     *
     * Media hora: el cliente puede tardar en mostrar la captura, y el reloj del
     * celular del banco no siempre coincide con el nuestro. Mas ancha
     * empezaria a juntar cobros de montos repetidos de distintos clientes.
     */
    const val VENTANA_MILLIS: Long = 30L * 60L * 1_000L

    /**
     * Extrae monto y referencia del texto que devolvio el OCR.
     *
     * Se reutiliza el mismo lector de importes que el parser de avisos, para que
     * "Bs 1.234,56" signifique lo mismo en los dos caminos. Si no, un
     * comprobante correcto podria no casar con su propio pago.
     */
    fun leer(textoOcr: String): LecturaDeComprobante {
        // Primero los importes marcados con "Bs". Es imprescindible: un OCR de
        // una captura trae la fecha ("10 mar 2026"), el telefono y el numero de
        // operacion, que son numeros validos y a veces mayores que el cobro.
        // Sin filtrar por el simbolo, el ano 2026 gana sobre un pago de Bs 50.
        val conSimbolo = Dinero.importesConSimbolo(textoOcr)

        // Entre los marcados se toma el mayor: las capturas suelen mostrar
        // tambien la comision y el saldo, y el cobro es el numero grande.
        val monto = conSimbolo.maxOrNull()
            // Si ninguno trae simbolo, se cae a cualquier numero. Es peor senal,
            // pero es mejor ofrecer una comparacion dudosa que ninguna.
            ?: Dinero.importes(textoOcr).maxOrNull()

        val referencia = REFERENCIA.find(textoOcr)
            ?.groups?.get("ref")?.value
            ?.replace(" ", "")
            ?.uppercase()

        return LecturaDeComprobante(textoOcr, monto, referencia)
    }

    /**
     * Elige el pago que corresponde al comprobante.
     *
     * @param candidatos pagos **sin reclamar** dentro de la ventana. Que ya
     *   vengan filtrados por la base es importante: es lo que impide que dos
     *   cajeros cobren el mismo aviso.
     */
    fun comparar(
        lectura: LecturaDeComprobante,
        candidatos: List<Payment>,
        ahoraMillis: Long,
    ): Veredicto {
        if (!lectura.sirve) {
            return Veredicto(
                resultado = MatchResult.AMBIGUO,
                pago = null,
                explicacion = "No se pudo leer el monto de la captura. Prueba con mejor luz " +
                    "o escribe el monto a mano.",
            )
        }

        val enVentana = candidatos.filter {
            ahoraMillis - it.notifPostedAtMillis in 0..VENTANA_MILLIS
        }

        // La referencia, cuando existe, es la senal mas fuerte: identifica la
        // transaccion y no depende de que dos clientes paguen lo mismo.
        if (lectura.reference != null) {
            val porReferencia = enVentana.filter { it.reference == lectura.reference }
            if (porReferencia.size == 1) {
                return Veredicto(
                    resultado = MatchResult.COINCIDE,
                    pago = porReferencia.single(),
                    candidatos = porReferencia,
                    explicacion = "Coincide la referencia ${lectura.reference}.",
                )
            }
        }

        if (lectura.amountCents == null) {
            return Veredicto(
                resultado = MatchResult.AMBIGUO,
                pago = null,
                explicacion = "Se leyó una referencia pero no el monto, y ningún pago " +
                    "tiene esa referencia.",
            )
        }

        val porMonto = enVentana.filter { it.amountCents == lectura.amountCents }

        return when (porMonto.size) {
            0 -> Veredicto(
                resultado = MatchResult.NO_LLEGO,
                pago = null,
                explicacion = "No llegó ningún pago de ${Dinero.formatear(lectura.amountCents)} " +
                    "en los últimos 30 minutos.",
            )

            1 -> Veredicto(
                resultado = MatchResult.COINCIDE,
                pago = porMonto.single(),
                candidatos = porMonto,
                explicacion = "Coincide el monto ${Dinero.formatear(lectura.amountCents)}.",
            )

            // Varios del mismo monto: no se elige por el agente. Decide la
            // persona, que puede mirar el nombre del pagador y la hora.
            else -> Veredicto(
                resultado = MatchResult.AMBIGUO,
                pago = null,
                candidatos = porMonto,
                explicacion = "Hay ${porMonto.size} pagos de " +
                    "${Dinero.formatear(lectura.amountCents)} sin verificar. " +
                    "Elige cuál corresponde.",
            )
        }
    }

    /**
     * Patron de la referencia dentro del texto del OCR.
     *
     * Se exige una etiqueta ("ref", "número de operación", ...) y no cualquier
     * cadena alfanumerica: sin eso, agarraria el numero de telefono o la fecha.
     */
    private val REFERENCIA = Regex(
        """(?i)(?:ref(?:erencia)?|n[uú]m(?:ero)?\.?\s*(?:de\s*)?(?:operaci[oó]n|transacci[oó]n)|c[oó]digo\s*de\s*operaci[oó]n)""" +
            """\s*[:.#]?\s*(?<ref>[A-Za-z0-9][A-Za-z0-9 -]{3,29})""",
    )
}
