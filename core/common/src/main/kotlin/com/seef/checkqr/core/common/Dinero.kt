package com.seef.checkqr.core.common

import com.seef.checkqr.core.model.AmountFormat

/**
 * Lectura y escritura de importes en bolivianos.
 *
 * Todo se maneja en centavos enteros. Ningun importe de la app pasa por Float ni
 * por Double: un pago de Bs 0,10 mal redondeado es un reclamo del comerciante.
 *
 * Regla de seguridad: ante un texto ambiguo o que contradice el formato
 * declarado por la plantilla, [aCentavos] devuelve null en vez de adivinar.
 * Perder un aviso es visible y se corrige; leer Bs 1,50 como Bs 150 no.
 */
object Dinero {

    /** Lo que un aviso puede poner delante del numero. */
    private val PREFIJO_MONEDA = Regex("""(?i)\bBs\.?\s*""")

    /** Un importe suelto dentro de un texto libre. */
    private val NUMERO = Regex("""\d{1,3}(?:[.,]\d{3})+(?:[.,]\d{1,2})?|\d+(?:[.,]\d{1,2})?""")

    /**
     * Un importe que viene marcado con "Bs".
     *
     * Distinguirlo importa: un texto suelto trae anos, telefonos y numeros de
     * operacion que son numeros perfectamente validos y a veces mas grandes que
     * el cobro. El simbolo de moneda es la unica senal fiable de que un numero
     * es dinero.
     */
    private val NUMERO_CON_SIMBOLO = Regex(
        """(?i)\bBs\.?\s*(\d{1,3}(?:[.,]\d{3})+(?:[.,]\d{1,2})?|\d+(?:[.,]\d{1,2})?)""",
    )

    /** Que papel juega cada separador dentro del numero. */
    private sealed interface Separadores {
        /** Numero entero: no hay parte decimal. */
        data object SinDecimales : Separadores

        /** [decimal] separa los centavos; el otro caracter son miles. */
        data class ConDecimal(val decimal: Char) : Separadores

        /** No se puede leer sin adivinar. */
        data object Ambiguo : Separadores
    }

    /**
     * Lee un importe suelto, p. ej. `"1.234,56"`, `"1,234.56"`, `"50"`.
     *
     * @return centavos, o null si no se puede leer sin ambiguedad.
     */
    fun aCentavos(texto: String, formato: AmountFormat = AmountFormat.AUTO): Long? {
        val limpio = texto.trim()
            .replace(PREFIJO_MONEDA, "")
            .filterNot { it == ' ' || it == ' ' }
        if (limpio.isEmpty()) return null
        if (!limpio.all { it.isDigit() || it == '.' || it == ',' }) return null
        if (limpio.none(Char::isDigit)) return null

        val enteraTxt: String
        val decimalTxt: String
        when (val sep = clasificar(limpio, formato)) {
            Separadores.Ambiguo -> return null
            Separadores.SinDecimales -> {
                enteraTxt = limpio
                decimalTxt = ""
            }
            is Separadores.ConDecimal -> {
                val i = limpio.lastIndexOf(sep.decimal)
                enteraTxt = limpio.substring(0, i)
                decimalTxt = limpio.substring(i + 1)
            }
        }

        if (decimalTxt.length > 2) return null
        if (decimalTxt.any { !it.isDigit() }) return null

        // Lo que quede de separadores en la parte entera son miles.
        val entera = enteraTxt.filter(Char::isDigit)
        val bolivianos = entera.ifEmpty { "0" }.toLongOrNull() ?: return null
        val centavos = decimalTxt.padEnd(2, '0').ifEmpty { "00" }.toLongOrNull() ?: return null

        return bolivianos * 100 + centavos
    }

    private fun clasificar(limpio: String, formato: AmountFormat): Separadores {
        val puntos = limpio.count { it == '.' }
        val comas = limpio.count { it == ',' }

        if (puntos == 0 && comas == 0) return Separadores.SinDecimales

        // Con los dos caracteres presentes no hay ambiguedad: el ultimo es el
        // decimal. "1.234,56" y "1,234.56" son ambos mil doscientos treinta y
        // cuatro con cincuenta y seis.
        if (puntos > 0 && comas > 0) {
            val decimal = if (limpio.lastIndexOf('.') > limpio.lastIndexOf(',')) '.' else ','
            return conDecimalSiCoincide(decimal, formato)
        }

        val sep = if (puntos > 0) '.' else ','
        val ocurrencias = if (puntos > 0) puntos else comas
        val digitosDespues = limpio.length - limpio.lastIndexOf(sep) - 1

        return when {
            // "1.234.567": repetido solo puede ser separador de miles.
            ocurrencias > 1 -> Separadores.SinDecimales

            // "1.234": tres digitos detras son miles en los dos formatos,
            // porque los centavos nunca son tres cifras.
            digitosDespues == 3 -> Separadores.SinDecimales

            // "50,00" / "1.50": uno o dos digitos detras son centavos, siempre
            // que el caracter sea el que la plantilla declara como decimal.
            digitosDespues in 1..2 -> conDecimalSiCoincide(sep, formato)

            // "1.2345" no es un importe.
            else -> Separadores.Ambiguo
        }
    }

    /**
     * Acepta [decimal] como separador de centavos solo si no contradice el
     * formato declarado por la plantilla del banco.
     */
    private fun conDecimalSiCoincide(decimal: Char, formato: AmountFormat): Separadores =
        when (formato) {
            AmountFormat.AUTO -> Separadores.ConDecimal(decimal)
            AmountFormat.COMA_DECIMAL ->
                if (decimal == ',') Separadores.ConDecimal(decimal) else Separadores.Ambiguo
            AmountFormat.PUNTO_DECIMAL ->
                if (decimal == '.') Separadores.ConDecimal(decimal) else Separadores.Ambiguo
        }

    /** Busca el primer importe legible dentro de un texto libre. */
    fun primerImporte(texto: String, formato: AmountFormat = AmountFormat.AUTO): Long? =
        NUMERO.findAll(texto)
            .mapNotNull { aCentavos(it.value, formato) }
            .firstOrNull()

    /** Todos los importes legibles del texto, en orden de aparicion. */
    fun importes(texto: String, formato: AmountFormat = AmountFormat.AUTO): List<Long> =
        NUMERO.findAll(texto)
            .mapNotNull { aCentavos(it.value, formato) }
            .toList()

    /**
     * Solo los importes que vienen marcados con "Bs".
     *
     * Es lo que hay que usar sobre texto libre poco fiable, como el OCR de una
     * captura de pantalla: sin el simbolo, la fecha "10 mar 2026" aporta un
     * 2026 que se confundiria con Bs 20,26.
     */
    fun importesConSimbolo(texto: String, formato: AmountFormat = AmountFormat.AUTO): List<Long> =
        NUMERO_CON_SIMBOLO.findAll(texto)
            .mapNotNull { aCentavos(it.groupValues[1], formato) }
            .toList()

    /** Formato para pantalla, al estilo boliviano: `Bs 1.234,56`. */
    fun formatear(centavos: Long, conSimbolo: Boolean = true): String {
        val signo = if (centavos < 0) "-" else ""
        val abs = if (centavos < 0) -centavos else centavos
        val conMiles = (abs / 100).toString()
            .reversed()
            .chunked(3)
            .joinToString(".")
            .reversed()
        val cuerpo = "$signo$conMiles,${(abs % 100).toString().padStart(2, '0')}"
        return if (conSimbolo) "Bs $cuerpo" else cuerpo
    }
}
