package com.seef.checkqr.capture.listener.di

import com.seef.checkqr.capture.listener.RegistradorCrudo
import com.seef.checkqr.core.model.RawNotice
import javax.inject.Inject

/** No guarda nada. Es lo que se usa en `release`. */
class RegistradorNoOp @Inject constructor() : RegistradorCrudo {
    override val activo: Boolean = false
    override suspend fun registrar(notice: RawNotice, enListaBlanca: Boolean) = Unit
}
