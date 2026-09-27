package com.seef.checkqr.feature.cuadre

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.seef.checkqr.core.common.Calendario
import com.seef.checkqr.core.common.Dinero
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** En que formato se comparte el cuadre. */
enum class FormatoDeExportacion(val extension: String, val mime: String, val etiqueta: String) {
    PDF("pdf", "application/pdf", "PDF"),

    /**
     * CSV y no .xlsx a proposito.
     *
     * Un .xlsx de verdad necesita Apache POI, que son decenas de megas y
     * problemas de dexing en Android, para una hoja de seis filas. Excel,
     * Google Sheets y WhatsApp abren un CSV sin quejarse, y el comerciante
     * consigue lo mismo. Si alguna vez hace falta el .xlsx nativo, se escribe un
     * generador minimo en lugar de meter POI.
     */
    CSV("csv", "text/csv", "Excel (CSV)"),
}

/**
 * Genera el cuadre para compartirlo por WhatsApp.
 *
 * El archivo va a la cache y se comparte por FileProvider: no hace falta
 * permiso de almacenamiento, y el sistema limpia solo lo que se acumula.
 */
@Singleton
class Exportador @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun exportar(cuadre: CuadreDelDia, formato: FormatoDeExportacion): Intent {
        val carpeta = File(context.cacheDir, CARPETA).apply { mkdirs() }
        val archivo = File(carpeta, "cuadre-${cuadre.dia}.${formato.extension}")

        when (formato) {
            FormatoDeExportacion.PDF -> escribirPdf(cuadre, archivo)
            FormatoDeExportacion.CSV -> archivo.writeText(comoCsv(cuadre), Charsets.UTF_8)
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)

        return Intent(Intent.ACTION_SEND).apply {
            type = formato.mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Cuadre de caja ${cuadre.dia}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    // --- CSV ------------------------------------------------------------------

    internal fun comoCsv(cuadre: CuadreDelDia): String = buildString {
        // BOM para que Excel en Windows reconozca UTF-8 y no rompa los acentos
        // ni el simbolo de los montos.
        append('﻿')

        appendLine("Cuadre de caja;${cuadre.dia}")
        appendLine("Total;${Dinero.formatear(cuadre.totalCentavos, conSimbolo = false)}")
        appendLine("Pagos;${cuadre.cantidad}")
        appendLine()

        seccion("Por billetera", cuadre.porBilletera)
        seccion("Por cajero", cuadre.porCajero)
        seccion("Por turno", cuadre.porTurno)
        if (cuadre.sinTurno.cantidad > 0) {
            seccion("Fuera de turno", listOf(cuadre.sinTurno))
        }

        appendLine("Detalle")
        appendLine("Hora;Billetera;Monto;Pagador;Referencia;Verificado")
        cuadre.pagos.forEach { p ->
            val hora = Calendario.horaDe(p.notifPostedAtMillis)
            appendLine(
                listOf(
                    "%02d:%02d".format(hora.hour, hora.minute),
                    p.wallet.nombreVisible,
                    Dinero.formatear(p.amountCents, conSimbolo = false),
                    escapar(p.payerName.orEmpty()),
                    escapar(p.reference.orEmpty()),
                    if (p.estaReclamado) "sí" else "no",
                ).joinToString(";"),
            )
        }
    }

    private fun StringBuilder.seccion(titulo: String, renglones: List<RenglonDeCuadre>) {
        if (renglones.isEmpty()) return
        appendLine(titulo)
        appendLine("Concepto;Pagos;Total")
        renglones.forEach {
            appendLine(
                "${escapar(it.etiqueta)};${it.cantidad};" +
                    Dinero.formatear(it.totalCentavos, conSimbolo = false),
            )
        }
        appendLine()
    }

    /**
     * Separador punto y coma y no coma, porque los montos bolivianos llevan coma
     * decimal: con coma como separador de campos, "1.234,56" partiria la celda.
     */
    private fun escapar(valor: String): String =
        if (valor.contains(';') || valor.contains('"') || valor.contains('\n')) {
            "\"" + valor.replace("\"", "\"\"") + "\""
        } else {
            valor
        }

    // --- PDF --------------------------------------------------------------------

    /**
     * PDF con `android.graphics.pdf.PdfDocument`, de la plataforma: sin ninguna
     * dependencia nueva para algo que es una hoja de texto.
     */
    private fun escribirPdf(cuadre: CuadreDelDia, destino: File) {
        val doc = PdfDocument()
        val tinta = Paint().apply { isAntiAlias = true }

        var pagina = doc.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, 1).create())
        var lienzo = pagina.canvas
        var y = MARGEN

        fun nuevaPagina() {
            doc.finishPage(pagina)
            pagina = doc.startPage(
                PdfDocument.PageInfo.Builder(ANCHO, ALTO, doc.pages.size + 1).create(),
            )
            lienzo = pagina.canvas
            y = MARGEN
        }

        fun linea(texto: String, tamano: Float = 11f, negrita: Boolean = false) {
            if (y > ALTO - MARGEN) nuevaPagina()
            tinta.textSize = tamano
            tinta.isFakeBoldText = negrita
            lienzo.drawText(texto, MARGEN.toFloat(), y.toFloat(), tinta)
            y += (tamano * 1.6f).toInt()
        }

        fun lineaDerecha(izquierda: String, derecha: String, negrita: Boolean = false) {
            if (y > ALTO - MARGEN) nuevaPagina()
            tinta.textSize = 11f
            tinta.isFakeBoldText = negrita
            lienzo.drawText(izquierda, MARGEN.toFloat(), y.toFloat(), tinta)
            val ancho = tinta.measureText(derecha)
            lienzo.drawText(derecha, ANCHO - MARGEN - ancho, y.toFloat(), tinta)
            y += 18
        }

        linea("Cuadre de caja", 18f, negrita = true)
        linea(cuadre.dia.toString(), 12f)
        y += 8
        lineaDerecha("TOTAL COBRADO", Dinero.formatear(cuadre.totalCentavos), negrita = true)
        lineaDerecha("Pagos recibidos", cuadre.cantidad.toString())
        y += 10

        fun bloque(titulo: String, renglones: List<RenglonDeCuadre>) {
            if (renglones.isEmpty()) return
            linea(titulo, 13f, negrita = true)
            renglones.forEach {
                lineaDerecha(
                    "  ${it.etiqueta} (${it.cantidad})",
                    Dinero.formatear(it.totalCentavos),
                )
            }
            y += 8
        }

        bloque("Por billetera", cuadre.porBilletera)
        bloque("Por cajero", cuadre.porCajero)
        bloque("Por turno", cuadre.porTurno)
        if (cuadre.sinTurno.cantidad > 0) {
            bloque("Fuera de turno", listOf(cuadre.sinTurno))
        }

        linea("Detalle", 13f, negrita = true)
        cuadre.pagos.forEach { p ->
            val hora = Calendario.horaDe(p.notifPostedAtMillis)
            lineaDerecha(
                "  %02d:%02d  %s  %s".format(
                    hora.hour, hora.minute, p.wallet.nombreVisible, p.payerName ?: "sin nombre",
                ),
                Dinero.formatear(p.amountCents),
            )
        }

        doc.finishPage(pagina)
        destino.outputStream().use(doc::writeTo)
        doc.close()
    }

    private companion object {
        const val CARPETA = "cuadres"

        /** A4 a 72 puntos por pulgada, que es la unidad de PdfDocument. */
        const val ANCHO = 595
        const val ALTO = 842
        const val MARGEN = 40
    }
}
