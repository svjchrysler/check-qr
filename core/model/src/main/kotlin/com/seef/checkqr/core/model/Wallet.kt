package com.seef.checkqr.core.model

import kotlinx.serialization.Serializable

/**
 * Las 6 billeteras del lanzamiento. El `id` es estable y se persiste en la base
 * y viaja al backend, asi que no se renombra nunca; el nombre visible si.
 */
@Serializable
enum class Wallet(val id: String, val nombreVisible: String) {
    YAPE("yape", "Yape"),
    BCP("bcp", "BCP"),
    BNB("bnb", "BNB"),
    MERCANTIL("mercantil", "Mercantil Santa Cruz"),
    UNION("union", "Banco Unión"),
    TIGO_MONEY("tigo_money", "Tigo Money"),

    /** Aviso de un paquete de la lista blanca que ninguna plantilla reconocio. */
    DESCONOCIDA("desconocida", "Desconocida"),
    ;

    companion object {
        private val porId = entries.associateBy(Wallet::id)

        fun porId(id: String): Wallet? = porId[id]

        /** Las que se capturan en el plan Basico, sin la pseudo-billetera. */
        val soportadas: List<Wallet> = entries.filter { it != DESCONOCIDA }
    }
}
