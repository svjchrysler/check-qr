package com.seef.checkqr.core.model

/**
 * Un aviso de notificacion tal como llego, ya filtrado por la lista blanca de
 * paquetes. Es el unico punto de contacto entre el listener de Android y el
 * parser, que es Kotlin puro: por eso aqui no hay ningun tipo de Android.
 */
data class RawNotice(
    /** Paquete de la app que emitio el aviso, p. ej. "com.bcp.innovacxion.yapeapp". */
    val sourcePackage: String,
    val titulo: String?,
    val texto: String?,
    /** `EXTRA_BIG_TEXT`: muchos bancos ponen el detalle completo solo aqui. */
    val textoLargo: String?,
    val subtexto: String?,
    /** `EXTRA_TEXT_LINES`, para los avisos agrupados. */
    val lineas: List<String> = emptyList(),
    /** `StatusBarNotification.postTime`, en milisegundos epoch. */
    val postedAtMillis: Long,
    /** Cuando lo vio la app. Puede ser bastante posterior si el celular dormia. */
    val capturedAtMillis: Long,
) {
    /**
     * Todo el texto del aviso, en el orden en que conviene buscar: el detalle
     * largo primero, porque es donde suele estar el monto y el pagador.
     */
    val textoCompleto: String
        get() = buildList {
            textoLargo?.let(::add)
            texto?.let(::add)
            titulo?.let(::add)
            subtexto?.let(::add)
            addAll(lineas)
        }.filter { it.isNotBlank() }.joinToString("\n")

    /**
     * Cierto si el aviso llego sin nada legible.
     *
     * Desde Android 15 el sistema puede ocultar el contenido de los avisos con
     * codigos OTP a los listeners no confiables, y a veces alcanza a avisos que
     * no lo son. Cuando pasa, no es un fallo del parser: la app tiene que
     * decirle al usuario "Revisa tu app del banco" en lugar de callarse.
     */
    val contenidoOculto: Boolean
        get() = textoCompleto.isBlank()
}
