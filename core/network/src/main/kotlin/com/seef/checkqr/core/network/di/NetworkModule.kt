package com.seef.checkqr.core.network.di

import com.seef.checkqr.capture.parser.VerificadorDePlantillas
import com.seef.checkqr.core.network.BuildConfig
import com.seef.checkqr.core.network.api.ApiDeCheckQr
import com.seef.checkqr.core.network.api.InterceptorDeCredencial
import com.seef.checkqr.core.network.api.ProveedorDeCredencial
import com.seef.checkqr.core.network.api.SinCredencial
import com.seef.checkqr.core.network.plantillas.ClavePublicaDePlantillas
import com.seef.checkqr.core.network.plantillas.VerificadorEd25519
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun verificadorDePlantillas(impl: VerificadorEd25519): VerificadorDePlantillas

    /**
     * Sin sesion por omision.
     *
     * Es deliberado que la app funcione entera sin backend: la captura, la voz y
     * el cuadre son locales. :feature:equipo reemplazara este enlace cuando haya
     * inicio de sesion con Google.
     */
    @Binds
    @Singleton
    abstract fun proveedorDeCredencial(impl: SinCredencial): ProveedorDeCredencial

    companion object {
        /**
         * Clave publica con la que se verifican las plantillas firmadas.
         *
         * Va fijada en el binario, no se descarga: si viniera de la red, quien
         * pudiera cambiar las plantillas podria cambiar tambien la clave y la
         * verificacion no protegeria de nada.
         *
         * PENDIENTE: reemplazar por la clave real cuando exista el par de claves
         * del backend (la privada en Secret Manager). Mientras este vacia, el
         * verificador rechaza todo y la app se queda con las plantillas
         * empaquetadas, que es el comportamiento seguro.
         */
        @Provides
        @Singleton
        @ClavePublicaDePlantillas
        fun clavePublica(): String = BuildConfig.CLAVE_PUBLICA_PLANTILLAS

        @Provides
        @Singleton
        fun json(): Json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

        @Provides
        @Singleton
        fun okhttp(credencial: InterceptorDeCredencial): OkHttpClient =
            OkHttpClient.Builder()
                .addInterceptor(credencial)
                // Tiempos cortos: el celular esta en un mostrador con red mala, y
                // una peticion colgada un minuto no ayuda a nadie. WorkManager la
                // reintenta con backoff.
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                // Sin pinning de certificado a proposito: con targetSdk 37 la
                // transparencia de certificados va activada por omision y el
                // backend usa una autoridad publica. Pinnear ademas solo agrega
                // el riesgo de dejar la flota sin sincronizar al rotar el cert.
                .build()

        @Provides
        @Singleton
        fun retrofit(cliente: OkHttpClient, json: Json): Retrofit =
            Retrofit.Builder()
                .baseUrl(BuildConfig.URL_BASE)
                .client(cliente)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()

        @Provides
        @Singleton
        fun api(retrofit: Retrofit): ApiDeCheckQr = retrofit.create(ApiDeCheckQr::class.java)
    }
}
