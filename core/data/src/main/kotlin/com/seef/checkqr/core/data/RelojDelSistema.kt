package com.seef.checkqr.core.data

import android.os.SystemClock
import com.seef.checkqr.core.common.Reloj
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RelojDelSistema @Inject constructor() : Reloj {
    override fun ahoraMillis(): Long = System.currentTimeMillis()
    override fun desdeElArranqueMillis(): Long = SystemClock.elapsedRealtime()
}
