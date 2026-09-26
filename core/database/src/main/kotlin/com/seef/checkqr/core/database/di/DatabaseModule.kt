package com.seef.checkqr.core.database.di

import android.content.Context
import androidx.room.Room
import com.seef.checkqr.core.database.CheckQrDatabase
import com.seef.checkqr.core.database.dao.ComprobanteDao
import com.seef.checkqr.core.database.dao.DiagnosticoDao
import com.seef.checkqr.core.database.dao.MiembroDao
import com.seef.checkqr.core.database.dao.PagoDao
import com.seef.checkqr.core.database.dao.TurnoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun base(@ApplicationContext context: Context): CheckQrDatabase =
        Room.databaseBuilder(context, CheckQrDatabase::class.java, CheckQrDatabase.NOMBRE)
            // Sin fallbackToDestructiveMigration a proposito: perder los pagos
            // de un comerciante por una migracion no escrita es inaceptable.
            // Cada cambio de esquema tiene que traer su migracion.
            .build()

    @Provides fun pagoDao(db: CheckQrDatabase): PagoDao = db.pagoDao()
    @Provides fun turnoDao(db: CheckQrDatabase): TurnoDao = db.turnoDao()
    @Provides fun miembroDao(db: CheckQrDatabase): MiembroDao = db.miembroDao()
    @Provides fun comprobanteDao(db: CheckQrDatabase): ComprobanteDao = db.comprobanteDao()
    @Provides fun diagnosticoDao(db: CheckQrDatabase): DiagnosticoDao = db.diagnosticoDao()
}
