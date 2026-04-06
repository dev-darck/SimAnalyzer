package com.project.analyzer.telemetry.analysis.impl.domain.resolver.analytics

import com.project.analyzer.telemetry.analysis.api.model.handling.SessionAnalysisHandlingState
import com.project.analyzer.telemetry.analysis.api.model.report.context.VehicleClassConfig
import com.project.analyzer.telemetry.analysis.api.model.report.corner.ApexClassification
import com.project.analyzer.telemetry.analysis.api.model.report.corner.CornerIssue
import com.project.analyzer.telemetry.analysis.api.model.report.corner.CornerThresholds
import com.project.analyzer.telemetry.analysis.api.model.report.corner.CornerType
import com.project.analyzer.telemetry.analysis.api.model.report.corner.EnhancedCornerAnalysis
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisReport
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.impl.domain.extension.bestLapBySegmentId
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.analytics.ComprehensiveSessionAnalysisResolverConstants.WarningDeltaMs
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.consistency.SessionAnalysisConsistencyReport
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.consistency.SessionAnalysisCornerConsistency
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerAnalysis
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerAnalysisReport
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerApexClassification
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerKey
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerReference
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic.SessionAnalysisCornerSetupInsight
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic.SessionAnalysisSetupDiagnosticReport
import dev.zacsweers.metro.Inject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Collapses repeated corner passes into coaching-oriented corner analyses.
 */
@Inject
internal class ComprehensiveSessionAnalysisCornerBuilder {

    internal fun build(
        report: SessionAnalysisReport,
        cornerReport: SessionAnalysisCornerAnalysisReport?,
        setupReport: SessionAnalysisSetupDiagnosticReport?,
        consistencyReport: SessionAnalysisConsistencyReport?,
        config: VehicleClassConfig,
    ): List<EnhancedCornerAnalysis> {
        val bestLapBySegment = report.laps.bestLapBySegmentId()
        val setupByCorner = setupReport?.cornerInsights.orEmpty()
        val consistencyByCorner = consistencyReport?.corners.orEmpty()
            .associateBy { SessionAnalysisCornerKey(it.segmentId, it.cornerNumber) }
        val referenceCornersBySegmentId = cornerReport?.referenceCornersBySegmentId.orEmpty()

        return cornerReport?.corners.orEmpty()
            .groupBy { SessionAnalysisCornerKey(it.segmentId, it.cornerNumber) }
            .map { (key, grouped) ->
                buildCornerAnalysisGroup(
                    key = key,
                    grouped = grouped,
                    report = report,
                    referenceCornersBySegmentId = referenceCornersBySegmentId,
                    bestLapBySegment = bestLapBySegment,
                    setupByCorner = setupByCorner,
                    consistencyByCorner = consistencyByCorner,
                    config = config,
                )
            }
            .sortedBy(EnhancedCornerAnalysis::cornerNumber)
    }

