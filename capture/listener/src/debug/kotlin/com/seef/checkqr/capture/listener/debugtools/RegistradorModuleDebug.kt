package com.seef.checkqr.capture.listener.di

import com.seef.checkqr.capture.listener.RegistradorCrudo
import com.seef.checkqr.capture.listener.debugtools.RegistradorDeDepuracion
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Variante `debug`: se enlaza el registrador que graba todo, para poder
 * descubrir los paquetes y textos reales de los bancos.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RegistradorModule {
    @Binds
    abstract fun registrador(impl: RegistradorDeDepuracion): RegistradorCrudo
}
