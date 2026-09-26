package com.seef.checkqr.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    private const val ARCHIVO = "checkqr_preferencias"

    @Provides
    @Singleton
    fun preferencias(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(ARCHIVO) },
        )

    private fun Context.preferencesDataStoreFile(nombre: String) =
        java.io.File(applicationContext.filesDir, "datastore/$nombre.preferences_pb")
}
