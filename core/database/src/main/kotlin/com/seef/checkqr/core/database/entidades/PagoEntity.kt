package com.seef.checkqr.core.database.entidades

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.seef.checkqr.core.model.ParseConfidence
import com.seef.checkqr.core.model.Payment
import com.seef.checkqr.core.model.PaymentLevel
import com.seef.checkqr.core.model.SyncState
import com.seef.checkqr.core.model.Wallet

/**
 * Un pago recibido.
 *
 * La deduplicacion vive en el indice unico sobre [dedupKey], no en una consulta
 * previa desde Kotlin: dos avisos del mismo cobro pueden llegar a la vez desde
 * hilos distintos, y solo la base puede resolver esa carrera. Al insertar con
 * `IGNORE`, el duplicado se descarta sin excepcion y sin trabajo extra.
 */
@Entity(
    tableName = "pagos",
    indices = [
        Index(value = ["dedup_key"], unique = true),
        Index(value = ["notif_posted_at_millis"]),
        Index(value = ["shift_id"]),
        Index(value = ["claimed_by_receipt_id"]),
        Index(value = ["sync_state"]),
    ],
)
data class PagoEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "dedup_key")
    val dedupKey: String,

    @ColumnInfo(name = "wallet")
    val walletId: String,

    @ColumnInfo(name = "source_package")
    val sourcePackage: String,

    /** Centavos enteros. Nunca un tipo de punto flotante. */
    @ColumnInfo(name = "amount_cents")
    val amountCents: Long,

    val currency: String,

    @ColumnInfo(name = "payer_name")
    val payerName: String?,

    val reference: String?,

    @ColumnInfo(name = "notif_posted_at_millis")
    val notifPostedAtMillis: Long,

    @ColumnInfo(name = "captured_at_millis")
    val capturedAtMillis: Long,

    @ColumnInfo(name = "level")
    val levelId: String,

    @ColumnInfo(name = "confidence")
    val confidenceId: String,

    @ColumnInfo(name = "sync_state")
    val syncStateId: String,

    @ColumnInfo(name = "shift_id")
    val shiftId: String?,

    @ColumnInfo(name = "cashier_id")
    val cashierId: String?,

    @ColumnInfo(name = "claimed_by_receipt_id")
    val claimedByReceiptId: String?,

    /**
     * Texto del aviso, solo para depurar plantillas. Se borra pasados unos dias
     * porque puede contener el nombre del pagador.
     */
    @ColumnInfo(name = "raw_text")
    val rawText: String?,
)

fun PagoEntity.aDominio(): Payment = Payment(
    id = id,
    dedupKey = dedupKey,
    wallet = Wallet.porId(walletId) ?: Wallet.DESCONOCIDA,
    sourcePackage = sourcePackage,
    amountCents = amountCents,
    currency = currency,
    payerName = payerName,
    reference = reference,
    notifPostedAtMillis = notifPostedAtMillis,
    capturedAtMillis = capturedAtMillis,
    level = PaymentLevel.porId(levelId) ?: PaymentLevel.AVISO_BANCO,
    confidence = ParseConfidence.porId(confidenceId) ?: ParseConfidence.PARCIAL,
    syncState = SyncState.porId(syncStateId) ?: SyncState.PENDIENTE,
    shiftId = shiftId,
    cashierId = cashierId,
    claimedByReceiptId = claimedByReceiptId,
    rawText = rawText,
)

fun Payment.aEntity(): PagoEntity = PagoEntity(
    id = id,
    dedupKey = dedupKey,
    walletId = wallet.id,
    sourcePackage = sourcePackage,
    amountCents = amountCents,
    currency = currency,
    payerName = payerName,
    reference = reference,
    notifPostedAtMillis = notifPostedAtMillis,
    capturedAtMillis = capturedAtMillis,
    levelId = level.id,
    confidenceId = confidence.id,
    syncStateId = syncState.id,
    shiftId = shiftId,
    cashierId = cashierId,
    claimedByReceiptId = claimedByReceiptId,
    rawText = rawText,
)
