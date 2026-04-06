package com.project.analyzer.telemetry.analysis.impl.domain.highlight

import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightSeverity
import com.project.analyzer.telemetry.analysis.impl.domain.highlight.extension.toPercentLabel
import com.project.analyzer.telemetry.analysis.impl.domain.highlight.mapper.toHighlightDraft
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerAnalysis
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerApexClassification
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerKey
import dev.zacsweers.metro.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

// Overview highlights use broad thresholds so they only fire on clearly review-worthy events.
private const val strongThrottleCommitmentThreshold: Float = 0.92f
private const val strongThrottleBrakeCeiling: Float = 0.1f
private const val heavyBrakePressureThreshold: Float = 0.75f
private const val repeatableTimeLossThresholdMs: Int = 90
private const val repeatableTimeLossMinimumLaps: Int = 2

/**
 * Builds top-level telemetry highlights from pace, deltas, and control usage before deeper corner details appear.
 */
@Inject
internal class SessionAnalysisTelemetryOverviewHighlightStage : SessionAnalysisHighlightStage {

    override val stageKey: String = "telemetry-overview"

    override fun isEnabled(options: SessionAnalysisHighlightPipelineOptions): Boolean = options.includeTelemetryOverview

    override suspend fun execute(input: SessionAnalysisHighlightContext): SessionAnalysisHighlightContext {
        val drafts = mutableListOf<SessionAnalysisHighlightDraft>()

        input.samples.maxByOrNull { sample -> sample.speedKmh ?: 0f }?.let { sample ->
            drafts += sample.toHighlightDraft(
                category = SessionAnalysisHighlightCategory.TopSpeed,
                severity = SessionAnalysisHighlightSeverity.Positive,
                title = "Top speed",
                description = "${sample.speedKmh?.roundToInt() ?: 0} km/h peak on the longest pull.",
                diagnosisSource = SessionAnalysisDiagnosisSource.DrivingStyle,
                recommendation = "Keep repeating the clean exit that feeds this straight.",
            )
        }

        input.samples
            .filter { sample ->
                (sample.throttle ?: 0f) > strongThrottleCommitmentThreshold &&
                    (sample.brake ?: 0f) < strongThrottleBrakeCeiling
            }
            .maxByOrNull { sample -> sample.speedKmh ?: 0f }
            ?.let { sample ->
                drafts += sample.toHighlightDraft(
                    category = SessionAnalysisHighlightCategory.ThrottleCommitment,
                    severity = SessionAnalysisHighlightSeverity.Positive,
                    title = "Strong throttle commitment",
                    description = "${((sample.throttle ?: 0f) * 100).roundToInt()}% throttle while the car is still accelerating hard.",
                    diagnosisSource = SessionAnalysisDiagnosisSource.DrivingStyle,
                    recommendation = "Use this exit as the reference for the rest of the lap.",
                )
            }

        input.samples
            .filter { sample -> (sample.brake ?: 0f) > heavyBrakePressureThreshold }
            .maxByOrNull { sample -> sample.speedKmh ?: 0f }
            ?.let { sample ->
                drafts += sample.toHighlightDraft(
                    category = SessionAnalysisHighlightCategory.BrakePoint,
                    severity = SessionAnalysisHighlightSeverity.Warning,
                    title = "Peak braking zone",
                    description = "${((sample.brake ?: 0f) * 100).roundToInt()}% brake pressure at speed.",
                    diagnosisSource = SessionAnalysisDiagnosisSource.DrivingStyle,
                    recommendation = "Use this stop as your baseline marker when comparing braking trends.",
                )
            }

        input.resolveRepeatableTimeLossHighlight()?.let(drafts::add)

        return input.appendDrafts(drafts)
    }

    private fun SessionAnalysisHighlightContext.resolveRepeatableTimeLossHighlight(): SessionAnalysisHighlightDraft? =
        cornerReport?.corners.orEmpty()
            .groupBy { corner -> SessionAnalysisCornerKey(corner.segmentId, corner.cornerNumber) }
            .values
            .mapNotNull { corners -> corners.toRepeatableTimeLossDraft(bestLapBySegmentId) }
            .maxByOrNull { draft -> draft.deltaMs ?: 0 }

