package com.project.analyzer.telemetry.analysis.impl.domain.resolver.analytics

import com.project.analyzer.telemetry.analysis.api.model.lap.SessionAnalysisLap
import com.project.analyzer.telemetry.analysis.api.model.report.corner.CornerIssue
import com.project.analyzer.telemetry.analysis.api.model.report.corner.CornerType
import com.project.analyzer.telemetry.analysis.api.model.report.corner.EnhancedCornerAnalysis
import com.project.analyzer.telemetry.analysis.api.model.report.segment.SegmentAnalysis
import com.project.analyzer.telemetry.analysis.api.model.report.segment.SegmentIssue
import com.project.analyzer.telemetry.analysis.api.model.report.segment.SegmentType
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisReport
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.impl.domain.extension.bestLapBySegmentId
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.analytics.ComprehensiveSessionAnalysisResolverConstants.MinStraightWidth
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.analytics.ComprehensiveSessionAnalysisResolverConstants.WarningDeltaMs
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerAnalysisReport
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerReference
import dev.zacsweers.metro.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * Derives segment-level analytics from corner analyses and straight windows.
 */
@Inject
internal class ComprehensiveSessionAnalysisSegmentBuilder {

    internal fun build(
        report: SessionAnalysisReport,
        cornerAnalyses: List<EnhancedCornerAnalysis>,
        cornerReport: SessionAnalysisCornerAnalysisReport?,
    ): List<SegmentAnalysis> {
        val segments = mutableListOf<SegmentAnalysis>()
        var nextId = 1

        cornerAnalyses.forEach { corner ->
            segments += SegmentAnalysis(
                segmentId = nextId++,
                segmentName = corner.cornerName,
                startPosition = min(corner.brakePointPosition, corner.apexPosition).coerceIn(0f, 1f),
                endPosition = max(corner.throttlePickupPoint, corner.apexPosition).coerceIn(0f, 1f),
                segmentType = toSegmentType(corner.cornerType),
                corners = listOf(corner),
                minSpeed = corner.minSpeed,
                maxSpeed = max(corner.entrySpeed, corner.exitSpeed),
                avgSpeed = listOf(corner.entrySpeed, corner.apexSpeed, corner.exitSpeed).average().toFloat(),
                timeDelta = corner.timeDelta,
                isTimeLossSegment = corner.timeDelta > 0L,
                avgThrottle = 0.5f,
                peakThrottle = 1f,
                avgBrake = corner.peakBrake * 0.65f,
                peakBrake = corner.peakBrake,
                avgSteeringAngle = corner.slipAngle,
                steeringSmoothness = corner.throttleSmoothness,
                deltaSpeed = corner.exitSpeed - corner.referenceExitSpeed,
                deltaThrottle = corner.throttlePickupPoint - corner.referenceThrottlePickup,
                deltaBrake = corner.brakePointDelta,
                issues = toSegmentIssues(corner.primaryIssue),
                recommendations = listOfNotNull(corner.recommendation),
                severity = corner.timeDelta.toIssueSeverity(),
            )
        }

        val bestLapBySegment = report.laps.bestLapBySegmentId()
        val validLapNumbersBySegment = report.laps
            .filter { lap -> lap.isValid && lap.isComplete && !lap.isPitLap }
            .groupBy(SessionAnalysisLap::segmentId)
            .mapValues { (_, laps) -> laps.map(SessionAnalysisLap::lapNumber).toSet() }

        report.samples.groupBy(SessionAnalysisSample::segmentId).forEach { (segmentId, segmentSamples) ->
            val references = cornerReport?.referenceCornersBySegmentId?.get(
                segmentId,
            ).orEmpty().sortedBy(SessionAnalysisCornerReference::startTrackPosition)
            if (references.isEmpty()) return@forEach
            val validLapNumbers = validLapNumbersBySegment[segmentId].orEmpty()
            val samplesByLap = segmentSamples
                .filter { sample -> sample.lapNumber in validLapNumbers }
                .groupBy(SessionAnalysisSample::lapNumber)
            if (samplesByLap.isEmpty()) return@forEach

            references.zipWithNext().forEachIndexed { index, (current, nextReference) ->
                val start = current.endTrackPosition.coerceIn(0f, 1f)
                val end = nextReference.startTrackPosition.coerceIn(0f, 1f)
                val width = (end - start).coerceAtLeast(0f)
                if (width < MinStraightWidth) {
                    return@forEachIndexed
                }
                resolveStraightSegment(
                    segmentId = nextId++,
                    name = "Straight ${index + 1}",
                    start = start,
                    end = end,
                    samplesByLap = samplesByLap,
                    referenceLapNumber = bestLapBySegment[segmentId],
                )?.let(segments::add)
            }

            val lastReference = references.last()
            val firstReference = references.first()
            val wrapStart = lastReference.endTrackPosition.coerceIn(0f, 1f)
            val wrapEnd = firstReference.startTrackPosition.coerceIn(0f, 1f)
            val wrapWidth = (1f - wrapStart) + wrapEnd
            if (wrapWidth >= MinStraightWidth) {
                resolveStraightSegment(
                    segmentId = nextId++,
                    name = "Start/finish straight",
                    start = wrapStart,
                    end = wrapEnd,
                    samplesByLap = samplesByLap,
                    referenceLapNumber = bestLapBySegment[segmentId],
                )?.let(segments::add)
            }
        }

        return segments
    }

