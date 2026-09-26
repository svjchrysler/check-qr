package com.seef.checkqr.core.database.entidades

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.seef.checkqr.core.model.MatchResult
import com.seef.checkqr.core.model.Receipt

/**
 * Un comprobante que el cajero fotografio. Se guarda el texto del OCR, no la
 * foto: la imagen no aporta nada despues de comparar y es lo que mas pesa.
 */
@Entity(
    tableName = "comprobantes",
    indices = [Index(value = ["matched_payment_id"])],
)
data class ComprobanteEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "captured_at_millis") val capturedAtMillis: Long,
    @ColumnInfo(name = "texto_ocr") val textoOcr: String,
    @ColumnInfo(name = "amount_cents") val amountCents: Long?,
    val reference: String?,
    @ColumnInfo(name = "matched_payment_id") val matchedPaymentId: String?,
    @ColumnInfo(name = "resultado") val resultadoId: String,
)

fun ComprobanteEntity.aDominio() = Receipt(
    id = id,
    capturedAtMillis = capturedAtMillis,
    textoOcr = textoOcr,
    amountCents = amountCents,
    reference = reference,
    matchedPaymentId = matchedPaymentId,
    resultado = MatchResult.porId(resultadoId) ?: MatchResult.AMBIGUO,
)

fun Receipt.aEntity() = ComprobanteEntity(
    id = id,
    capturedAtMillis = capturedAtMillis,
    textoOcr = textoOcr,
    amountCents = amountCents,
    reference = reference,
    matchedPaymentId = matchedPaymentId,
    resultadoId = resultado.id,
)