    private fun buildCornerAnalysisGroup(
        key: SessionAnalysisCornerKey,
        grouped: List<SessionAnalysisCornerAnalysis>,
        report: SessionAnalysisReport,
        referenceCornersBySegmentId: Map<Long, List<SessionAnalysisCornerReference>>,
        bestLapBySegment: Map<Long, Int?>,
        setupByCorner: Map<SessionAnalysisCornerKey, SessionAnalysisCornerSetupInsight>,
        consistencyByCorner: Map<SessionAnalysisCornerKey, SessionAnalysisCornerConsistency>,
        config: VehicleClassConfig,
    ): EnhancedCornerAnalysis {
        val reference = referenceCornersBySegmentId[key.segmentId].orEmpty()
            .firstOrNull { it.cornerNumber == key.cornerNumber }
        val referenceLap = grouped.firstOrNull { it.lapNumber == bestLapBySegment[key.segmentId] }
            ?: grouped.minByOrNull(SessionAnalysisCornerAnalysis::timeLossMs)
        val samples = collectCornerSamples(grouped)
        val groupStats = resolveCornerGroupStatistics(grouped, referenceLap)
        val sampleStats = resolveCornerSampleStatistics(
            samples = samples,
            report = report,
            averageBrakePoint = groupStats.averageBrakePoint,
            averageApexPosition = groupStats.averageApexPosition,
        )
        val referenceBrakePoint = reference?.brakePointTrackPosition ?: referenceLap?.brakePointTrackPosition ?: 0f
        val referenceThrottlePickup =
            reference?.throttlePickupTrackPosition ?: referenceLap?.throttlePickupTrackPosition ?: 0f
        val issue = detectCornerIssue(
            groupStats = groupStats,
            reference = reference,
            thresholds = config.cornerThresholds,
        )
        val setupRecommendation = setupByCorner[key]?.recommendation
        val consistency = consistencyByCorner[key]?.score ?: 80

        return EnhancedCornerAnalysis(
            cornerNumber = key.cornerNumber,
            cornerName = "Corner ${key.cornerNumber}",
            cornerType = classifyCornerType(groupStats.apexSpeed, config.cornerThresholds),
            entrySpeed = groupStats.entrySpeed,
            referenceEntrySpeed = reference?.entrySpeedKmh ?: referenceLap?.entrySpeedKmh ?: 0f,
            brakePointPosition = groupStats.averageBrakePoint,
            referenceBrakePoint = referenceBrakePoint,
            brakePointDelta = groupStats.averageBrakePoint - referenceBrakePoint,
            peakBrake = sampleStats.peakBrake,
            trailBrakingDistance = abs(groupStats.averageApexPosition - groupStats.averageBrakePoint) *
                approximateTrackLengthMeters(report.trackMap),
            trailBrakingIntensity = sampleStats.trailBrakingIntensity,
            apexSpeed = groupStats.apexSpeed,
            referenceApexSpeed = reference?.apexSpeedKmh ?: referenceLap?.apexSpeedKmh ?: 0f,
            apexPosition = groupStats.averageApexPosition,
            referenceApexPosition = reference?.apexTrackPosition ?: referenceLap?.referenceApexTrackPosition ?: 0f,
            apexClassification = toApiApex(groupStats.dominantApexClassification),
            minSpeed = sampleStats.minSpeed,
            lateralG = sampleStats.lateralG,
            exitSpeed = groupStats.exitSpeed,
            referenceExitSpeed = reference?.exitSpeedKmh ?: referenceLap?.exitSpeedKmh ?: 0f,
            throttlePickupPoint = groupStats.averageThrottlePickup,
            referenceThrottlePickup = referenceThrottlePickup,
            throttleSmoothness = sampleStats.throttleSmoothness,
            wheelSpinDetected = groupStats.hasWheelSpin,
            wheelSpinDuration = sampleStats.wheelSpinDuration,
            cornerTime = groupStats.cornerTime,
            referenceCornerTime = groupStats.referenceTime,
            timeDelta = groupStats.timeDelta,
            cornerScore = groupStats.averageScore,
            understeerRatio = groupStats.averageUndersteer,
            oversteerRatio = groupStats.averageOversteer,
            handlingState = sampleStats.handlingState,
            slipAngle = sampleStats.slipAngle,
            lapCount = grouped.size,
            speedStdDev = groupStats.speedStdDev,
            consistencyScore = consistency,
            primaryIssue = issue,
            recommendation = recommendationFor(
                issue = issue,
                cornerNumber = key.cornerNumber,
                setupSuggestion = setupRecommendation,
                groupStats = groupStats,
                reference = reference,
            ),
            setupSuggestion = setupRecommendation,
        )
    }

