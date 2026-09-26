package com.seef.checkqr.core.common

import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * El dia comercial del comerciante, no el dia UTC.
 *
 * Importa porque el cuadre se agrupa por dia: un pago de las 21:00 del martes en
 * La Paz es martes para el comerciante y miercoles en UTC. Agrupar mal significa
 * que el cuadre no cierra y el comerciante desconfia de la app.
 *
 * Bolivia es UTC-4 sin horario de verano, pero se usa la zona con nombre y no un
 * desplazamiento fijo para que siga siendo correcto si eso cambia.
 */
object Calendario {

    val ZONA_BOLIVIA: TimeZone = TimeZone.of("America/La_Paz")

    /** El dia local al que pertenece un instante. */
    fun diaDe(millis: Long, zona: TimeZone = ZONA_BOLIVIA): LocalDate =
        Instant.fromEpochMilliseconds(millis).toLocalDateTime(zona).date

    /** La hora local de un instante, para mostrar el pago en la lista. */
    fun horaDe(millis: Long, zona: TimeZone = ZONA_BOLIVIA): LocalTime =
        Instant.fromEpochMilliseconds(millis).toLocalDateTime(zona).time

    /** Primer milisegundo del dia local. Limite inferior de las consultas del cuadre. */
    fun inicioDelDiaMillis(dia: LocalDate, zona: TimeZone = ZONA_BOLIVIA): Long =
        dia.atStartOfDayIn(zona).toEpochMilliseconds()

    /**
     * Primer milisegundo del dia siguiente. Las consultas del cuadre usan un
     * intervalo semiabierto `[inicio, fin)` para no perder ni duplicar el ultimo
     * milisegundo del dia.
     */
    fun finDelDiaMillis(dia: LocalDate, zona: TimeZone = ZONA_BOLIVIA): Long =
        inicioDelDiaMillis(dia.plus(1, DateTimeUnit.DAY), zona)

    /** El rango del dia local que contiene [millis]. */
    fun rangoDelDiaDe(millis: Long, zona: TimeZone = ZONA_BOLIVIA): LongRange {
        val dia = diaDe(millis, zona)
        return inicioDelDiaMillis(dia, zona) until finDelDiaMillis(dia, zona)
    }
}
