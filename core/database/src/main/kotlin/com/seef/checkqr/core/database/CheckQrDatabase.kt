package com.seef.checkqr.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.seef.checkqr.core.database.dao.ComprobanteDao
import com.seef.checkqr.core.database.dao.DiagnosticoDao
import com.seef.checkqr.core.database.dao.MiembroDao
import com.seef.checkqr.core.database.dao.PagoDao
import com.seef.checkqr.core.database.dao.TurnoDao
import com.seef.checkqr.core.database.entidades.AvisoNoReconocidoEntity
import com.seef.checkqr.core.database.entidades.CapturaCrudaDebugEntity
import com.seef.checkqr.core.database.entidades.ComprobanteEntity
import com.seef.checkqr.core.database.entidades.MiembroEntity
import com.seef.checkqr.core.database.entidades.PagoEntity
import com.seef.checkqr.core.database.entidades.SenalPorBancoEntity
import com.seef.checkqr.core.database.entidades.TurnoEntity

/**
 * La base local. Es la fuente de verdad de la app: el backend sincroniza, no
 * manda. Asi la caja sigue funcionando sin internet, que es el caso normal en un
 * mostrador.
 *
 * Los enums se guardan como su `id` de texto y no como ordinal: reordenar un
 * enum en el codigo no puede cambiar el significado de lo que ya esta escrito.
 *
 * No se exportan esquemas con `exportSchema = false` porque el plugin de Room ya
 * los escribe en `core/database/schemas`, que se commitean: son la base de las
 * migraciones y del test que verifica que el indice unico existe.
 */
@Database(
    entities = [
        PagoEntity::class,
        TurnoEntity::class,
        MiembroEntity::class,
        ComprobanteEntity::class,
        AvisoNoReconocidoEntity::class,
        SenalPorBancoEntity::class,
        CapturaCrudaDebugEntity::class,
    ],
    version = 1,
)
abstract class CheckQrDatabase : RoomDatabase() {
    abstract fun pagoDao(): PagoDao
    abstract fun turnoDao(): TurnoDao
    abstract fun miembroDao(): MiembroDao
    abstract fun comprobanteDao(): ComprobanteDao
    abstract fun diagnosticoDao(): DiagnosticoDao

    companion object {
        const val NOMBRE = "checkqr.db"
    }
}