    private fun resolveCornerGroupStatistics(
        corners: List<SessionAnalysisCornerAnalysis>,
        referenceLap: SessionAnalysisCornerAnalysis?,
    ): CornerGroupStatistics {
        val measuredCornerTime = corners.map { it.samples.windowDurationMs() }.averageOrNull()?.roundToLong() ?: 0L
        val referenceTime = referenceLap?.samples?.windowDurationMs()?.roundToLong() ?: measuredCornerTime
        val measuredTimeDelta = measuredCornerTime - referenceTime
        val resolvedTimeDelta = corners.map(SessionAnalysisCornerAnalysis::timeLossMs).average().roundToLong()
            .takeIf { it != 0L || measuredTimeDelta == 0L }
            ?: measuredTimeDelta
        val dominantApexClassification = corners.map(SessionAnalysisCornerAnalysis::apexClassification)
            .dominantOr(SessionAnalysisCornerApexClassification.GoodApex)
        val totalPasses = corners.size.coerceAtLeast(1).toFloat()
        val wheelSpinPasses = corners.count(SessionAnalysisCornerAnalysis::wheelSpin).toFloat()
        val wheelLockupPasses = corners.count(SessionAnalysisCornerAnalysis::wheelLockup).toFloat()

        return CornerGroupStatistics(
            averageBrakePoint = corners.mapNotNull(
                SessionAnalysisCornerAnalysis::brakePointTrackPosition,
            ).averageOrNull() ?: 0f,
            averageThrottlePickup = corners.mapNotNull(
                SessionAnalysisCornerAnalysis::throttlePickupTrackPosition,
            ).averageOrNull() ?: 0f,
            averageApexPosition = corners.map(SessionAnalysisCornerAnalysis::apexTrackPosition).averageOrNull() ?: 0f,
            entrySpeed = corners.mapNotNull(SessionAnalysisCornerAnalysis::entrySpeedKmh).averageOrNull() ?: 0f,
            apexSpeed = corners.mapNotNull(SessionAnalysisCornerAnalysis::apexSpeedKmh).averageOrNull() ?: 0f,
            exitSpeed = corners.mapNotNull(SessionAnalysisCornerAnalysis::exitSpeedKmh).averageOrNull() ?: 0f,
            averageScore = corners.map(SessionAnalysisCornerAnalysis::score).average().roundToInt(),
            averageUndersteer = corners.map(SessionAnalysisCornerAnalysis::understeerRatio).averageOrNull() ?: 0f,
            averageOversteer = corners.map(SessionAnalysisCornerAnalysis::oversteerRatio).averageOrNull() ?: 0f,
            speedStdDev = corners.mapNotNull(SessionAnalysisCornerAnalysis::apexSpeedKmh).standardDeviation() ?: 0f,
            dominantApexClassification = dominantApexClassification,
            hasWheelSpin = corners.any(SessionAnalysisCornerAnalysis::wheelSpin),
            hasWheelLockup = corners.any(SessionAnalysisCornerAnalysis::wheelLockup),
            wheelSpinShare = wheelSpinPasses / totalPasses,
            wheelLockupShare = wheelLockupPasses / totalPasses,
            referenceTime = referenceTime,
            timeDelta = resolvedTimeDelta,
            cornerTime = (referenceTime + resolvedTimeDelta).coerceAtLeast(0L),
        )
    }

    private fun resolveCornerSampleStatistics(
        samples: List<SessionAnalysisSample>,
        report: SessionAnalysisReport,
        averageBrakePoint: Float,
        averageApexPosition: Float,
    ): CornerSampleStatistics {
        val throttleValues = samples.mapNotNull(SessionAnalysisSample::throttle)
        return CornerSampleStatistics(
            peakBrake = samples.mapNotNull(SessionAnalysisSample::brake).maxOrNull() ?: 0f,
            minSpeed = samples.mapNotNull(SessionAnalysisSample::speedKmh).minOrNull() ?: 0f,
            lateralG = samples.mapNotNull(SessionAnalysisSample::lateralG).maxOfOrNull(::abs) ?: 0f,
            trailBrakingIntensity = samples.filterByTrackWindow(averageBrakePoint, averageApexPosition)
                .mapNotNull(SessionAnalysisSample::brake)
                .averageOrNull()
                ?: 0f,
            throttleSmoothness = throttleValues.smoothness(0.22f),
            wheelSpinDuration = samples.durationAboveThreshold { sample ->
                listOfNotNull(sample.tyreRl?.slip, sample.tyreRr?.slip).maxOrNull()
            },
            handlingState = resolveCornerHandlingState(samples, report),
            slipAngle = resolveSlipAngle(samples),
        )
    }

    private fun resolveCornerHandlingState(
        samples: List<SessionAnalysisSample>,
        report: SessionAnalysisReport,
    ): SessionAnalysisHandlingState = samples.map(SessionAnalysisSample::handlingState).dominantOr(
        samples.firstOrNull()?.handlingState
            ?: report.samples.firstOrNull()?.handlingState
            ?: SessionAnalysisHandlingState.Neutral,
    )

