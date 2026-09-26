package com.seef.checkqr.core.common

/**
 * El tiempo, inyectable.
 *
 * Existe porque casi todo lo delicado de esta app depende del reloj: la ventana
 * de deduplicacion, el dia del cuadre, la ventana con la que un comprobante casa
 * con un pago. Sin poder fijar el tiempo en los tests, esas reglas solo se
 * podrian comprobar a mano.
 *
 * La implementacion real vive en :core:data, porque necesita `SystemClock` y este
 * modulo es Kotlin puro.
 */
interface Reloj {
    /** Milisegundos epoch. Lo que se guarda en la base. */
    fun ahoraMillis(): Long

    /**
     * Milisegundos desde el arranque del sistema.
     *
     * Sirve para detectar un reinicio: si el valor bajo respecto de la ultima
     * vez, el celular se reinicio y la voz quedo apagada, porque Android 17 no
     * permite arrancar el servicio en primer plano sin una accion del usuario.
     */
    fun desdeElArranqueMillis(): Long
}

/** Reloj de prueba: el tiempo lo fija el test. */
class RelojFijo(
    var ahora: Long = 1_700_000_000_000L,
    var desdeArranque: Long = 60_000L,
) : Reloj {
    override fun ahoraMillis(): Long = ahora
    override fun desdeElArranqueMillis(): Long = desdeArranque

    fun avanzar(millis: Long) {
        ahora += millis
        desdeArranque += millis
    }
}
