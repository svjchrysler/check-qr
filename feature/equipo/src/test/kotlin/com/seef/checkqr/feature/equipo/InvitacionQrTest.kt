package com.seef.checkqr.feature.equipo

import com.seef.checkqr.core.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InvitacionQrTest {

    private val carga = CargaDeInvitacion(token = "abc123XYZ", rol = Role.CAJERO.id)

    @Test
    fun `ida y vuelta por el texto del QR`() {
        val leida = InvitacionQr.deTexto(InvitacionQr.aTexto(carga))
        assertEquals(carga, leida)
        assertEquals(Role.CAJERO, leida?.rolTipado)
    }

    @Test
    fun `un QR ajeno se ignora sin lanzar`() {
        // La camara escanea cualquier QR que se le cruce: el de un producto, el
        // del menu de un restaurante. Eso no es un error que deba molestar.
        assertNull(InvitacionQr.deTexto("https://ejemplo.com"))
        assertNull(InvitacionQr.deTexto(""))
        assertNull(InvitacionQr.deTexto("checkqr://otra-cosa"))
        assertNull(InvitacionQr.deTexto("checkqr://invitacion"))
        assertNull(InvitacionQr.deTexto("checkqr://invitacion?d=no-es-json"))
    }

    @Test
    fun `se rechaza un rol desconocido`() {
        val texto = InvitacionQr.aTexto(carga.copy(rol = "administrador-supremo"))
        assertNull(InvitacionQr.deTexto(texto))
    }

    @Test
    fun `se rechaza una version futura del formato`() {
        // Un QR generado por una version mas nueva de la app puede significar
        // algo distinto. Es mas seguro rechazarlo que interpretarlo mal.
        val texto = InvitacionQr.aTexto(carga.copy(version = 2))
        assertNull(InvitacionQr.deTexto(texto))
    }

    @Test
    fun `el QR no lleva nada mas que el token y el rol`() {
        // Un QR se fotografia y se reenvia por WhatsApp: no debe contener el
        // nombre del comercio, ni el del dueno, ni nada que no haga falta.
        //
        // La version 1 se omite por ser el valor por omision, lo que acorta el
        // QR y lo hace mas facil de escanear con reflejos. Al leerlo vuelve a
        // valer 1, y una version futura si viaja explicita.
        val texto = InvitacionQr.aTexto(carga)
        assertEquals(
            """checkqr://invitacion?d={"t":"abc123XYZ","r":"cajero"}""",
            texto,
        )
        assertEquals(1, InvitacionQr.deTexto(texto)?.version)
    }

    @Test
    fun `una version futura si viaja explicita en el QR`() {
        assertTrue(InvitacionQr.aTexto(carga.copy(version = 2)).contains(""""v":2"""))
    }
}
