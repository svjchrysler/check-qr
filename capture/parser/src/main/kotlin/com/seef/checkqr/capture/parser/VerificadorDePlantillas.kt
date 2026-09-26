package com.seef.checkqr.capture.parser

import com.seef.checkqr.core.model.SignedTemplateBundle
import com.seef.checkqr.core.model.TemplateBundle

/**
 * Comprueba que un paquete de plantillas viene de nuestro backend.
 *
 * Es imprescindible, no un extra: las plantillas son expresiones regulares que
 * la app ejecuta, y decidir cual aviso es un cobro y por cuanto. Quien pueda
 * cambiarlas puede hacer que la app anuncie pagos que no existen. Por eso se
 * verifica la firma **antes** de escribir el paquete en disco, y nunca despues.
 *
 * La interfaz vive aqui, en Kotlin puro, para que el parser y sus tests no
 * dependan de Android; la implementacion Ed25519 con Tink esta en :core:network.
 */
interface VerificadorDePlantillas {

    /**
     * @return el paquete si la firma corresponde a [SignedTemplateBundle.bundle]
     *   y a la clave publica fijada en la app; null en cualquier otro caso,
     *   incluido un error de formato de la firma.
     */
    fun verificar(firmado: SignedTemplateBundle): TemplateBundle?
}

/** Verificador que rechaza todo. Es el valor por omision seguro. */
object VerificadorQueRechaza : VerificadorDePlantillas {
    override fun verificar(firmado: SignedTemplateBundle): TemplateBundle? = null
}
