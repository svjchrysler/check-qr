package com.seef.checkqr.capture.listener.di

import com.seef.checkqr.capture.listener.RegistradorCrudo
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Variante `release`: se enlaza el registrador que no guarda nada. La clase que
 * si graba vive en `src/debug` y no se compila aqui.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RegistradorModule {
    @Binds
    abstract fun registrador(impl: RegistradorNoOp): RegistradorCrudo
}
