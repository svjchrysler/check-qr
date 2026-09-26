package com.seef.checkqr.core.data.di

import com.seef.checkqr.core.common.Reloj
import com.seef.checkqr.core.data.RelojDelSistema
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun reloj(impl: RelojDelSistema): Reloj
}
