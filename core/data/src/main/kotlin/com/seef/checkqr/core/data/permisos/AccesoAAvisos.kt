package com.seef.checkqr.core.data.permisos

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprobar y pedir el acceso a las notificaciones.
 *
 * Vive aqui y no en :capture:listener para que el estado del sistema lo pueda
 * consultar sin depender del modulo del servicio. Le alcanza con el nombre del
 * paquete: el ajuste del sistema lista componentes, y basta con que alguno sea
 * nuestro.
 *
 * No existe API para pedirlo con un dialogo: solo se puede llevar al usuario a
 * la pantalla de ajustes y despues verificar si quedo concedido. Por eso el
 * onboarding explica primero para que sirve y recien despues abre los ajustes.
 */
@Singleton
class AccesoAAvisos @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Cierto si el usuario concedio el acceso.
     *
     * Es la fuente de verdad del estado "esta escuchando", y no el latido que
     * escribe `onListenerConnected`: ese callback solo se dispara al conectar, y
     * si el servicio ya estaba conectado cuando la app arranca, no vuelve a
     * llegar. Basarse solo en el latido produce una falsa alarma de "dejó de
     * escuchar" justo cuando todo funciona, y una falsa alarma en el indicador
     * mas importante ensena al comerciante a ignorarlo.
     */
    fun concedido(): Boolean {
        val habilitados = Settings.Secure.getString(
            context.contentResolver,
            AJUSTE_LISTENERS,
        ) ?: return false

        return habilitados.split(':')
            .mapNotNull(ComponentName::unflattenFromString)
            .any { it.packageName == context.packageName }
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

    private companion object {
        const val AJUSTE_LISTENERS = "enabled_notification_listeners"
    }
}