    private fun resolveStraightSegment(
        segmentId: Int,
        name: String,
        start: Float,
        end: Float,
        samplesByLap: Map<Int, List<SessionAnalysisSample>>,
        referenceLapNumber: Int?,
    ): SegmentAnalysis? {
        val windows = samplesByLap.values.map { lap ->
            lap.sortedBy(SessionAnalysisSample::sampleIndexInLap).filterByTrackWindow(start, end)
        }.filter { it.size >= 2 }
        if (windows.isEmpty()) return null

        val referenceWindow = referenceLapNumber?.let { lapNumber ->
            samplesByLap[lapNumber]?.sortedBy(SessionAnalysisSample::sampleIndexInLap)?.filterByTrackWindow(start, end)
        }?.takeIf { it.size >= 2 }
        val aggregate = windows.collectStraightWindowAggregate()
        val referenceAggregate = referenceWindow?.let { listOf(it).collectStraightWindowAggregate() }
        val avgSpeed = aggregate.averageSpeed
        val referenceSpeed = referenceAggregate?.averageSpeed ?: avgSpeed
        val avgTime = aggregate.averageDurationMs.roundToLong()
        val referenceTime = referenceAggregate?.averageDurationMs?.roundToLong() ?: avgTime
        val issues = buildList {
            if (referenceSpeed - avgSpeed >= 6f) add(SegmentIssue.STRAIGHT_SPEED_DEFICIT)
            if (avgTime > referenceTime + WarningDeltaMs) add(SegmentIssue.TIME_LOSS)
        }

        return SegmentAnalysis(
            segmentId = segmentId,
            segmentName = name,
            startPosition = start,
            endPosition = end,
            segmentType = SegmentType.STRAIGHT,
            minSpeed = aggregate.resolvedMinSpeed,
            maxSpeed = aggregate.resolvedMaxSpeed,
            avgSpeed = avgSpeed,
            timeDelta = avgTime - referenceTime,
            isTimeLossSegment = avgTime > referenceTime,
            avgThrottle = aggregate.averageThrottle,
            peakThrottle = aggregate.peakThrottle,
            avgBrake = aggregate.averageBrake,
            peakBrake = aggregate.peakBrake,
            avgSteeringAngle = aggregate.averageSteeringAngle,
            steeringSmoothness = aggregate.steeringValues.smoothness(0.18f),
            deltaSpeed = avgSpeed - referenceSpeed,
            deltaThrottle = aggregate.averageThrottle - (referenceAggregate?.averageThrottle ?: 0f),
            deltaBrake = aggregate.averageBrake - (referenceAggregate?.averageBrake ?: 0f),
            issues = issues,
            recommendations = if (SegmentIssue.STRAIGHT_SPEED_DEFICIT in issues) {
                listOf("Prioritize the exit before this straight. The loss is carried onto full throttle.")
            } else {
                emptyList()
            },
            severity = (avgTime - referenceTime).toIssueSeverity(),
        )
    }