    private fun resolveSlipAngle(samples: List<SessionAnalysisSample>): Float = samples.mapNotNull { sample ->
        val yaw = sample.yawRateRad ?: return@mapNotNull null
        val speed = sample.speedKmh ?: return@mapNotNull null
        abs(yaw * (speed / 3.6f) * SLIP_ANGLE_COEFFICIENT * (180f / PI.toFloat()))
    }.averageOrNull() ?: 0f

    private fun detectCornerIssue(
        groupStats: CornerGroupStatistics,
        reference: SessionAnalysisCornerReference?,
        thresholds: CornerThresholds,
    ): CornerIssue {
        if (groupStats.timeDelta <= 0L) {
            return CornerIssue.NONE
        }

        val entrySpeedLoss = resolveSpeedLoss(
            actualSpeed = groupStats.entrySpeed,
            referenceSpeed = reference?.entrySpeedKmh,
        )
        val apexSpeedLoss = resolveSpeedLoss(
            actualSpeed = groupStats.apexSpeed,
            referenceSpeed = reference?.apexSpeedKmh,
        )
        val exitSpeedLoss = resolveSpeedLoss(
            actualSpeed = groupStats.exitSpeed,
            referenceSpeed = reference?.exitSpeedKmh,
        )
        val earlyBrakeDelta = resolveEarlierBrakeDelta(groupStats, reference)
        val lateBrakeDelta = resolveLaterBrakeDelta(groupStats, reference)
        val throttlePickupDelay = resolveThrottlePickupDelay(groupStats, reference)
        val isReferenceNeutral = entrySpeedLoss < 1.5f &&
            apexSpeedLoss < 1.5f &&
            exitSpeedLoss < 1.5f &&
            earlyBrakeDelta < 0.006f &&
            lateBrakeDelta < 0.006f &&
            throttlePickupDelay < 0.008f
        val hasMeaningfulLoss = hasMeaningfulCornerLoss(
            groupStats = groupStats,
            thresholds = thresholds,
            entrySpeedLoss = entrySpeedLoss,
            apexSpeedLoss = apexSpeedLoss,
            exitSpeedLoss = exitSpeedLoss,
            earlyBrakeDelta = earlyBrakeDelta,
            lateBrakeDelta = lateBrakeDelta,
            throttlePickupDelay = throttlePickupDelay,
        )

        if (isReferenceNeutral) {
            return CornerIssue.NONE
        }

        if (!hasMeaningfulLoss && groupStats.wheelLockupShare < 0.75f && groupStats.wheelSpinShare < 0.75f) {
            return CornerIssue.NONE
        }

        return when {
            shouldFlagUndersteerEntry(
                groupStats = groupStats,
                thresholds = thresholds,
                entrySpeedLoss = entrySpeedLoss,
                apexSpeedLoss = apexSpeedLoss,
                earlyBrakeDelta = earlyBrakeDelta,
            ) -> CornerIssue.UNDERSTEER_ENTRY

            shouldFlagOversteerExit(
                groupStats = groupStats,
                thresholds = thresholds,
                exitSpeedLoss = exitSpeedLoss,
                throttlePickupDelay = throttlePickupDelay,
            ) -> CornerIssue.OVERSTEER_EXIT

            groupStats.wheelSpinShare >= 0.34f && exitSpeedLoss >= 3f -> CornerIssue.WHEEL_SPIN_EXIT

            earlyBrakeDelta > 0.012f && entrySpeedLoss >= 3f -> CornerIssue.EARLY_BRAKING

            lateBrakeDelta > 0.012f && groupStats.wheelLockupShare < 0.5f -> CornerIssue.LATE_BRAKING

            shouldFlagLockupBraking(
                groupStats = groupStats,
                entrySpeedLoss = entrySpeedLoss,
                earlyBrakeDelta = earlyBrakeDelta,
                lateBrakeDelta = lateBrakeDelta,
            ) ->
                CornerIssue.LOCKUP_BRAKING

            groupStats.dominantApexClassification == SessionAnalysisCornerApexClassification.EarlyApex &&
                exitSpeedLoss >= 2f -> CornerIssue.EARLY_APEX

            groupStats.dominantApexClassification == SessionAnalysisCornerApexClassification.LateApex &&
                entrySpeedLoss >= 2f -> CornerIssue.LATE_APEX

            throttlePickupDelay > 0.012f && exitSpeedLoss >= 2f -> CornerIssue.LATE_THROTTLE

            apexSpeedLoss >= 4f || groupStats.timeDelta > WarningDeltaMs -> CornerIssue.SLOW_APEX_SPEED

            else -> CornerIssue.NONE
        }
    }

