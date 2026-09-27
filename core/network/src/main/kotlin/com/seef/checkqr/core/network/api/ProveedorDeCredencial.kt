package com.seef.checkqr.core.network.api

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * De donde sale el token de sesion.
 *
 * Se declara como interfaz para que :core:network no tenga que saber como se
 * inicia sesion: eso vive en :feature:equipo, que es quien habla con Credential
 * Manager.
 */
interface ProveedorDeCredencial {
    /** El token vigente, o null si todavia no hay sesion. */
    suspend fun token(): String?
}

/** Por omision, sin sesion: el sincronizado queda inactivo y la caja local sigue. */
@Singleton
class SinCredencial @Inject constructor() : ProveedorDeCredencial {
    override suspend fun token(): String? = null
}

/**
 * Anade la credencial a cada peticion.
 *
 * `runBlocking` dentro de un interceptor de OkHttp es correcto aqui: el
 * interceptor ya corre en el hilo de red de OkHttp, nunca en el principal.
 */
@Singleton
class InterceptorDeCredencial @Inject constructor(
    private val proveedor: ProveedorDeCredencial,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { proveedor.token() }
            ?: return chain.proceed(chain.request())

        val conCredencial = chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(conCredencial)
    }
}
