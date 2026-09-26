package com.seef.checkqr.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Como reconocer el aviso de un banco. Los patrones son cadenas, no Regex, para
 * que la plantilla viaje en JSON: quien los compila es el parser.
 *
 * Que esto sea dato y no codigo es la razon por la que un cambio de formato de
 * un banco se corrige sin publicar una version nueva de la app.
 */
@Serializable
data class BankTemplate(
    val wallet: Wallet,

    /** Paquetes de la app oficial. Si no esta aqui, el aviso ni se lee. */
    @SerialName("packages")
    val packageNames: List<String>,

    /**
     * El aviso tiene que calzar con alguno de estos para considerarse un cobro.
     * Vacio significa "cualquier aviso de este paquete".
     */
    @SerialName("patrones_titulo")
    val titlePatterns: List<String> = emptyList(),

    /**
     * Patron con grupos nombrados sobre el texto del aviso. Grupos reconocidos:
     * `monto` (obligatorio), `pagador` y `referencia`.
     */
    @SerialName("patron_cuerpo")
    val bodyPattern: String,

    /** Como leer los separadores del monto. */
    @SerialName("formato_monto")
    val amountFormat: AmountFormat = AmountFormat.AUTO,

    /**
     * Avisos que este paquete emite pero que NO son cobros (por ejemplo
     * "Enviaste Bs 50"). Si calza alguno, el aviso se descarta.
     */
    @SerialName("patrones_excluir")
    val excludePatterns: List<String> = emptyList(),

    /** Con varias plantillas aplicables gana la de prioridad mas alta. */
    val prioridad: Int = 0,
)

/**
 * Bolivia escribe los importes de las dos maneras, a veces la misma app en
 * pantallas distintas.
 */
@Serializable
enum class AmountFormat {
    /** `Bs 1.234,56` — punto de miles, coma decimal. */
    @SerialName("coma_decimal")
    COMA_DECIMAL,

    /** `Bs 1,234.56` — coma de miles, punto decimal. */
    @SerialName("punto_decimal")
    PUNTO_DECIMAL,

    /** Deducirlo del propio texto. Es lo correcto mientras no haya datos reales. */
    @SerialName("auto")
    AUTO,
}

/**
 * El paquete completo de plantillas, firmado por el backend.
 *
 * La firma es Ed25519 sobre [canonico] y se verifica con una clave publica
 * fijada en la app, antes de escribir nada en disco. Sin eso, cualquiera que
 * intercepte la respuesta podria inyectar patrones arbitrarios.
 */
@Serializable
data class TemplateBundle(
    val version: Int,
    @SerialName("generado_en_millis")
    val generadoEnMillis: Long,
    val templates: List<BankTemplate>,
) {
    /**
     * Los bytes que se firman.
     *
     * Tiene que ser estable y no depender de como kotlinx.serialization ordene
     * las claves, asi que se arma a mano. Cada campo va con su longitud por
     * delante: un separador cualquiera seria ambiguo, porque los campos son
     * patrones de expresion regular y pueden contener cualquier caracter.
     */
    val canonico: String
        get() = buildString {
            campo(version.toString())
            campo(generadoEnMillis.toString())
            campo(templates.size.toString())
            templates
                .sortedWith(compareBy({ it.wallet.id }, { it.bodyPattern }))
                .forEach { t ->
                    campo(t.wallet.id)
                    campo(t.packageNames.sorted().joinToString(","))
                    campo(t.titlePatterns.joinToString(","))
                    campo(t.bodyPattern)
                    campo(t.amountFormat.name)
                    campo(t.excludePatterns.joinToString(","))
                    campo(t.prioridad.toString())
                }
        }

    private fun StringBuilder.campo(valor: String) {
        append(valor.length).append(':').append(valor).append(';')
    }
}

/** Respuesta del backend: el paquete mas su firma. */
@Serializable
data class SignedTemplateBundle(
    val bundle: TemplateBundle,
    /** Firma Ed25519 de [TemplateBundle.canonico], en base64. */
    @SerialName("firma_base64")
    val firmaBase64: String,
)
