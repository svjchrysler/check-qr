package com.seef.checkqr.feature.onboarding

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Guia al usuario a desactivar el ahorro de bateria agresivo de su marca.
 *
 * Esto existe porque varias marcas matan los servicios y los listeners de
 * notificaciones de las apps que consideran inactivas, y cuando lo hacen la app
 * deja de capturar pagos **sin ningun aviso**. Es la causa mas comun de "la app
 * dejó de funcionar" en el mercado boliviano, donde Xiaomi y los fabricantes
 * chinos tienen mucha presencia.
 *
 * Deliberadamente NO se usa `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: Play
 * restringe ese permiso a un puñado de casos y pedirlo es motivo de rechazo.
 * Se lleva al usuario a los ajustes y se le explica que tocar.
 */
object AjustesDeBateria {

    data class Guia(
        val marca: String,
        val pasos: List<String>,
        /**
         * Pantalla de ajustes propia de la marca. Puede no existir: por eso
         * siempre hay un camino de respaldo a los ajustes de la app.
         */
        val intentEspecifico: Intent?,
    )

    fun para(context: Context): Guia {
        val marca = Build.MANUFACTURER.lowercase()
        return when {
            marca.contains("xiaomi") || marca.contains("redmi") || marca.contains("poco") -> Guia(
                marca = "Xiaomi",
                pasos = listOf(
                    "Entra en Ajustes › Aplicaciones › CheckQr",
                    "Toca «Ahorro de batería» y elige «Sin restricciones»",
                    "Vuelve atrás y activa «Inicio automático»",
                    "En la pantalla de apps recientes, desliza CheckQr hacia abajo " +
                        "y tócale el candado",
                ),
                intentEspecifico = primeroQueExista(
                    context,
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity",
                ),
            )

            marca.contains("huawei") || marca.contains("honor") -> Guia(
                marca = "Huawei",
                pasos = listOf(
                    "Entra en Ajustes › Batería › Inicio de aplicaciones",
                    "Busca CheckQr y desactiva «Gestionar automáticamente»",
                    "Activa las tres opciones: inicio automático, inicio secundario " +
                        "y ejecución en segundo plano",
                ),
                intentEspecifico = primeroQueExista(
                    context,
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
                ),
            )

            marca.contains("oppo") || marca.contains("realme") -> Guia(
                marca = "Oppo",
                pasos = listOf(
                    "Entra en Ajustes › Batería › Uso de batería en segundo plano",
                    "Busca CheckQr y elige «Permitir actividad en segundo plano»",
                    "Vuelve a Ajustes › Gestión de aplicaciones › Inicio automático " +
                        "y actívalo para CheckQr",
                ),
                intentEspecifico = primeroQueExista(
                    context,
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity",
                ),
            )

            marca.contains("vivo") -> Guia(
                marca = "Vivo",
                pasos = listOf(
                    "Entra en Ajustes › Batería › Consumo de fondo de alta potencia",
                    "Activa CheckQr",
                    "Ve a iManager › Gestor de aplicaciones › Inicio automático " +
                        "y activa CheckQr",
                ),
                intentEspecifico = primeroQueExista(
                    context,
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
                ),
            )

            marca.contains("samsung") -> Guia(
                marca = "Samsung",
                pasos = listOf(
                    "Entra en Ajustes › Batería › Límites de uso en segundo plano",
                    "Comprueba que CheckQr NO esté en «Aplicaciones en suspensión»",
                    "En Ajustes › Aplicaciones › CheckQr › Batería, elige «Sin restricciones»",
                ),
                intentEspecifico = null,
            )

            marca.contains("motorola") || marca.contains("lenovo") -> Guia(
                marca = "Motorola",
                pasos = listOf(
                    "Entra en Ajustes › Batería › Optimización de batería",
                    "Busca CheckQr y elige «No optimizar»",
                ),
                intentEspecifico = null,
            )

            else -> Guia(
                marca = Build.MANUFACTURER,
                pasos = listOf(
                    "Entra en Ajustes › Aplicaciones › CheckQr › Batería",
                    "Elige la opción menos restrictiva («Sin restricciones» o «No optimizar»)",
                ),
                intentEspecifico = null,
            )
        }
    }

    /** Ajustes de la app. Siempre existe, y es el camino de respaldo. */
    fun intentDeAjustesDeLaApp(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    /**
     * Devuelve el intent solo si la actividad existe de verdad en el celular.
     *
     * Las rutas internas de cada marca cambian entre versiones, y lanzar una que
     * no existe revienta la app. Mejor caer a los ajustes generales.
     */
    private fun primeroQueExista(context: Context, paquete: String, clase: String): Intent? {
        val intent = Intent().apply {
            component = ComponentName(paquete, clase)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val hayQuienLoAtienda = context.packageManager
            .queryIntentActivities(intent, 0)
            .isNotEmpty()
        return intent.takeIf { hayQuienLoAtienda }
    }
}