    private fun collectCornerSamples(corners: List<SessionAnalysisCornerAnalysis>): List<SessionAnalysisSample> =
        buildList {
            corners.forEach { corner ->
                if (corner.samples.isNotEmpty()) {
                    addAll(corner.samples)
                } else {
                    corner.representativeSample?.let(::add)
                }
            }
        }

    private fun recommendationFor(
        issue: CornerIssue,
        cornerNumber: Int,
        setupSuggestion: String?,
        groupStats: CornerGroupStatistics,
        reference: SessionAnalysisCornerReference?,
    ): String {
        val entrySpeedLoss = resolveSpeedLoss(groupStats.entrySpeed, reference?.entrySpeedKmh).toGuidanceDelta()
        val apexSpeedLoss = resolveSpeedLoss(groupStats.apexSpeed, reference?.apexSpeedKmh).toGuidanceDelta()
        val exitSpeedLoss = resolveSpeedLoss(groupStats.exitSpeed, reference?.exitSpeedKmh).toGuidanceDelta()
        return when (issue) {
            CornerIssue.EARLY_BRAKING ->
                "Brake slightly later into corner $cornerNumber. Entry is about $entrySpeedLoss km/h down on reference."

            CornerIssue.LATE_BRAKING ->
                "Move the brake marker a touch earlier for corner $cornerNumber so the car can rotate without panic release."

            CornerIssue.EARLY_APEX ->
                "Delay turn-in for corner $cornerNumber. The early apex is costing roughly $exitSpeedLoss km/h on exit."

            CornerIssue.LATE_APEX ->
                "Complete rotation earlier for corner $cornerNumber so apex speed stops bleeding away."

            CornerIssue.SLOW_APEX_SPEED ->
                "Carry about $apexSpeedLoss km/h more minimum speed through corner $cornerNumber by releasing the brake more progressively."

            CornerIssue.LATE_THROTTLE ->
                "Commit to throttle earlier once the wheel is opening in corner $cornerNumber. Exit is about $exitSpeedLoss km/h down."

            CornerIssue.WHEEL_SPIN_EXIT ->
                setupSuggestion
                    ?: "Be more progressive on throttle in corner $cornerNumber. Exit is about $exitSpeedLoss km/h down on reference."

            CornerIssue.UNDERSTEER_ENTRY ->
                setupSuggestion
                    ?: "Keep the nose loaded longer into corner $cornerNumber. You are giving up roughly $apexSpeedLoss km/h at apex."

            CornerIssue.OVERSTEER_EXIT ->
                setupSuggestion
                    ?: "Stabilize the rear before full power in corner $cornerNumber. Exit is about $exitSpeedLoss km/h down."

            CornerIssue.LOCKUP_BRAKING ->
                setupSuggestion
                    ?: "Brake a touch less aggressively into corner $cornerNumber. The stop is costing entry speed and rotation."

            else -> "Use the benchmark trace from corner $cornerNumber as the model."
        }
    }

    private fun resolveSpeedLoss(actualSpeed: Float, referenceSpeed: Float?): Float {
        if (referenceSpeed == null) return 0f
        return (referenceSpeed - actualSpeed).coerceAtLeast(0f)
    }

    private fun hasMeaningfulCornerLoss(
        groupStats: CornerGroupStatistics,
        thresholds: CornerThresholds,
        entrySpeedLoss: Float,
        apexSpeedLoss: Float,
        exitSpeedLoss: Float,
        earlyBrakeDelta: Float,
        lateBrakeDelta: Float,
        throttlePickupDelay: Float,
    ): Boolean = groupStats.timeDelta > WarningDeltaMs ||
        entrySpeedLoss >= 4f ||
        apexSpeedLoss >= 4f ||
        exitSpeedLoss >= 4f ||
        earlyBrakeDelta > 0.012f ||
        lateBrakeDelta > 0.012f ||
        throttlePickupDelay > 0.012f ||
        groupStats.averageUndersteer >= thresholds.understeerWarningThreshold ||
        groupStats.averageOversteer >= thresholds.oversteerWarningThreshold

