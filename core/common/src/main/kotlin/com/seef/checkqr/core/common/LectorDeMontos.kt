package com.seef.checkqr.core.common

/**
 * Convierte un importe en centavos al texto que se le pasa al sintetizador.
 *
 * El numero se escribe en palabras en lugar de dejarselo al motor de voz: un
 * motor cualquiera lee "1.234,56" de formas distintas segun la version y el
 * idioma instalado, y el comerciante necesita oir siempre lo mismo. Ademas asi
 * el anuncio no depende de que voz este instalada, que es parte del requisito
 * de funcionar sin internet.
 */
object LectorDeMontos {

    private val UNIDADES = arrayOf(
        "cero", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho",
        "nueve", "diez", "once", "doce", "trece", "catorce", "quince",
        "dieciséis", "diecisiete", "dieciocho", "diecinueve", "veinte",
        "veintiuno", "veintidós", "veintitrés", "veinticuatro", "veinticinco",
        "veintiséis", "veintisiete", "veintiocho", "veintinueve",
    )

    private val DECENAS = arrayOf(
        "", "", "", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta",
        "ochenta", "noventa",
    )

    private val CENTENAS = arrayOf(
        "", "ciento", "doscientos", "trescientos", "cuatrocientos", "quinientos",
        "seiscientos", "setecientos", "ochocientos", "novecientos",
    )

    /** Tope de lo que se sabe leer: mil millones. */
    private const val MAXIMO = 999_999_999L

    /**
     * El anuncio completo, p. ej.
     * `"Recibiste cincuenta bolivianos de Juan Pérez por Yape"`.
     *
     * @param pagador nombre del pagador, o null si el aviso no lo trae.
     * @param billetera nombre visible de la billetera.
     */
    fun anuncioDePago(centavos: Long, pagador: String?, billetera: String): String =
        buildString {
            append("Recibiste ")
            append(enPalabras(centavos))
            if (!pagador.isNullOrBlank()) {
                append(" de ")
                append(pagador.trim())
            }
            append(" por ")
            append(billetera)
        }

    /**
     * El importe en palabras: `5000` -> `"cincuenta bolivianos"`,
     * `150` -> `"un boliviano con cincuenta centavos"`.
     */
    fun enPalabras(centavos: Long): String {
        require(centavos >= 0) { "Un pago no puede ser negativo: $centavos" }

        val bolivianos = centavos / 100
        val resto = centavos % 100

        val parteEntera = when {
            bolivianos > MAXIMO -> bolivianos.toString() // fuera de rango: digitos
            bolivianos == 1L -> "un boliviano"
            else -> "${numero(bolivianos, apocopar = true)} bolivianos"
        }

        return when {
            resto == 0L -> parteEntera
            resto == 1L -> "$parteEntera con un centavo"
            else -> "$parteEntera con ${numero(resto, apocopar = true)} centavos"
        }
    }

    /**
     * Un entero en palabras.
     *
     * @param apocopar si true, "uno" se vuelve "un" y "veintiuno" "veintiún"
     *   cuando van delante de un sustantivo, como pide el espanol.
     */
    fun numero(n: Long, apocopar: Boolean = false): String {
        require(n >= 0) { "No se leen numeros negativos: $n" }
        if (n > MAXIMO) return n.toString()
        val palabras = construir(n)
        return if (apocopar) apocope(palabras) else palabras
    }

    private fun construir(n: Long): String = when {
        n < 30 -> UNIDADES[n.toInt()]
        n < 100 -> decenas(n.toInt())
        n < 1_000 -> centenas(n.toInt())
        n < 1_000_000 -> miles(n)
        else -> millones(n)
    }

    private fun decenas(n: Int): String {
        val d = n / 10
        val u = n % 10
        return if (u == 0) DECENAS[d] else "${DECENAS[d]} y ${UNIDADES[u]}"
    }

    private fun centenas(n: Int): String {
        if (n == 100) return "cien"
        val c = n / 100
        val resto = n % 100
        return if (resto == 0) CENTENAS[c] else "${CENTENAS[c]} ${construir(resto.toLong())}"
    }

    private fun miles(n: Long): String {
        val millares = n / 1_000
        val resto = n % 1_000
        // "mil", no "uno mil"; y "veintiún mil", no "veintiuno mil".
        val prefijo = if (millares == 1L) "mil" else "${apocope(construir(millares))} mil"
        return if (resto == 0L) prefijo else "$prefijo ${construir(resto)}"
    }

    private fun millones(n: Long): String {
        val mill = n / 1_000_000
        val resto = n % 1_000_000
        val prefijo = if (mill == 1L) "un millón" else "${apocope(construir(mill))} millones"
        return if (resto == 0L) prefijo else "$prefijo ${construir(resto)}"
    }

    /** "uno" -> "un", "veintiuno" -> "veintiún", tambien al final de un compuesto. */
    private fun apocope(palabras: String): String = when {
        palabras == "uno" -> "un"
        palabras == "veintiuno" -> "veintiún"
        palabras.endsWith(" uno") -> palabras.dropLast(4) + " un"
        palabras.endsWith(" veintiuno") -> palabras.dropLast(10) + " veintiún"
        else -> palabras
    }
}
