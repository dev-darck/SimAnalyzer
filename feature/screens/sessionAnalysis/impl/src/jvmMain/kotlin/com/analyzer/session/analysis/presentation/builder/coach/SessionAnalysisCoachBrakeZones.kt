package com.analyzer.session.analysis.presentation.builder.coach

import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.utils.ext.averageOrNull
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun List<BrakeZone>.trailBrakingScore(): Int? = mapNotNull { zone ->
    val total = zone.endTrackPosition - zone.startTrackPosition
    val trail = zone.endTrackPosition - zone.peakTrackPosition
    if (total <= 0.001f || trail < 0f) {
        null
    } else {
        ((trail / total).coerceIn(0f, 1f) * 100f).roundToInt()
    }
}.map(Int::toFloat).averageOrNull()?.roundToInt()

internal fun extractBrakeZones(samples: List<SessionAnalysisSample>): List<BrakeZone> {
    if (samples.isEmpty()) return emptyList()
    val ordered = samples.sortedBy(SessionAnalysisSample::sampleIndexInLap)
    val zones = mutableListOf<BrakeZone>()
    var index = 0
    while (index < ordered.size) {
        val sample = ordered[index]
        val speed = sample.speedKmh ?: 0f
        val brake = sample.brake ?: 0f
        if (brake < COACH_BRAKE_THRESHOLD || speed < COACH_MIN_BRAKE_SPEED_KMH) {
            index++
            continue
        }

        var endIndex = index
        var peakBrake = brake
        var peakIndex = index
        var minimumSpeed = speed
        while (endIndex + 1 < ordered.size) {
            val next = ordered[endIndex + 1]
            val nextBrake = next.brake ?: 0f
            if (nextBrake < COACH_BRAKE_RELEASE_THRESHOLD) break
            endIndex++
            if (nextBrake > peakBrake) {
                peakBrake = nextBrake
                peakIndex = endIndex
            }
            minimumSpeed = minOf(minimumSpeed, next.speedKmh ?: minimumSpeed)
        }

        var fullThrottleTrackPosition: Float? = null
        var exitIndex = endIndex + 1
        while (exitIndex < ordered.size) {
            val exitSample = ordered[exitIndex]
            if ((exitSample.brake ?: 0f) >= COACH_BRAKE_THRESHOLD) break
            if ((exitSample.throttle ?: 0f) >= COACH_THROTTLE_COMMIT_THRESHOLD) {
                fullThrottleTrackPosition = exitSample.trackPosition
                break
            }
            exitIndex++
        }

        val startTrackPosition = ordered[index].trackPosition
        val peakTrackPosition = ordered[peakIndex].trackPosition
        val endTrackPosition = ordered[endIndex].trackPosition
        if (startTrackPosition != null && peakTrackPosition != null && endTrackPosition != null) {
            zones += BrakeZone(
                startTrackPosition = startTrackPosition,
                peakTrackPosition = peakTrackPosition,
                endTrackPosition = endTrackPosition,
                minimumSpeedKmh = minimumSpeed,
                fullThrottleTrackPosition = fullThrottleTrackPosition,
            )
        }
        index = exitIndex.coerceAtLeast(endIndex + 1)
    }
    return zones
}

internal fun matchZones(selectedZones: List<BrakeZone>, referenceZones: List<BrakeZone>): List<MatchedZone> {
    if (selectedZones.isEmpty() || referenceZones.isEmpty()) return emptyList()

    val unmatched = referenceZones.toMutableList()
    return selectedZones.mapNotNull { selected ->
        val match = unmatched.minByOrNull { reference ->
            abs(reference.peakTrackPosition - selected.peakTrackPosition)
        }?.takeIf { reference ->
            abs(reference.peakTrackPosition - selected.peakTrackPosition) <= COACH_ZONE_MATCH_WINDOW
        } ?: return@mapNotNull null

        unmatched.remove(match)
        MatchedZone(
            selected = selected,
            reference = match,
        )
    }
}
