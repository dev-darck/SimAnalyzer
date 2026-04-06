package com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner

import com.project.analyzer.telemetry.analysis.api.model.handling.SessionAnalysisHandlingState
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Measurement helpers turn raw corner windows into comparable entry, apex, and exit metrics.
 */
internal fun CornerWindow.analyzeWindow(allSamples: List<SessionAnalysisSample>): CornerMeasurements? {
    val windowIndices = sampleIndices(allSamples.size)
    if (windowIndices.size < cornerMinWindowSamples) return null

    val contextIndices = expanded(
        totalSampleCount = allSamples.size,
        leadingPadding = cornerContextPaddingSamples,
        trailingPadding = cornerExitLookAheadSamples,
    ).sampleIndices(allSamples.size)
    if (contextIndices.isEmpty()) return null

    val windowSamples = windowIndices.map(allSamples::get)
    val contextSamples = contextIndices.map(allSamples::get)
    val apexSample = windowSamples.minByOrNull { sample ->
        sample.speedKmh ?: Float.MAX_VALUE
    } ?: windowSamples.maxByOrNull { sample ->
        abs(sample.lateralG ?: 0f)
    } ?: return null
    val apexIndex = contextSamples.indexOf(apexSample)
    if (apexIndex !in contextSamples.indices) return null

    val windowStartIndex = contextIndices.indexOf(windowIndices.first())
    val windowEndIndex = contextIndices.indexOf(windowIndices.last())
    if (windowStartIndex !in contextSamples.indices || windowEndIndex !in contextSamples.indices) return null

    val startTrackPosition = windowSamples.firstOrNull()?.trackPosition ?: return null
    val apexTrackPosition = apexSample.trackPosition ?: return null
    val endTrackPosition = windowSamples.lastOrNull()?.trackPosition ?: return null

    val brakePointIndex = (0..apexIndex).firstOrNull { index ->
        val sample = contextSamples[index]
        (sample.brake ?: 0f) >= cornerBrakeThreshold &&
            (sample.speedKmh ?: 0f) >= cornerLoadedSpeedKmh
    }
    val throttlePickupIndex = (apexIndex..contextSamples.lastIndex).firstOrNull { index ->
        val sample = contextSamples[index]
        (sample.throttle ?: 0f) >= cornerThrottlePickupThreshold &&
            (sample.brake ?: 0f) <= cornerCoastBrakeThreshold
    }
    val trailBrakingScore = computeTrailBrakingScore(
        allSamples = contextSamples,
        brakePointIndex = brakePointIndex,
        apexIndex = apexIndex,
    )
    val coastingRatio = computeCoastingRatio(
        allSamples = contextSamples,
        windowStartIndex = windowStartIndex,
        windowEndIndex = windowEndIndex,
        releaseIndex = brakePointIndex?.let { index ->
            findBrakeReleaseIndex(
                allSamples = contextSamples,
                brakePointIndex = index,
                apexIndex = apexIndex,
            )
        },
        throttlePickupIndex = throttlePickupIndex,
    )
    val entrySpeedKmh = contextSamples.getOrNull(brakePointIndex ?: windowStartIndex)?.speedKmh
        ?: windowSamples.firstOrNull()?.speedKmh
    val apexSpeedKmh = apexSample.speedKmh
    val exitSpeedKmh = contextSamples.takeLast(3)
        .mapNotNull(SessionAnalysisSample::speedKmh)
        .averageOrNull()
    val timeLossMs = if (wrapsAroundLap) {
        (
            (windowSamples.mapNotNull(SessionAnalysisSample::deltaToBestMs).maxOrNull() ?: 0) -
                (windowSamples.mapNotNull(SessionAnalysisSample::deltaToBestMs).minOrNull() ?: 0)
            ).coerceAtLeast(0)
    } else {
        (
            (windowSamples.lastOrNull()?.deltaToBestMs ?: 0) -
                (windowSamples.firstOrNull()?.deltaToBestMs ?: 0)
            ).coerceAtLeast(0)
    }
    val loadedSamples = windowSamples.filter { sample ->
        (sample.speedKmh ?: 0f) >= cornerLoadedSpeedKmh
    }
    val loadedCount = loadedSamples.size.coerceAtLeast(1).toFloat()

    return CornerMeasurements(
        samples = windowSamples,
        representativeSample = apexSample,
        startTrackPosition = startTrackPosition,
        apexTrackPosition = apexTrackPosition,
        endTrackPosition = endTrackPosition,
        brakePointTrackPosition = brakePointIndex?.let(contextSamples::getOrNull)?.trackPosition,
        throttlePickupTrackPosition = throttlePickupIndex?.let(contextSamples::getOrNull)?.trackPosition,
        coastingRatio = coastingRatio,
        trailBrakingScore = trailBrakingScore,
        entrySpeedKmh = entrySpeedKmh,
        apexSpeedKmh = apexSpeedKmh,
        exitSpeedKmh = exitSpeedKmh,
        timeLossMs = timeLossMs,
        understeerRatio = loadedSamples.count { sample ->
            sample.handlingState == SessionAnalysisHandlingState.Understeer
        } / loadedCount,
        oversteerRatio = loadedSamples.count { sample ->
            sample.handlingState == SessionAnalysisHandlingState.Oversteer
        } / loadedCount,
        wheelLockup = contextSamples.any(SessionAnalysisSample::hasFrontLockupSignal),
        wheelSpin = contextSamples.any(SessionAnalysisSample::hasRearWheelSpinSignal),
    )
}

