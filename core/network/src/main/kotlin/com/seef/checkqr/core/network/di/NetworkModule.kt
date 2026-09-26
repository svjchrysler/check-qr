package com.seef.checkqr.core.network.di

import com.seef.checkqr.capture.parser.VerificadorDePlantillas
import com.seef.checkqr.core.network.BuildConfig
import com.seef.checkqr.core.network.plantillas.ClavePublicaDePlantillas
import com.seef.checkqr.core.network.plantillas.VerificadorEd25519
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
    }
}