    private fun toSegmentType(cornerType: CornerType): SegmentType = when (cornerType) {
        CornerType.HAIRPIN -> SegmentType.HAIRPIN
        CornerType.CHICANE -> SegmentType.CHICANE
        CornerType.HIGH_SPEED -> SegmentType.HIGH_SPEED_CORNER
        CornerType.MEDIUM_SPEED -> SegmentType.MEDIUM_SPEED_CORNER
        CornerType.LOW_SPEED -> SegmentType.LOW_SPEED_CORNER
    }

    private fun toSegmentIssues(issue: CornerIssue?): List<SegmentIssue> = when (issue) {
        CornerIssue.EARLY_BRAKING -> listOf(SegmentIssue.EARLY_BRAKING)

        CornerIssue.LATE_BRAKING -> listOf(SegmentIssue.LATE_BRAKING)

        CornerIssue.SLOW_APEX_SPEED -> listOf(SegmentIssue.SLOW_APEX)

        CornerIssue.LATE_THROTTLE -> listOf(SegmentIssue.LATE_THROTTLE)

        CornerIssue.WHEEL_SPIN_EXIT -> listOf(SegmentIssue.WHEEL_SPIN)

        CornerIssue.UNDERSTEER_ENTRY -> listOf(SegmentIssue.UNDERSTEER)

        CornerIssue.OVERSTEER_EXIT -> listOf(SegmentIssue.OVERSTEER)

        CornerIssue.LOCKUP_BRAKING -> listOf(SegmentIssue.BRAKE_LOCKUP)

        null,
        CornerIssue.NONE,
        -> emptyList()

        else -> listOf(SegmentIssue.TIME_LOSS)
    }

    private fun List<List<SessionAnalysisSample>>.collectStraightWindowAggregate(): StraightWindowAggregate {
        var windowCount = 0
        var speedAverageSum = 0f
        var minSpeed = Float.POSITIVE_INFINITY
        var maxSpeed = Float.NEGATIVE_INFINITY
        var throttleSum = 0f
        var throttleCount = 0
        var peakThrottle = 0f
        var brakeSum = 0f
        var brakeCount = 0
        var peakBrake = 0f
        var steeringAbsSum = 0f
        var steeringCount = 0
        val steeringValues = mutableListOf<Float>()
        var durationSumMs = 0f

        forEach { window ->
            var windowSpeedSum = 0f
            var windowSpeedCount = 0
            window.forEach { sample ->
                sample.speedKmh?.let { speed ->
                    minSpeed = min(minSpeed, speed)
                    maxSpeed = max(maxSpeed, speed)
                    windowSpeedSum += speed
                    windowSpeedCount++
                }
                sample.throttle?.let { throttle ->
                    throttleSum += throttle
                    throttleCount++
                    peakThrottle = max(peakThrottle, throttle)
                }
                sample.brake?.let { brake ->
                    brakeSum += brake
                    brakeCount++
                    peakBrake = max(peakBrake, brake)
                }
                sample.steeringAngleRad?.let { steering ->
                    steeringAbsSum += abs(steering)
                    steeringCount++
                    steeringValues += steering
                }
            }
            if (windowSpeedCount > 0) {
                speedAverageSum += windowSpeedSum / windowSpeedCount.toFloat()
                windowCount++
            }
            durationSumMs += window.windowDurationMs()
        }

        val resolvedWindowCount = windowCount.takeIf { it > 0 } ?: size
        return StraightWindowAggregate(
            windowCount = resolvedWindowCount,
            speedAverageSum = speedAverageSum,
            minSpeed = minSpeed,
            maxSpeed = maxSpeed,
            throttleSum = throttleSum,
            throttleCount = throttleCount,
            peakThrottle = peakThrottle,
            brakeSum = brakeSum,
            brakeCount = brakeCount,
            peakBrake = peakBrake,
            steeringAbsSum = steeringAbsSum,
            steeringCount = steeringCount,
            steeringValues = steeringValues,
            durationSumMs = durationSumMs,
        )
    }
}
