package com.seef.checkqr.capture.listener.debugtools

import com.seef.checkqr.capture.listener.RegistradorCrudo
import com.seef.checkqr.core.database.dao.DiagnosticoDao
import com.seef.checkqr.core.database.entidades.CapturaCrudaDebugEntity
import com.seef.checkqr.core.model.RawNotice
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Graba TODOS los avisos, de cualquier app.
 *
 * Es la herramienta con la que se descubre el nombre de paquete y el texto
 * literal de las 6 apps de banco, que es lo que hoy falta para que la captura
 * funcione de verdad. Solo existe en la variante `debug`.
 */
@Singleton
class RegistradorDeDepuracion @Inject constructor(
    private val dao: DiagnosticoDao,
) : RegistradorCrudo {

    override val activo: Boolean = true

    override suspend fun registrar(notice: RawNotice, enListaBlanca: Boolean) {
        dao.guardarCapturaCruda(
            CapturaCrudaDebugEntity(
                sourcePackage = notice.sourcePackage,
                titulo = notice.titulo,
                texto = notice.texto,
                textoLargo = notice.textoLargo,
                subtexto = notice.subtexto,
                lineas = notice.lineas.takeIf { it.isNotEmpty() }?.joinToString("\n"),
                postedAtMillis = notice.postedAtMillis,
                enListaBlanca = enListaBlanca,
            ),
        )
    }
}
