package com.seef.checkqr.core.network.plantillas

import com.seef.checkqr.core.model.AmountFormat
import com.seef.checkqr.core.model.BankTemplate
import com.seef.checkqr.core.model.SignedTemplateBundle
import com.seef.checkqr.core.model.TemplateBundle
import com.seef.checkqr.core.model.Wallet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Verifica la firma contra un vector generado con la libreria estandar de Go
 * (`crypto/ed25519`), que es lo que va a usar el backend.
 *
 * El valor de este test no es probar Tink, es probar la **interoperabilidad**:
 * que la forma canonica que calcula Kotlin sea byte por byte la misma que firma
 * Go, y que la variante NO_PREFIX de Tink acepte una firma Ed25519 cruda de 64
 * bytes. Si eso no calza, las plantillas nunca se actualizarian en produccion y
 * el sintoma seria silencio, no un error.
 *
 * La semilla del vector es fija (1..32) y NO es una clave real.
 */
@RunWith(RobolectricTestRunner::class)
class VerificadorEd25519Test {

    private companion object {
        const val CLAVE_PUBLICA = "ebVWLo/mVPlAeLES6KmLp5AfhTrmlb7X4OORC60ElmQ="
        const val FIRMA =
            "iwqcuz2DDvp/CiEdJjNI2LWDDpyh3BUia6iuNRo0vK2OXgXhNVvAeOl4AhqEVvb5MQwx/998yDr0Sf/b9nohDw=="

        /** El mismo paquete que se firmo del lado de Go. */
        val BUNDLE = TemplateBundle(
            version = 7,
            generadoEnMillis = 1_700_000_000_000L,
            templates = listOf(
                BankTemplate(
                    wallet = Wallet.YAPE,
                    packageNames = listOf("com.banco.yape"),
                    titlePatterns = emptyList(),
                    bodyPattern = """recibiste Bs (?<monto>[\d.,]+)""",
                    amountFormat = AmountFormat.AUTO,
                    excludePatterns = emptyList(),
                    prioridad = 10,
                ),
            ),
        )
    }

    private fun verificador(clave: String = CLAVE_PUBLICA) = VerificadorEd25519(clave)

    @Test
    fun `la forma canonica de Kotlin coincide con la que firmo Go`() {
        val esperado = "1:7;13:1700000000000;1:1;4:yape;14:com.banco.yape;0:;" +
            """30:recibiste Bs (?<monto>[\d.,]+);4:AUTO;0:;2:10;"""
        assertEquals(esperado, BUNDLE.canonico)
    }

    @Test
    fun `acepta una firma Ed25519 hecha en Go`() {
        val r = verificador().verificar(SignedTemplateBundle(BUNDLE, FIRMA))
        assertNotNull("la firma valida de Go debe aceptarse", r)
        assertEquals(7, r!!.version)
    }

    @Test
    fun `rechaza si alguien cambia el contenido del paquete`() {
        // Este es el ataque que la firma existe para frenar: cambiar el patron
        // para que la app lea como cobro algo que no lo es.
        val alterado = BUNDLE.copy(
            templates = listOf(
                BUNDLE.templates.single().copy(bodyPattern = """(?<monto>[\d.,]+)"""),
            ),
        )
        assertNull(verificador().verificar(SignedTemplateBundle(alterado, FIRMA)))
    }

    @Test
    fun `rechaza si cambia la version aunque el resto sea igual`() {
        val alterado = BUNDLE.copy(version = 8)
        assertNull(verificador().verificar(SignedTemplateBundle(alterado, FIRMA)))
    }

    @Test
    fun `rechaza una firma de otra clave`() {
        val otraClave = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        assertNull(verificador(otraClave).verificar(SignedTemplateBundle(BUNDLE, FIRMA)))
    }

    @Test
    fun `rechaza firmas mal formadas sin lanzar excepcion`() {
        val v = verificador()
        assertNull(v.verificar(SignedTemplateBundle(BUNDLE, "")))
        assertNull(v.verificar(SignedTemplateBundle(BUNDLE, "no-es-base64-!!!")))
        assertNull(v.verificar(SignedTemplateBundle(BUNDLE, "YWJj"))) // 3 bytes
    }

    @Test
    fun `con la clave sin configurar rechaza todo, que es el fallo seguro`() {
        // Es el estado actual del proyecto: BuildConfig.CLAVE_PUBLICA_PLANTILLAS
        // esta vacia, asi que la app se queda con las plantillas empaquetadas en
        // lugar de aceptar unas sin verificar.
        assertNull(verificador("").verificar(SignedTemplateBundle(BUNDLE, FIRMA)))
    }

    @Test
    fun `una clave que no mide 32 bytes se rechaza`() {
        assertNull(verificador("YWJj").verificar(SignedTemplateBundle(BUNDLE, FIRMA)))
    }
}