    private fun shouldFlagUndersteerEntry(
        groupStats: CornerGroupStatistics,
        thresholds: CornerThresholds,
        entrySpeedLoss: Float,
        apexSpeedLoss: Float,
        earlyBrakeDelta: Float,
    ): Boolean = groupStats.averageUndersteer >= thresholds.understeerWarningThreshold &&
        (
            entrySpeedLoss >= 2f ||
                apexSpeedLoss >= 2f ||
                earlyBrakeDelta > 0.008f ||
                groupStats.dominantApexClassification == SessionAnalysisCornerApexClassification.EarlyApex
            )

    private fun shouldFlagOversteerExit(
        groupStats: CornerGroupStatistics,
        thresholds: CornerThresholds,
        exitSpeedLoss: Float,
        throttlePickupDelay: Float,
    ): Boolean = groupStats.averageOversteer >= thresholds.oversteerWarningThreshold &&
        (
            exitSpeedLoss >= 2f ||
                throttlePickupDelay > 0.008f ||
                groupStats.wheelSpinShare >= 0.34f
            )

    private fun shouldFlagLockupBraking(
        groupStats: CornerGroupStatistics,
        entrySpeedLoss: Float,
        earlyBrakeDelta: Float,
        lateBrakeDelta: Float,
    ): Boolean = groupStats.wheelLockupShare >= 0.5f &&
        (
            entrySpeedLoss >= 2f ||
                earlyBrakeDelta > 0.006f ||
                lateBrakeDelta > 0.006f
            )

    private fun resolveEarlierBrakeDelta(
        groupStats: CornerGroupStatistics,
        reference: SessionAnalysisCornerReference?,
    ): Float {
        val referenceBrakePoint = reference?.brakePointTrackPosition ?: return 0f
        return (referenceBrakePoint - groupStats.averageBrakePoint).coerceAtLeast(0f)
    }

    private fun resolveLaterBrakeDelta(
        groupStats: CornerGroupStatistics,
        reference: SessionAnalysisCornerReference?,
    ): Float {
        val referenceBrakePoint = reference?.brakePointTrackPosition ?: return 0f
        return (groupStats.averageBrakePoint - referenceBrakePoint).coerceAtLeast(0f)
    }

    private fun resolveThrottlePickupDelay(
        groupStats: CornerGroupStatistics,
        reference: SessionAnalysisCornerReference?,
    ): Float {
        val referenceThrottlePickup = reference?.throttlePickupTrackPosition ?: return 0f
        return (groupStats.averageThrottlePickup - referenceThrottlePickup).coerceAtLeast(0f)
    }

    private fun Float.toGuidanceDelta(): Int = max(1, roundToInt())

    private fun classifyCornerType(apexSpeed: Float, thresholds: CornerThresholds): CornerType = when {
        apexSpeed >= thresholds.highSpeedCornerMin -> CornerType.HIGH_SPEED
        apexSpeed >= thresholds.mediumSpeedCornerMin -> CornerType.MEDIUM_SPEED
        apexSpeed <= thresholds.mediumSpeedCornerMin * 0.75f -> CornerType.HAIRPIN
        else -> CornerType.LOW_SPEED
    }

    private fun toApiApex(classification: SessionAnalysisCornerApexClassification): ApexClassification =
        when (classification) {
            SessionAnalysisCornerApexClassification.EarlyApex -> ApexClassification.EARLY
            SessionAnalysisCornerApexClassification.LateApex -> ApexClassification.LATE
            SessionAnalysisCornerApexClassification.GoodApex -> ApexClassification.OPTIMAL
        }

    private companion object {

        // Slightly inflates the raw yaw/speed estimate so reported slip is closer to driver feel.
        private const val SLIP_ANGLE_COEFFICIENT: Float = 0.08f
    }
}