internal fun SessionAnalysisSample.isCornerCandidate(): Boolean = when {
    (speedKmh ?: 0f) < cornerLoadedSpeedKmh -> abs(steeringAngleRad ?: 0f) >= cornerSteeringThresholdRad * 1.3f
    abs(steeringAngleRad ?: 0f) >= cornerSteeringThresholdRad -> true
    abs(lateralG ?: 0f) >= cornerLateralThresholdG -> true
    abs(yawRateRad ?: 0f) >= cornerYawRateThreshold -> true
    else -> false
}

internal fun SessionAnalysisSample.hasFrontLockupSignal(): Boolean {
    val brakeValue = brake ?: return false
    if (brakeValue < 0.8f) return false
    val frontSlip = listOf(tyreFl?.slip, tyreFr?.slip).mapNotNull { value -> value }
    val rearSlip = listOf(tyreRl?.slip, tyreRr?.slip).mapNotNull { value -> value }
    if (frontSlip.isEmpty()) return false
    val frontMax = frontSlip.maxOrNull() ?: return false
    val rearAverage = rearSlip.averageOrNull() ?: 0f
    return frontMax >= cornerFrontSlipWarn && (frontMax - rearAverage) >= cornerSlipDeltaWarn
}

internal fun SessionAnalysisSample.hasRearWheelSpinSignal(): Boolean {
    val throttleValue = throttle ?: return false
    if (throttleValue < 0.8f) return false
    val rearSlip = listOf(tyreRl?.slip, tyreRr?.slip).mapNotNull { value -> value }
    val frontSlip = listOf(tyreFl?.slip, tyreFr?.slip).mapNotNull { value -> value }
    if (rearSlip.isEmpty()) return false
    val rearMax = rearSlip.maxOrNull() ?: return false
    val frontAverage = frontSlip.averageOrNull() ?: 0f
    return rearMax >= cornerRearSlipWarn && (rearMax - frontAverage) >= cornerSlipDeltaWarn
}

internal fun Iterable<Float>.averageOrNull(): Float? {
    var count = 0
    var sum = 0f
    forEach { value ->
        sum += value
        count += 1
    }
    return if (count == 0) null else sum / count.toFloat()
}

private fun computeTrailBrakingScore(
    allSamples: List<SessionAnalysisSample>,
    brakePointIndex: Int?,
    apexIndex: Int,
): Int {
    val startIndex = brakePointIndex ?: return 100
    if (startIndex >= apexIndex) return 100

    val brakeWindow = allSamples.subList(startIndex, apexIndex + 1)
    val peakBrake = brakeWindow.maxOfOrNull { sample -> sample.brake ?: 0f } ?: return 100
    if (peakBrake < cornerBrakeThreshold) return 100

    val normalizedArea = brakeWindow
        .map { sample -> (sample.brake ?: 0f) / peakBrake }
        .average()
        .toFloat()
        .coerceIn(0f, 1f)
    val activeRatio = brakeWindow.count { sample ->
        (sample.brake ?: 0f) >= cornerBrakeReleaseThreshold
    }.toFloat() / brakeWindow.size.toFloat()
    val abruptDropPenalty = brakeWindow
        .zipWithNext()
        .maxOfOrNull { (current, next) ->
            ((current.brake ?: 0f) - (next.brake ?: 0f)).coerceAtLeast(0f)
        }
        ?.coerceIn(0f, 1f)
        ?: 0f

    return (
        normalizedArea * 55f +
            activeRatio * 45f -
            abruptDropPenalty * 18f
        ).roundToInt().coerceIn(0, 100)
}

private fun computeCoastingRatio(
    allSamples: List<SessionAnalysisSample>,
    windowStartIndex: Int,
    windowEndIndex: Int,
    releaseIndex: Int?,
    throttlePickupIndex: Int?,
): Float {
    val startIndex = releaseIndex ?: windowStartIndex
    val endIndex = throttlePickupIndex ?: windowEndIndex
    if (endIndex <= startIndex) return 0f

    val coastSamples = allSamples.subList(startIndex, endIndex + 1)
    return coastSamples.count { sample ->
        (sample.throttle ?: 0f) <= cornerCoastThrottleThreshold &&
            (sample.brake ?: 0f) <= cornerCoastBrakeThreshold
    }.toFloat() / coastSamples.size.toFloat()
}

private fun findBrakeReleaseIndex(
    allSamples: List<SessionAnalysisSample>,
    brakePointIndex: Int,
    apexIndex: Int,
): Int? {
    if (apexIndex <= brakePointIndex) return null
    for (index in apexIndex downTo brakePointIndex) {
        if ((allSamples[index].brake ?: 0f) >= cornerBrakeReleaseThreshold) {
            return index
        }
    }
    return null
}
