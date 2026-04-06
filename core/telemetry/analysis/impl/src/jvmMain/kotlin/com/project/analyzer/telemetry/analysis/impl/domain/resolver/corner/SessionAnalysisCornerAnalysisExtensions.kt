package com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner

import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.TrackMapCornerZone
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.extension.normalizeTrackPosition
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Corner-analysis helpers keep repeated score and issue calculations out of the main resolver body.
 */
internal fun CornerMeasurements.toReference(cornerNumber: Int): SessionAnalysisCornerReference =
    SessionAnalysisCornerReference(
        cornerNumber = cornerNumber,
        startTrackPosition = startTrackPosition,
        apexTrackPosition = apexTrackPosition,
        endTrackPosition = endTrackPosition,
        brakePointTrackPosition = brakePointTrackPosition,
        throttlePickupTrackPosition = throttlePickupTrackPosition,
        entrySpeedKmh = entrySpeedKmh,
        apexSpeedKmh = apexSpeedKmh,
        exitSpeedKmh = exitSpeedKmh,
    )

internal fun CornerMeasurements.toGeometryReference(zone: TrackMapCornerZone): SessionAnalysisCornerReference =
    SessionAnalysisCornerReference(
        cornerNumber = zone.cornerNumber,
        startTrackPosition = zone.startTrackPosition,
        apexTrackPosition = zone.apexTrackPosition,
        endTrackPosition = zone.endTrackPosition,
        brakePointTrackPosition = brakePointTrackPosition,
        throttlePickupTrackPosition = throttlePickupTrackPosition,
        entrySpeedKmh = entrySpeedKmh,
        apexSpeedKmh = apexSpeedKmh,
        exitSpeedKmh = exitSpeedKmh,
    )

internal fun CornerMeasurements.isMeaningfulCorner(): Boolean {
    if (samples.isEmpty()) return false
    val peakSteering = samples.maxOfOrNull { sample -> abs(sample.steeringAngleRad ?: 0f) } ?: 0f
    val peakLateral = samples.maxOfOrNull { sample -> abs(sample.lateralG ?: 0f) } ?: 0f
    val loadedRatio = samples.count { sample ->
        val speed = sample.speedKmh ?: 0f
        val steering = abs(sample.steeringAngleRad ?: 0f)
        val lateral = abs(sample.lateralG ?: 0f)
        speed >= cornerLoadedSpeedKmh &&
            (steering >= cornerSteeringThresholdRad || lateral >= cornerLateralThresholdG)
    }.toFloat() / samples.size.toFloat()
    val speedDrop = ((entrySpeedKmh ?: apexSpeedKmh ?: 0f) - (apexSpeedKmh ?: entrySpeedKmh ?: 0f))
        .coerceAtLeast(0f)
    val dynamicSignals = listOf(
        peakSteering >= cornerMeaningfulSteeringRad,
        peakLateral >= cornerMeaningfulLateralG,
        speedDrop >= cornerMeaningfulSpeedDropKmh,
        loadedRatio >= cornerMeaningfulLoadedSampleRatio,
        brakePointTrackPosition != null && speedDrop >= cornerMeaningfulSpeedDropKmh * 0.55f,
    ).count { signal -> signal }

    return dynamicSignals >= 2 || (
        peakLateral >= cornerMeaningfulLateralG * 1.15f &&
            loadedRatio >= cornerMeaningfulLoadedSampleRatio * 0.85f &&
            (speedDrop >= cornerMeaningfulSpeedDropKmh * 0.45f || brakePointTrackPosition != null)
        )
}

