package com.seef.checkqr.core.database.entidades

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Un aviso de un banco de la lista blanca que ninguna plantilla reconocio.
 *
 * Es la senal de que un banco cambio de formato. Se guarda para corregir la
 * plantilla, que es justamente lo que permite arreglarlo sin publicar una
 * version nueva de la app. Subirlo al backend es opcional y requiere
 * consentimiento, porque el texto puede traer el nombre de una persona.
 */
@Entity(
    tableName = "avisos_no_reconocidos",
    indices = [Index(value = ["source_package"]), Index(value = ["posted_at_millis"])],
)
data class AvisoNoReconocidoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "source_package") val sourcePackage: String,
    val titulo: String?,
    val texto: String?,
    @ColumnInfo(name = "posted_at_millis") val postedAtMillis: Long,
    /** True cuando el usuario acepto compartirlo para mejorar las plantillas. */
    @ColumnInfo(name = "compartido") val compartido: Boolean,
)

/**
 * Ultima senal de vida de cada app de banco.
 *
 * Alimenta la pantalla "Estado del sistema": si un banco lleva dias sin dar
 * senales, o el listener se desconecto, el comerciante tiene que saberlo antes
 * de descubrirlo por un pago perdido.
 */
@Entity(tableName = "senal_por_banco")
data class SenalPorBancoEntity(
    @PrimaryKey
    @ColumnInfo(name = "source_package")
    val sourcePackage: String,
    @ColumnInfo(name = "wallet") val walletId: String,
    @ColumnInfo(name = "ultimo_aviso_millis") val ultimoAvisoMillis: Long,
    /** Cierto si el ultimo aviso de este banco llego sin contenido legible. */
    @ColumnInfo(name = "ultimo_oculto") val ultimoOculto: Boolean,
)

/**
 * Volcado de TODOS los avisos, de cualquier app. Solo se escribe en la variante
 * `debug`: es la herramienta con la que se descubren los nombres de paquete y el
 * texto literal de los 6 bancos.
 *
 * La tabla existe tambien en `release` para no tener dos esquemas de Room, pero
 * en `release` nada escribe en ella.
 */
@Entity(
    tableName = "captura_cruda_debug",
    indices = [Index(value = ["source_package"]), Index(value = ["posted_at_millis"])],
)
data class CapturaCrudaDebugEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0L,
    @ColumnInfo(name = "source_package") val sourcePackage: String,
    val titulo: String?,
    val texto: String?,
    @ColumnInfo(name = "texto_largo") val textoLargo: String?,
    val subtexto: String?,
    val lineas: String?,
    @ColumnInfo(name = "posted_at_millis") val postedAtMillis: Long,
    @ColumnInfo(name = "en_lista_blanca") val enListaBlanca: Boolean,
)
