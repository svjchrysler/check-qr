package com.seef.checkqr.capture.parser

import com.seef.checkqr.core.model.AmountFormat
import com.seef.checkqr.core.model.BankTemplate
import com.seef.checkqr.core.model.ParseConfidence
import com.seef.checkqr.core.model.RawNotice
import com.seef.checkqr.core.model.Wallet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoticeParserTest {

    private val plantillaYape = BankTemplate(
        wallet = Wallet.YAPE,
        packageNames = listOf("com.banco.yape"),
        bodyPattern = """recibiste\D{0,20}Bs\.?\s*(?<monto>[\d.,]+)""" +
            """(?:\s+de\s+(?<pagador>(?:(?!\s*ref\b)[^.,\n]){2,60}))?""" +
            """(?:\D{0,20}ref\.?\s*:?\s*(?<referencia>[A-Za-z0-9-]{4,30}))?""",
        excludePatterns = listOf("""\benviaste\b"""),
        prioridad = 10,
    )

    private fun parser(vararg plantillas: BankTemplate) = NoticeParser(plantillas.toList())

    private fun aviso(
        texto: String,
        paquete: String = "com.banco.yape",
        titulo: String? = "Yape",
        postedAt: Long = 1_700_000_000_000L,
    ) = RawNotice(
        sourcePackage = paquete,
        titulo = titulo,
        texto = texto,
        textoLargo = null,
        subtexto = null,
        postedAtMillis = postedAt,
        capturedAtMillis = postedAt + 500L,
    )

    // --- Camino feliz ---------------------------------------------------------

    @Test
    fun `extrae monto, pagador y referencia`() {
        val r = parser(plantillaYape)
            .parse(aviso("Recibiste Bs 50,00 de Juan Perez ref: A1B2C3D4"))

        val cobro = r as ParseResult.Cobro
        assertEquals(Wallet.YAPE, cobro.wallet)
        assertEquals(5_000L, cobro.amountCents)
        assertEquals("Juan Perez", cobro.payerName)
        assertEquals("A1B2C3D4", cobro.reference)
        assertEquals(ParseConfidence.COMPLETA, cobro.confidence)
    }

    @Test
    fun `sin referencia la confianza es parcial pero el cobro vale`() {
        val cobro = parser(plantillaYape)
            .parse(aviso("Recibiste Bs 50,00 de Juan Perez")) as ParseResult.Cobro

        assertEquals(5_000L, cobro.amountCents)
        assertEquals("Juan Perez", cobro.payerName)
        assertEquals(null, cobro.reference)
        assertEquals(ParseConfidence.PARCIAL, cobro.confidence)
    }

    @Test
    fun `sin pagador tambien vale`() {
        val cobro = parser(plantillaYape)
            .parse(aviso("Recibiste Bs 120,50")) as ParseResult.Cobro

        assertEquals(12_050L, cobro.amountCents)
        assertEquals(null, cobro.payerName)
        assertEquals(ParseConfidence.PARCIAL, cobro.confidence)
    }

    @Test
    fun `conserva los dos instantes del aviso`() {
        val cobro = parser(plantillaYape)
            .parse(aviso("Recibiste Bs 10", postedAt = 1_234_567_890L)) as ParseResult.Cobro

        assertEquals(1_234_567_890L, cobro.notifPostedAtMillis)
        assertEquals(1_234_568_390L, cobro.capturedAtMillis)
    }

    // --- Lo que hay que descartar --------------------------------------------

    @Test
    fun `un pago saliente no es un cobro`() {
        val r = parser(plantillaYape).parse(aviso("Enviaste Bs 50,00 a Juan Perez"))
        assertTrue("fue $r", r is ParseResult.NoEsCobro)
    }

    @Test
    fun `paquete fuera de la lista blanca`() {
        val r = parser(plantillaYape)
            .parse(aviso("Recibiste Bs 50,00", paquete = "com.falso.banco"))
        assertTrue("fue $r", r is ParseResult.NoEsCobro)
    }

    @Test
    fun `aviso que ninguna plantilla reconoce`() {
        val r = parser(plantillaYape).parse(aviso("Tu saldo disponible es Bs 900,00"))
        assertTrue("fue $r", r is ParseResult.NoReconocido)
    }

    @Test
    fun `monto cero se descarta`() {
        val r = parser(plantillaYape).parse(aviso("Recibiste Bs 0,00 de Nadie"))
        assertTrue("fue $r", r is ParseResult.NoReconocido)
    }

    // --- Contenido oculto por el sistema -------------------------------------

    @Test
    fun `un aviso sin texto legible se reporta como contenido oculto`() {
        val vacio = RawNotice(
            sourcePackage = "com.banco.yape",
            titulo = null,
            texto = null,
            textoLargo = null,
            subtexto = null,
            postedAtMillis = 1L,
            capturedAtMillis = 2L,
        )
        val r = parser(plantillaYape).parse(vacio)
        assertEquals(ParseResult.ContenidoOculto("com.banco.yape"), r)
    }

    @Test
    fun `texto en blanco cuenta como oculto, no como no reconocido`() {
        val r = parser(plantillaYape).parse(aviso("   ", titulo = "  "))
        assertTrue("fue $r", r is ParseResult.ContenidoOculto)
    }

    // --- De donde se saca el texto -------------------------------------------

    @Test
    fun `usa el texto largo, que es donde los bancos ponen el detalle`() {
        val notice = RawNotice(
            sourcePackage = "com.banco.yape",
            titulo = "Yape",
            texto = "Nuevo pago",
            textoLargo = "Recibiste Bs 75,00 de Maria Lopez ref: ZZ998877",
            subtexto = null,
            postedAtMillis = 1L,
            capturedAtMillis = 2L,
        )
        val cobro = parser(plantillaYape).parse(notice) as ParseResult.Cobro
        assertEquals(7_500L, cobro.amountCents)
        assertEquals("Maria Lopez", cobro.payerName)
    }

    @Test
    fun `tambien mira las lineas de un aviso agrupado`() {
        val notice = RawNotice(
            sourcePackage = "com.banco.yape",
            titulo = "Yape",
            texto = "2 pagos nuevos",
            textoLargo = null,
            subtexto = null,
            lineas = listOf("Recibiste Bs 30,00 de Ana Quispe"),
            postedAtMillis = 1L,
            capturedAtMillis = 2L,
        )
        val cobro = parser(plantillaYape).parse(notice) as ParseResult.Cobro
        assertEquals(3_000L, cobro.amountCents)
    }

    // --- Prioridad y filtro por titulo ---------------------------------------

    @Test
    fun `con dos plantillas aplicables gana la de mayor prioridad`() {
        val generica = plantillaYape.copy(
            wallet = Wallet.DESCONOCIDA,
            bodyPattern = """Bs\.?\s*(?<monto>[\d.,]+)""",
            excludePatterns = emptyList(),
            prioridad = 0,
        )
        val cobro = parser(generica, plantillaYape)
            .parse(aviso("Recibiste Bs 50,00 de Juan Perez")) as ParseResult.Cobro

        assertEquals(Wallet.YAPE, cobro.wallet)
    }

    @Test
    fun `el filtro por titulo descarta plantillas que no corresponden`() {
        val conTitulo = plantillaYape.copy(titlePatterns = listOf("^Yape$"))
        val r = parser(conTitulo)
            .parse(aviso("Recibiste Bs 50,00", titulo = "Promociones"))
        assertTrue("fue $r", r is ParseResult.NoReconocido)
    }

    // --- Robustez frente a plantillas malas ----------------------------------

    @Test
    fun `una plantilla con regex invalida se ignora sin tumbar las demas`() {
        val rota = BankTemplate(
            wallet = Wallet.BCP,
            packageNames = listOf("com.banco.yape"),
            bodyPattern = """(?<monto>[\d.,]+""", // parentesis sin cerrar
        )
        val cobro = parser(rota, plantillaYape)
            .parse(aviso("Recibiste Bs 50,00 de Juan Perez")) as ParseResult.Cobro

        assertEquals(Wallet.YAPE, cobro.wallet)
    }

    @Test
    fun `una plantilla sin el grupo monto se descarta`() {
        val sinMonto = BankTemplate(
            wallet = Wallet.BCP,
            packageNames = listOf("com.solo.esta"),
            bodyPattern = """recibiste\s+algo""",
        )
        val p = parser(sinMonto)
        assertTrue(p.paquetesPermitidos.isEmpty())
    }

    @Test
    fun `pedir un grupo que el patron no declara no revienta`() {
        // Este patron solo define `monto`: pagador y referencia no existen.
        val soloMonto = BankTemplate(
            wallet = Wallet.BNB,
            packageNames = listOf("com.banco.bnb"),
            bodyPattern = """recibiste\D{0,20}Bs\.?\s*(?<monto>[\d.,]+)""",
        )
        val cobro = parser(soloMonto)
            .parse(aviso("Recibiste Bs 42,00", paquete = "com.banco.bnb")) as ParseResult.Cobro

        assertEquals(4_200L, cobro.amountCents)
        assertEquals(null, cobro.payerName)
        assertEquals(null, cobro.reference)
    }

    @Test
    fun `acota el texto para no darle un caso patologico a la regex`() {
        val largo = "x".repeat(50_000) + " Recibiste Bs 50,00"
        // Lo importante es que termine, y que lo que quede afuera del tope no
        // se analice.
        val r = parser(plantillaYape).parse(aviso(largo))
        assertTrue("fue $r", r is ParseResult.NoReconocido)
    }

    // --- Lista blanca --------------------------------------------------------

    @Test
    fun `la lista blanca sale de las plantillas`() {
        val p = parser(plantillaYape)
        assertEquals(setOf("com.banco.yape"), p.paquetesPermitidos)
        assertTrue(p.aceptaPaquete("com.banco.yape"))
        assertTrue(!p.aceptaPaquete("com.whatsapp"))
    }

    // --- Formato del monto segun la plantilla --------------------------------

    @Test
    fun `respeta el formato de monto que declara la plantilla`() {
        val comaDecimal = plantillaYape.copy(amountFormat = AmountFormat.COMA_DECIMAL)
        val cobro = parser(comaDecimal)
            .parse(aviso("Recibiste Bs 1.234,56")) as ParseResult.Cobro
        assertEquals(123_456L, cobro.amountCents)

        // El mismo texto con punto decimal contradice la plantilla: no se adivina.
        val r = parser(comaDecimal).parse(aviso("Recibiste Bs 50.50"))
        assertTrue("fue $r", r is ParseResult.NoReconocido)
    }

    // --- Limpieza de los campos ----------------------------------------------

    @Test
    fun `normaliza el nombre del pagador y la referencia`() {
        val cobro = parser(plantillaYape)
            .parse(aviso("Recibiste Bs 50,00 de  Juan   Perez  ref: a1b2c3d4")) as ParseResult.Cobro

        assertEquals("Juan Perez", cobro.payerName)
        assertEquals("A1B2C3D4", cobro.reference)
    }
}
