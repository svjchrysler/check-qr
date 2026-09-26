package com.seef.checkqr.capture.listener

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Comprobar y pedir el acceso a notificaciones.
 *
 * No hay API para solicitarlo con un dialogo: solo se puede llevar al usuario a
 * la pantalla de ajustes y despues verificar si quedo concedido. Por eso el
 * onboarding explica primero para que sirve y recien despues abre los ajustes.
 */
object AccesoAAvisos {

    /** Cierto si el usuario ya concedio el acceso a este servicio. */
    fun concedido(context: Context): Boolean {
        val propio = ComponentName(context, ServicioDeAvisosBancarios::class.java)
        val habilitados = Settings.Secure.getString(
            context.contentResolver,
            AJUSTE_LISTENERS,
        ) ?: return false

        return habilitados.split(':')
            .mapNotNull { ComponentName.unflattenFromString(it) }
            .any { it.packageName == propio.packageName }
    }

    /**
     * Intent a los ajustes de acceso a notificaciones.
     *
     * Se usa el listado general y no el de la app concreta: el intent especifico
     * no existe en todas las versiones ni en todas las marcas, y aterrizar en una
     * pantalla vacia es peor que aterrizar en la lista.
     */
    fun intentDeAjustes(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private const val AJUSTE_LISTENERS = "enabled_notification_listeners"
}
