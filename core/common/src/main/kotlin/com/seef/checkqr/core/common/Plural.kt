package com.seef.checkqr.core.common

/**
 * Concordancia de numero para el texto que ve el usuario.
 *
 * Existe porque interpolar `"$n pagos"` produce "1 pagos", y una app que escribe
 * mal delante del comerciante pierde credibilidad justo donde mas la necesita:
 * en las pantallas que le dicen cuanto dinero tiene.
 */
object Plural {

    /** `2, "pago", "pagos"` -> `"2 pagos"`; `1, ...` -> `"1 pago"`. */
    fun contar(cantidad: Int, singular: String, plural: String): String =
        "$cantidad ${palabra(cantidad, singular, plural)}"

    /** Solo la palabra, para cuando el numero va en otro sitio de la frase. */
    fun palabra(cantidad: Int, singular: String, plural: String): String =
        if (cantidad == 1) singular else plural

    /** Atajo para el caso mas frecuente de la app. */
    fun pagos(cantidad: Int): String = contar(cantidad, "pago", "pagos")
}