internal fun CornerMeasurements.toCornerAnalysis(
    segmentId: Long,
    lapNumber: Int,
    cornerNumber: Int,
    reference: SessionAnalysisCornerReference?,
): SessionAnalysisCornerAnalysis {
    val apexShift = reference?.let { referenceCorner ->
        signedTrackPositionDelta(
            referenceTrackPosition = referenceCorner.apexTrackPosition,
            actualTrackPosition = apexTrackPosition,
        )
    }
    val apexClassification = when {
        apexShift == null -> SessionAnalysisCornerApexClassification.GoodApex
        apexShift < -cornerApexShiftWarnPct -> SessionAnalysisCornerApexClassification.EarlyApex
        apexShift > cornerApexShiftWarnPct -> SessionAnalysisCornerApexClassification.LateApex
        else -> SessionAnalysisCornerApexClassification.GoodApex
    }
    val entrySpeedDeltaKmh = if (entrySpeedKmh != null && reference?.entrySpeedKmh != null) {
        entrySpeedKmh - reference.entrySpeedKmh
    } else {
        null
    }
    val exitSpeedDeltaKmh = if (exitSpeedKmh != null && reference?.exitSpeedKmh != null) {
        exitSpeedKmh - reference.exitSpeedKmh
    } else {
        null
    }
    val score = computeCornerScore(
        timeLossMs = timeLossMs,
        trailBrakingScore = trailBrakingScore,
        coastingRatio = coastingRatio,
        apexClassification = apexClassification,
        exitSpeedDeltaKmh = exitSpeedDeltaKmh,
        understeerRatio = understeerRatio,
        oversteerRatio = oversteerRatio,
        wheelLockup = wheelLockup,
        wheelSpin = wheelSpin,
    )

    return SessionAnalysisCornerAnalysis(
        segmentId = segmentId,
        lapNumber = lapNumber,
        cornerNumber = cornerNumber,
        score = score,
        samples = samples,
        representativeSample = representativeSample,
        startTrackPosition = startTrackPosition,
        apexTrackPosition = apexTrackPosition,
        endTrackPosition = endTrackPosition,
        brakePointTrackPosition = brakePointTrackPosition,
        throttlePickupTrackPosition = throttlePickupTrackPosition,
        coastingRatio = coastingRatio,
        trailBrakingScore = trailBrakingScore,
        apexClassification = apexClassification,
        entrySpeedKmh = entrySpeedKmh,
        apexSpeedKmh = apexSpeedKmh,
        exitSpeedKmh = exitSpeedKmh,
        entrySpeedDeltaKmh = entrySpeedDeltaKmh,
        exitSpeedDeltaKmh = exitSpeedDeltaKmh,
        timeLossMs = timeLossMs,
        understeerRatio = understeerRatio,
        oversteerRatio = oversteerRatio,
        wheelLockup = wheelLockup,
        wheelSpin = wheelSpin,
        referenceApexTrackPosition = reference?.apexTrackPosition,
    )
}

private fun computeCornerScore(
    timeLossMs: Int,
    trailBrakingScore: Int,
    coastingRatio: Float,
    apexClassification: SessionAnalysisCornerApexClassification,
    exitSpeedDeltaKmh: Float?,
    understeerRatio: Float,
    oversteerRatio: Float,
    wheelLockup: Boolean,
    wheelSpin: Boolean,
): Int {
    var score = 100
    score -= (timeLossMs / 7).coerceAtMost(35)
    score -= ((1f - (trailBrakingScore / 100f)) * 16f).roundToInt()
    score -= ((coastingRatio - 0.12f).coerceAtLeast(0f) * 42f).roundToInt()
    if (apexClassification != SessionAnalysisCornerApexClassification.GoodApex) score -= 9
    if ((exitSpeedDeltaKmh ?: 0f) <= -4f) {
        score -= abs(exitSpeedDeltaKmh?.roundToInt() ?: 0).coerceAtMost(10)
    }
    if (understeerRatio >= 0.34f || oversteerRatio >= 0.34f) score -= 8
    if (wheelLockup) score -= 7
    if (wheelSpin) score -= 5
    return score.coerceIn(0, 100)
}

private fun signedTrackPositionDelta(referenceTrackPosition: Float, actualTrackPosition: Float): Float {
    var delta = actualTrackPosition.normalizeTrackPosition() - referenceTrackPosition.normalizeTrackPosition()
    if (delta > 0.5f) delta -= 1f
    if (delta < -0.5f) delta += 1f
    return delta
}