    private fun List<SessionAnalysisCornerAnalysis>.toRepeatableTimeLossDraft(
        bestLapBySegmentId: Map<Long, Int?>,
    ): SessionAnalysisHighlightDraft? {
        val representative = firstOrNull() ?: return null
        val referenceLapNumber = bestLapBySegmentId[representative.segmentId]
        val nonReferenceLosses = filter { corner ->
            corner.lapNumber != referenceLapNumber && corner.timeLossMs >= repeatableTimeLossThresholdMs
        }
        if (nonReferenceLosses.isEmpty()) {
            return null
        }

        val strongestPass = nonReferenceLosses.maxByOrNull(SessionAnalysisCornerAnalysis::timeLossMs) ?: return null
        val sample = strongestPass.representativeSample ?: return null
        val repeatableLossMs = nonReferenceLosses.map(SessionAnalysisCornerAnalysis::timeLossMs).average().roundToInt()
        val affectedLapCount = nonReferenceLosses.map(SessionAnalysisCornerAnalysis::lapNumber).distinct().size
        if (affectedLapCount < repeatableTimeLossMinimumLaps) {
            return null
        }
        return strongestPass.toHighlightDraft(
            sample = sample,
            category = SessionAnalysisHighlightCategory.TimeLoss,
            severity = if (repeatableLossMs >= 220) {
                SessionAnalysisHighlightSeverity.Critical
            } else {
                SessionAnalysisHighlightSeverity.Warning
            },
            title = "Largest repeatable loss",
            description = buildString {
                append("Turn ")
                append(strongestPass.cornerNumber)
                append(" is costing about ")
                append(repeatableLossMs)
                append(" ms")
                if (affectedLapCount > 1) {
                    append(" across ")
                    append(affectedLapCount)
                    append(" laps")
                }
                append(". ")
                append(strongestPass.repeatableLossContext())
            },
            diagnosisSource = SessionAnalysisDiagnosisSource.DrivingStyle,
            recommendation = strongestPass.repeatableLossRecommendation(),
            deltaMs = repeatableLossMs,
        )
    }

    private fun SessionAnalysisCornerAnalysis.repeatableLossContext(): String {
        val entryLoss = abs(entrySpeedDeltaKmh ?: 0f).roundToInt()
        val exitLoss = abs(exitSpeedDeltaKmh ?: 0f).roundToInt()
        return when {
            trailBrakingScore <= 55 ->
                "Brake release fades too early before the apex, so the front axle never stays loaded."

            apexClassification == SessionAnalysisCornerApexClassification.EarlyApex ->
                "The car reaches apex too early and the exit gets pinched."

            wheelSpin && exitSpeedDeltaKmh != null && exitSpeedDeltaKmh <= -4f ->
                "Exit traction is worth roughly $exitLoss km/h versus the reference car."

            exitSpeedDeltaKmh != null && exitSpeedDeltaKmh <= -4f ->
                "The main leak is on exit where speed is about $exitLoss km/h down on reference."

            entrySpeedDeltaKmh != null && entrySpeedDeltaKmh <= -5f ->
                "The loss starts on entry where the car arrives about $entryLoss km/h slower than reference."

            coastingRatio >= 0.22f ->
                "There is too much neutral coast time through the middle of the corner."

            else ->
                "Use this turn as the first review point before chasing smaller deltas elsewhere."
        }
    }

    private fun SessionAnalysisCornerAnalysis.repeatableLossRecommendation(): String = when {
        trailBrakingScore <= 55 ->
            "Hold a small amount of brake closer to apex ${apexTrackPosition.toPercentLabel()} before releasing the pedal."

        apexClassification == SessionAnalysisCornerApexClassification.EarlyApex ->
            "Delay turn-in slightly so the car reaches a later apex and you can open the exit sooner."

        wheelSpin || (exitSpeedDeltaKmh ?: 0f) <= -4f ->
            "Be more patient to apex and commit throttle only once the wheel is opening on exit."

        (entrySpeedDeltaKmh ?: 0f) <= -5f ->
            "Brake a touch later and keep the car loaded on entry instead of overslowing the turn."

        coastingRatio >= 0.22f ->
            "Replace the coast phase with gentler trail brake or a cleaner throttle pickup."

        else ->
            "Open this corner first and compare entry, apex, and exit traces against the reference lap."
    }
}
