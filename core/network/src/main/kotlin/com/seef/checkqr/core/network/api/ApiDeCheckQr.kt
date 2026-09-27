package com.seef.checkqr.core.network.api

import com.seef.checkqr.core.model.SignedTemplateBundle
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * El contrato con el backend.
 *
 * Los nombres de los campos coinciden con las etiquetas JSON del servidor Go;
 * hay un test a cada lado que lo comprueba, porque una discrepancia aqui se
 * manifiesta como "los pagos no se sincronizan" sin ningun error.
 */
interface ApiDeCheckQr {

    @POST("v1/dispositivos")
    suspend fun registrarDispositivo(@Body cuerpo: PeticionDispositivo): Response<Unit>

    @POST("v1/dispositivos/caja")
    suspend fun fijarCajaAbierta(@Body cuerpo: PeticionCaja): Response<Unit>

    @POST("v1/pagos")
    suspend fun subirPago(@Body cuerpo: PeticionPago): Response<RespuestaPago>

    /**
     * Devuelve 204 sin cuerpo cuando el celular ya esta al dia. No es un error:
     * es la respuesta normal la mayoria de las veces.
     */
    @GET("v1/plantillas")
    suspend fun plantillas(@Query("desde") desdeVersion: Int): Response<SignedTemplateBundle>

    @POST("v1/avisos-no-reconocidos")
    suspend fun subirAvisoNoReconocido(@Body cuerpo: PeticionAviso): Response<Unit>
}

@Serializable
data class PeticionDispositivo(
    @SerialName("device_id") val deviceId: String,
    @SerialName("fcm_token") val fcmToken: String,
)

@Serializable
data class PeticionCaja(
    @SerialName("device_id") val deviceId: String,
    val abierta: Boolean,
)

@Serializable
data class PeticionPago(
    /**
     * La misma clave que calcula la app para deduplicar en local.
     *
     * Es lo que hace idempotente la subida: si se corta la red a mitad de la
     * peticion, el reintento no crea un segundo pago del lado del servidor.
     */
    @SerialName("dedup_key") val dedupKey: String,
    val wallet: String,
    @SerialName("monto_centavos") val montoCentavos: Long,
    val moneda: String,
    val pagador: String?,
    val referencia: String?,
    @SerialName("aviso_en_millis") val avisoEnMillis: Long,
    val nivel: String,
    val confianza: String,
    @SerialName("turno_id") val turnoId: String?,
    @SerialName("device_id") val deviceId: String,
)

@Serializable
data class RespuestaPago(
    /** Falso si el pago ya existia. Cuenta como exito igual. */
    val nuevo: Boolean,
)

@Serializable
data class PeticionAviso(
    @SerialName("source_package") val sourcePackage: String,
    val titulo: String?,
    val texto: String?,
)
