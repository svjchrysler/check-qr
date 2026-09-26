package com.seef.checkqr.capture.listener

import com.seef.checkqr.core.model.RawNotice

/**
 * Volcado de avisos para descubrir los datos reales de los bancos.
 *
 * Existe en dos implementaciones separadas por variante, y no como un `if
 * (BuildConfig.DEBUG)`, a proposito: en `release` la clase que graba
 * **no esta en el binario**. La garantia de que la app no registra las
 * notificaciones de otras apps es asi estructural y no depende de que nadie se
 * equivoque con una condicion.
 */
interface RegistradorCrudo {

    /** Cierto solo en la variante debug. */
    val activo: Boolean

    /**
     * Registra un aviso de cualquier app.
     *
     * @param enListaBlanca si el paquete estaba en la lista de bancos.
     */
    suspend fun registrar(notice: RawNotice, enListaBlanca: Boolean)
}
