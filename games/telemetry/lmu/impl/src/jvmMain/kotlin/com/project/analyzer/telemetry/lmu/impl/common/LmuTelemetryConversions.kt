package com.project.analyzer.telemetry.lmu.impl.common

import com.project.analyzer.telemetry.api.contract.SessionPhase
import com.project.analyzer.telemetry.api.contract.SessionType
import com.project.analyzer.telemetry.api.contract.SimStatus

internal object LmuTelemetryConversions {

    fun commonGear(rawGear: Int): Int = (rawGear + 1).coerceAtLeast(0)

    fun commonSectorIndex(rawSector: Int): Int? = when (rawSector) {
        1 -> 0
        2 -> 1
        0 -> 2
        else -> null
    }

    fun sessionType(rawSession: Int): SessionType = when (rawSession) {
        0 -> SessionType.PRACTICE
        in 1..4 -> SessionType.PRACTICE
        in 5..8 -> SessionType.QUALIFYING
        9 -> SessionType.WARMUP
        in 10..13 -> SessionType.RACE
        else -> SessionType.UNKNOWN
    }

    fun sessionTypeLabel(rawSession: Int): String? =
        sessionType(rawSession).takeUnless { it == SessionType.UNKNOWN }?.name

    fun sessionPhase(rawGamePhase: Int): SessionPhase = when (rawGamePhase) {
        1, 2, 3, 4 -> SessionPhase.STARTING
        5, 6 -> SessionPhase.GREEN_FLAG
        7, 8 -> SessionPhase.SESSION_OVER
        else -> SessionPhase.NONE
    }

    fun sessionPhaseLabel(rawGamePhase: Int): String? = when (rawGamePhase) {
        0 -> "Before session"
        1 -> "Reconnaissance"
        2 -> "Grid walk"
        3 -> "Formation lap"
        4 -> "Countdown"
        5 -> "Green flag"
        6 -> "Full course yellow"
        7 -> "Session stopped"
        8 -> "Session over"
        9 -> "Paused"
        10 -> "Yellow flag"
        11 -> "Blue flag"
        else -> null
    }

    fun simStatus(inRealtime: Boolean?, rawGamePhase: Int?): SimStatus = when {
        inRealtime == false -> SimStatus.PAUSE
        rawGamePhase == null -> SimStatus.OFF
        rawGamePhase == 5 || rawGamePhase == 6 -> SimStatus.LIVE
        rawGamePhase in 1..4 -> SimStatus.PAUSE
        rawGamePhase == 7 || rawGamePhase == 8 -> SimStatus.PAUSE
        else -> SimStatus.OFF
    }

    fun lapSecondsToMs(seconds: Double): Int? =
        seconds.takeIf { it.isFinite() && it > 0.0 }?.let { (it * MS_IN_SECOND).toInt() }

    fun currentLapTimeMs(currentEt: Double, lapStartEt: Double): Int? {
        if (!currentEt.isFinite() || currentEt < 0.0) return null
        val delta = currentEt - lapStartEt.coerceAtLeast(0.0)
        return if (delta >= 0.0) (delta * MS_IN_SECOND).toInt() else null
    }

    fun kpaToPsi(kpa: Double): Float? =
        kpa.takeIf { it.isFinite() && it > 0.0 }?.let { (it * KPA_TO_PSI).toFiniteFloat() }

    fun kelvinToCelsius(kelvin: Double): Float? =
        kelvin.takeIf { it.isFinite() && it > 0.0 }?.let { (it - KELVIN_CELSIUS_OFFSET).toFiniteFloat() }

    fun finiteFloat(value: Double, min: Float? = null): Float? = value.toFiniteFloat(min = min)

    fun frontBrakeBiasFromRearBias(rearBias: Double): Float? {
        if (!rearBias.isFinite()) return null
        val frontBias = if (rearBias in 0.0..1.0) 1.0 - rearBias else rearBias
        return frontBias.toFiniteFloat(min = 0f)
    }

    private fun Double.toFiniteFloat(min: Float? = null): Float? {
        val value = toFloat()
        if (!value.isFinite()) return null
        return if (min == null || value >= min) value else null
    }

    private const val MS_IN_SECOND = 1000.0
    private const val KPA_TO_PSI = 0.1450377377
    private const val KELVIN_CELSIUS_OFFSET = 273.15
}
