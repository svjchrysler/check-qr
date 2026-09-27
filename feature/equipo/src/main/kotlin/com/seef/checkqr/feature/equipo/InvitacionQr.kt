package com.seef.checkqr.feature.equipo

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.seef.checkqr.core.model.Role
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Lo que viaja dentro del QR de invitacion.
 *
 * El token lo emite el backend, es corto y de un solo uso, y el QR no lleva nada
 * mas: ni el nombre del comercio ni el del dueno. Un QR se fotografia, se
 * reenvia por WhatsApp y termina en sitios impredecibles, asi que no debe
 * contener nada que no sea necesario para canjearlo.
 */
@Serializable
data class CargaDeInvitacion(
    @SerialName("v") val version: Int = 1,
    @SerialName("t") val token: String,
    @SerialName("r") val rol: String,
) {
    val rolTipado: Role? get() = Role.porId(rol)
}

object InvitacionQr {

    private const val ESQUEMA = "checkqr://invitacion"
    private val json = Json { ignoreUnknownKeys = true }

    /** Texto que se codifica en el QR. */
    fun aTexto(carga: CargaDeInvitacion): String =
        "$ESQUEMA?d=" + json.encodeToString(CargaDeInvitacion.serializer(), carga)

    /**
     * Lee el contenido de un QR escaneado.
     *
     * @return null si no es una invitacion de CheckQr. Devolver null y no lanzar
     *   importa: la camara escanea cualquier QR que se le cruce, incluidos los de
     *   productos y menus, y eso no es un error que deba molestar al usuario.
     */
    fun deTexto(texto: String): CargaDeInvitacion? {
        if (!texto.startsWith(ESQUEMA)) return null
        val datos = texto.substringAfter("?d=", "").takeIf { it.isNotBlank() } ?: return null
        return runCatching { json.decodeFromString(CargaDeInvitacion.serializer(), datos) }
            .getOrNull()
            ?.takeIf { it.version == 1 && it.rolTipado != null }
    }

    /**
     * Dibuja el QR.
     *
     * Correccion de errores alta: el QR se va a mostrar en una pantalla con
     * brillo variable y se va a escanear con la camara de otro celular, a veces
     * con reflejos.
     */
    fun aBitmap(carga: CargaDeInvitacion, lado: Int = 512): Bitmap {
        val matriz = QRCodeWriter().encode(
            aTexto(carga),
            BarcodeFormat.QR_CODE,
            lado,
            lado,
            mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN to 2,
            ),
        )

        val pixeles = IntArray(lado * lado)
        for (y in 0 until lado) {
            val fila = y * lado
            for (x in 0 until lado) {
                pixeles[fila + x] = if (matriz[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(pixeles, lado, lado, Bitmap.Config.ARGB_8888)
    }
}
