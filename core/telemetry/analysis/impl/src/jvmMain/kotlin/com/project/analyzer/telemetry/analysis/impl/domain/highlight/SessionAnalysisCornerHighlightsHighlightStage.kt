package com.project.analyzer.telemetry.analysis.impl.domain.highlight

import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightSeverity
import com.project.analyzer.telemetry.analysis.impl.domain.highlight.mapper.toHighlightDraft
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerApexClassification
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerKey
import dev.zacsweers.metro.Inject
import kotlin.math.roundToInt

/**
 * Promotes corner strengths and weaknesses into concise feed items that match the rest of the highlight pipeline.
 */
@Inject
internal class SessionAnalysisCornerHighlightsHighlightStage : SessionAnalysisHighlightStage {

    override val stageKey: String = "corner-highlights"

    override fun isEnabled(options: SessionAnalysisHighlightPipelineOptions): Boolean = options.includeCornerDiagnostics

    override suspend fun execute(input: SessionAnalysisHighlightContext): SessionAnalysisHighlightContext {
        val cornerReport = input.cornerReport ?: return input
        val setupReport = input.setupReport ?: return input
        val consistencyReport = input.consistencyReport ?: return input
        val drafts = mutableListOf<SessionAnalysisHighlightDraft>()
        val setupInsightByCorner = setupReport.cornerInsights
        val consistencyByCorner = consistencyReport.corners.associateBy { consistency ->
            SessionAnalysisCornerKey(
                segmentId = consistency.segmentId,
                cornerNumber = consistency.cornerNumber,
            )
        }

        cornerReport.corners.forEach { corner ->
            val sample = corner.representativeSample ?: return@forEach
            val bestLapNumber = input.bestLapBySegmentId[corner.segmentId]
            val isReferenceLap = bestLapNumber != null && bestLapNumber == corner.lapNumber
            val cornerKey = SessionAnalysisCornerKey(
                segmentId = corner.segmentId,
                cornerNumber = corner.cornerNumber,
            )
            val setupInsight = setupInsightByCorner[cornerKey]
            val consistency = consistencyByCorner[cornerKey]

            if (!isReferenceLap && corner.timeLossMs >= 60) {
                drafts += corner.toHighlightDraft(
                    sample = sample,
                    category = SessionAnalysisHighlightCategory.TimeLoss,
                    severity = if (corner.timeLossMs >= 140) {
                        SessionAnalysisHighlightSeverity.Critical
                    } else {
                        SessionAnalysisHighlightSeverity.Warning
                    },
                    title = "Corner ${corner.cornerNumber} is costing time",
                    description =
                    "Corner ${corner.cornerNumber} gives away ${corner.timeLossMs} ms versus the benchmark lap. " +
                        corner.timeLossContextText(),
                    diagnosisSource = SessionAnalysisDiagnosisSource.DrivingStyle,
                    recommendation = corner.timeLossRecommendationText(),
                    deltaMs = corner.timeLossMs,
                )
            }
            if (!isReferenceLap && corner.trailBrakingScore <= 55 && corner.brakePointTrackPosition != null) {
                drafts += corner.toHighlightDraft(
                    sample = sample,
                    category = SessionAnalysisHighlightCategory.TrailBrakingMissing,
                    severity = SessionAnalysisHighlightSeverity.Warning,
                    title = "Trail braking fades too early in corner ${corner.cornerNumber}",
                    description = "Brake pressure is released abruptly before the apex, which unloads the front axle.",
                    diagnosisSource = SessionAnalysisDiagnosisSource.DrivingStyle,
                    recommendation = corner.trailBrakingRecommendationText(),
                    deltaMs = corner.timeLossMs.takeIf { value -> value > 0 },
                )
            }
            if (!isReferenceLap && corner.apexClassification == SessionAnalysisCornerApexClassification.EarlyApex) {
                drafts += corner.toHighlightDraft(
                    sample = sample,
                    category = SessionAnalysisHighlightCategory.EarlyApexEntry,
                    severity = SessionAnalysisHighlightSeverity.Warning,
                    title = "Early apex in corner ${corner.cornerNumber}",
                    description = "The car reaches apex earlier than reference, which typically hurts exit width and exit speed.",
                    diagnosisSource = SessionAnalysisDiagnosisSource.DrivingStyle,
                    recommendation = earlyApexRecommendationText,
                    deltaMs = corner.timeLossMs.takeIf { value -> value > 0 },
                )
            }
            if (!isReferenceLap && corner.apexClassification == SessionAnalysisCornerApexClassification.LateApex) {
                drafts += corner.toHighlightDraft(
                    sample = sample,
                    category = SessionAnalysisHighlightCategory.LateApexEntry,
                    severity = SessionAnalysisHighlightSeverity.Warning,
                    title = "Late apex in corner ${corner.cornerNumber}",
                    description = "The car stays too deep into the corner before rotation completes, costing entry confidence.",
                    diagnosisSource = SessionAnalysisDiagnosisSource.DrivingStyle,
                    recommendation = lateApexRecommendationText,
                    deltaMs = corner.timeLossMs.takeIf { value -> value > 0 },
                )
            }
            if (!isReferenceLap && corner.coastingRatio >= 0.22f) {
                drafts += corner.toHighlightDraft(
                    sample = sample,
                    category = SessionAnalysisHighlightCategory.CoastingZone,
                    severity = SessionAnalysisHighlightSeverity.Warning,
                    title = "Coasting zone in corner ${corner.cornerNumber}",
                    description = "Inputs are idle through ${(corner.coastingRatio * 100f).roundToInt()}% of the corner window.",
                    diagnosisSource = SessionAnalysisDiagnosisSource.DrivingStyle,
                    recommendation = coastingRecommendationText,
                    deltaMs = corner.timeLossMs.takeIf { value -> value > 0 },
                )
            }
            if (corner.wheelLockup && !isReferenceLap) {
                drafts += corner.toHighlightDraft(
                    sample = sample,
                    category = SessionAnalysisHighlightCategory.WheelLockup,
                    severity = SessionAnalysisHighlightSeverity.Warning,
                    title = "Wheel lockup into corner ${corner.cornerNumber}",
                    description = "Front-wheel slip spikes under peak braking are flattening entry grip.",
                    diagnosisSource = setupInsight?.diagnosisSource ?: SessionAnalysisDiagnosisSource.Mixed,
                    recommendation = corner.lockupRecommendationText(setupInsight),
                    deltaMs = corner.timeLossMs.takeIf { value -> value > 0 },
                )
            }
            if (corner.wheelSpin && !isReferenceLap) {
                drafts += corner.toHighlightDraft(
                    sample = sample,
                    category = SessionAnalysisHighlightCategory.WheelSpin,
                    severity = SessionAnalysisHighlightSeverity.Warning,
                    title = "Wheel spin on exit of corner ${corner.cornerNumber}",
                    description = "Rear tyre slip spikes under throttle are limiting traction on exit.",
                    diagnosisSource = SessionAnalysisDiagnosisSource.Mixed,
                    recommendation = wheelSpinRecommendationText,
                    deltaMs = corner.timeLossMs.takeIf { value -> value > 0 },
                )
            }
            if (corner.understeerRatio >= 0.34f && (!isReferenceLap || setupInsight != null)) {
                drafts += corner.toHighlightDraft(
                    sample = sample,
                    category = SessionAnalysisHighlightCategory.Understeer,
                    severity = SessionAnalysisHighlightSeverity.Warning,
                    title = "Understeer in corner ${corner.cornerNumber}",
                    description = "Front-end response falls behind steering demand for ${(corner.understeerRatio * 100f).roundToInt()}% of the loaded phase.",
                    diagnosisSource = setupInsight?.diagnosisSource ?: SessionAnalysisDiagnosisSource.DrivingStyle,
                    recommendation = corner.understeerRecommendationText(setupInsight),
                    deltaMs = corner.timeLossMs.takeIf { value -> value > 0 },
                )
            }
            if (corner.oversteerRatio >= 0.34f && (!isReferenceLap || setupInsight != null)) {
                drafts += corner.toHighlightDraft(
                    sample = sample,
                    category = SessionAnalysisHighlightCategory.Oversteer,
                    severity = SessionAnalysisHighlightSeverity.Warning,
                    title = "Oversteer in corner ${corner.cornerNumber}",
                    description = "Rear rotation exceeds the steering demand for ${(corner.oversteerRatio * 100f).roundToInt()}% of the loaded phase.",
                    diagnosisSource = setupInsight?.diagnosisSource ?: SessionAnalysisDiagnosisSource.DrivingStyle,
                    recommendation = corner.oversteerRecommendationText(setupInsight),
                    deltaMs = corner.timeLossMs.takeIf { value -> value > 0 },
                )
            }
            if (consistency != null && consistency.score <= 72) {
                drafts += corner.toHighlightDraft(
                    sample = sample,
                    category = SessionAnalysisHighlightCategory.InconsistentLine,
                    severity = SessionAnalysisHighlightSeverity.Warning,
                    title = "Inconsistent execution in corner ${corner.cornerNumber}",
                    description = "Brake point, throttle pickup or line varies heavily between laps in the same corner.",
                    diagnosisSource = SessionAnalysisDiagnosisSource.DrivingStyle,
                    recommendation = "Pick one repeatable brake marker and apex target here before trying to drive around the inconsistency.",
                    deltaMs = corner.timeLossMs.takeIf { value -> value > 0 },
                    affectedLaps = consistency.affectedLaps,
                )
            }
        }

        setupReport.diagnostics.forEach { diagnostic ->
            drafts += diagnostic.toHighlightDraft()
        }

        return input.appendDrafts(drafts)
    }
}
