package com.seef.checkqr.core.network.plantillas

import android.util.Base64
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.PublicKeyVerify
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.signature.Ed25519PublicKey
import com.google.crypto.tink.signature.SignatureConfig
import com.google.crypto.tink.util.Bytes
import com.seef.checkqr.capture.parser.VerificadorDePlantillas
import com.seef.checkqr.core.model.SignedTemplateBundle
import com.seef.checkqr.core.model.TemplateBundle
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Verifica la firma Ed25519 del paquete de plantillas.
 *
 * Se usa Tink y no `java.security.Signature` porque Ed25519 solo llego a la
 * plataforma en API 33 y el minSdk de la app es 26: en un celular con Android 8
 * el proveedor del sistema no lo tiene.
 *
 * La variante es NO_PREFIX, es decir Ed25519 crudo de 64 bytes sin cabecera de
 * Tink. Eso importa: es lo que produce `ed25519.Sign` de la libreria estandar de
 * Go, asi que el backend no necesita Tink para firmar.
 */
@Singleton
class VerificadorEd25519 @Inject constructor(
    @ClavePublicaDePlantillas private val clavePublicaBase64: String,
) : VerificadorDePlantillas {

    /**
     * Se construye una vez y de forma perezosa. Si la clave esta mal, queda null
     * y el verificador rechaza todo, que es el fallo seguro: es preferible
     * quedarse con las plantillas empaquetadas que aceptar unas sin verificar.
     */
    private val verificador: PublicKeyVerify? by lazy { construir() }

    override fun verificar(firmado: SignedTemplateBundle): TemplateBundle? {
        val v = verificador ?: return null

        val firma = try {
            Base64.decode(firmado.firmaBase64, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            return null
        }
        if (firma.size != TAMANO_FIRMA_ED25519) return null

        // Se firma la forma canonica, no el JSON crudo: asi la firma no depende de
        // como el serializador ordene las claves ni de los espacios.
        val datos = firmado.bundle.canonico.toByteArray(Charsets.UTF_8)

        return try {
            v.verify(firma, datos)
            firmado.bundle
        } catch (e: GeneralSecurityException) {
            null
        }
    }

    private fun construir(): PublicKeyVerify? = try {
        SignatureConfig.register()
        val bytes = Base64.decode(clavePublicaBase64, Base64.DEFAULT)
        if (bytes.size != TAMANO_CLAVE_ED25519) return null

        val clave = Ed25519PublicKey.create(Bytes.copyFrom(bytes))
        KeysetHandle.newBuilder()
            .addEntry(KeysetHandle.importKey(clave).withRandomId().makePrimary())
            .build()
            .getPrimitive(RegistryConfiguration.get(), PublicKeyVerify::class.java)
    } catch (e: GeneralSecurityException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }

    private companion object {
        const val TAMANO_CLAVE_ED25519 = 32
        const val TAMANO_FIRMA_ED25519 = 64
    }
}

/** Marca la clave publica con la que se verifican las plantillas. */
@javax.inject.Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ClavePublicaDePlantillas
