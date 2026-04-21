package com.project.analyzer.telemetry.analysis.impl.domain.resolver.analytics

import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlight
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightSeverity
import com.project.analyzer.telemetry.analysis.api.model.lap.SessionAnalysisLap
import com.project.analyzer.telemetry.analysis.api.model.report.analysis.ConsistencyAnalysis
import com.project.analyzer.telemetry.analysis.api.model.report.analysis.FuelAnalysis
import com.project.analyzer.telemetry.analysis.api.model.report.analysis.TyreAnalysis
import com.project.analyzer.telemetry.analysis.api.model.report.common.Difficulty
import com.project.analyzer.telemetry.analysis.api.model.report.common.Grade
import com.project.analyzer.telemetry.analysis.api.model.report.common.IssueSeverity
import com.project.analyzer.telemetry.analysis.api.model.report.common.Priority
import com.project.analyzer.telemetry.analysis.api.model.report.comparison.ComparativeAnalysis
import com.project.analyzer.telemetry.analysis.api.model.report.comparison.ImprovementOpportunity
import com.project.analyzer.telemetry.analysis.api.model.report.comparison.LapReference
import com.project.analyzer.telemetry.analysis.api.model.report.comparison.PatternType
import com.project.analyzer.telemetry.analysis.api.model.report.comparison.PerformanceLoss
import com.project.analyzer.telemetry.analysis.api.model.report.comparison.SegmentComparison
import com.project.analyzer.telemetry.analysis.api.model.report.context.SessionContext
import com.project.analyzer.telemetry.analysis.api.model.report.corner.CornerIssue
import com.project.analyzer.telemetry.analysis.api.model.report.corner.EnhancedCornerAnalysis
import com.project.analyzer.telemetry.analysis.api.model.report.segment.SegmentAnalysis
import com.project.analyzer.telemetry.analysis.api.model.report.segment.SegmentIssue
import com.project.analyzer.telemetry.analysis.api.model.report.session.DrivingStyle
import com.project.analyzer.telemetry.analysis.api.model.report.session.ImprovementArea
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisReport
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionSummary
import com.project.analyzer.telemetry.analysis.api.model.report.setup.SetupCategory
import com.project.analyzer.telemetry.analysis.api.model.report.setup.SetupRecommendation
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic.SessionAnalysisSetupDiagnostic
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic.SessionAnalysisSetupDiagnosticReport
import dev.zacsweers.metro.Inject
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Aggregates detailed analytics branches into comparative, summary, and narrative report sections.
 */
@Inject
internal class ComprehensiveSessionAnalysisSummaryBuilder {

    internal fun buildComparativeAnalysis(
        report: SessionAnalysisReport,
        segmentAnalyses: List<SegmentAnalysis>,
        cornerAnalyses: List<EnhancedCornerAnalysis>,
    ): ComparativeAnalysis? {
        val validLaps = report.laps.filter { it.isValid && it.isComplete && !it.isPitLap }
        val baseLap = validLaps.minByOrNull { it.durationMs ?: Int.MAX_VALUE } ?: return null
        if (validLaps.size < 2) return null

        val biggestLosses = segmentAnalyses.filter { it.timeDelta > 0L }.sortedByDescending(
            SegmentAnalysis::timeDelta,
        ).take(5)
        return ComparativeAnalysis(
            baseLap = LapReference(baseLap.lapNumber, baseLap.durationMs?.toLong() ?: 0L, baseLap.isValid, "Best lap"),
            comparisonLaps = validLaps.filterNot { it.lapNumber == baseLap.lapNumber }.map { lap ->
                LapReference(lap.lapNumber, lap.durationMs?.toLong() ?: 0L, lap.isValid, "Lap ${lap.lapNumber}")
            },
            segmentComparisons = segmentAnalyses.map { segment ->
                SegmentComparison(
                    segment.segmentId,
                    segment.segmentName,
                    segment.timeDelta + (baseLap.durationMs?.toLong() ?: 0L),
                    baseLap.durationMs?.toLong() ?: 0L,
                    segment.timeDelta,
                    segment.issues.firstOrNull(),
                )
            },
            cornerComparisons = cornerAnalyses.map { corner ->
                com.project.analyzer.telemetry.analysis.api.model.report.comparison.CornerComparison(
                    corner.cornerNumber,
                    corner.cornerTime,
                    corner.referenceCornerTime,
                    corner.timeDelta,
                    corner.primaryIssue,
                )
            },
            biggestGains = biggestLosses.map { segment ->
                ImprovementOpportunity(
                    segment.segmentName,
                    segment.timeDelta + (baseLap.durationMs?.toLong() ?: 0L),
                    baseLap.durationMs?.toLong() ?: 0L,
                    segment.timeDelta,
                    difficultyFor(segment.timeDelta, segment.issues),
                    segment.recommendations.firstOrNull() ?: "Use the reference trace.",
                )
            },
            biggestLosses = biggestLosses.map { segment ->
                PerformanceLoss(
                    segment.segmentName,
                    segment.timeDelta,
                    segment.issues.firstOrNull()?.name?.replace('_', ' ')?.lowercase() ?: "time loss",
                    segment.recommendations.firstOrNull() ?: "Review this zone.",
                )
            },
            overallDelta = (
                validLaps.mapNotNull(
                    SessionAnalysisLap::durationMs,
                ).averageOrNull()?.roundToLong() ?: 0L
                ) - (baseLap.durationMs?.toLong() ?: 0L),
            potentialGain = biggestLosses.sumOf(SegmentAnalysis::timeDelta).coerceAtMost(2_000L),
            patternDetected = detectPattern(cornerAnalyses),
            recommendations = biggestLosses.take(3).mapNotNull { it.recommendations.firstOrNull() },
        )
    }

    internal fun buildSessionSummary(
        report: SessionAnalysisReport,
        context: SessionContext,
        segmentAnalyses: List<SegmentAnalysis>,
        cornerAnalyses: List<EnhancedCornerAnalysis>,
        tyreAnalyses: List<TyreAnalysis>,
        fuelAnalysis: FuelAnalysis,
        consistencyAnalysis: ConsistencyAnalysis,
        comparativeAnalysis: ComparativeAnalysis?,
        highlights: List<SessionAnalysisHighlight>,
    ): SessionSummary {
        val validLaps = report.laps.filter { it.isValid && it.isComplete && !it.isPitLap }
        val bestLapTime = validLaps.minByOrNull { it.durationMs ?: Int.MAX_VALUE }?.durationMs?.toLong() ?: 0L
        val avgLapTime = validLaps.mapNotNull(SessionAnalysisLap::durationMs).averageOrNull()?.roundToLong() ?: 0L
        val improvementAreas = segmentAnalyses.filter { it.timeDelta > 0L }.sortedByDescending(
            SegmentAnalysis::timeDelta,
        ).take(3).map { segment ->
            ImprovementArea(
                segment.segmentName,
                segment.recommendations.firstOrNull() ?: "Use the benchmark trace.",
                segment.timeDelta,
                if (segment.severity == IssueSeverity.CRITICAL) 0.85f else 0.7f,
            )
        }
        val tyreScore = tyreAnalyses.map {
            when (it.severity) {
                IssueSeverity.CRITICAL -> TYRE_SCORE_CRITICAL
                IssueSeverity.WARNING -> TYRE_SCORE_WARNING
                else -> TYRE_SCORE_HEALTHY
            }
        }.averageOrNull()?.roundToInt() ?: DEFAULT_TYRE_SCORE
        val overallScore = listOf(
            cornerAnalyses.map(
                EnhancedCornerAnalysis::cornerScore,
            ).averageOrNull()?.roundToInt() ?: DEFAULT_CORNER_SCORE,
            consistencyAnalysis.consistencyScore,
            tyreScore,
            (100f - (fuelAnalysis.efficiencyDelta * FUEL_EFFICIENCY_PENALTY_MULTIPLIER))
                .roundToInt()
                .coerceIn(MINIMUM_OVERALL_SCORE, 100),
        ).average().roundToInt()
        return SessionSummary(
            totalLaps = report.laps.count(SessionAnalysisLap::isComplete),
            validLaps = validLaps.size,
            bestLapTime = bestLapTime,
            avgLapTime = avgLapTime,
            consistency = consistencyAnalysis.consistencyScore,
            totalTimeLost = comparativeAnalysis?.potentialGain ?: improvementAreas.sumOf(
                ImprovementArea::potentialGain,
            ),
            potentialBestLap = (bestLapTime - (comparativeAnalysis?.potentialGain ?: 0L)).coerceAtLeast(0L),
            topStrengths = buildList {
                highlights.filter { it.severity == SessionAnalysisHighlightSeverity.Positive }.take(
                    2,
                ).map(SessionAnalysisHighlight::title).forEach(::add)
                cornerAnalyses.sortedByDescending(
                    EnhancedCornerAnalysis::cornerScore,
                ).take(2).map { "${it.cornerName} score ${it.cornerScore}" }.forEach(::add)
            }.distinct().take(3),
            topWeaknesses = segmentAnalyses.filter { it.timeDelta > 0L }.sortedByDescending(
                SegmentAnalysis::timeDelta,
            ).take(3).map { "${it.segmentName} (${it.timeDelta} ms)" },
            top3ImprovementAreas = improvementAreas,
            estimatedLapTimePotential = comparativeAnalysis?.potentialGain ?: improvementAreas.sumOf(
                ImprovementArea::potentialGain,
            ),
            drivingStyle = resolveDrivingStyle(cornerAnalyses),
            tyreManagementGrade = gradeFromScore(tyreScore),
            consistencyGrade = gradeFromScore(consistencyAnalysis.consistencyScore),
            overallGrade = gradeFromScore(overallScore),
            sessionNarrative = "${
                context.trackName.ifBlank {
                    "Session"
                }
            }: focus on ${improvementAreas.firstOrNull()?.title ?: "repeatability"}.",
        )
    }

    internal fun buildSetupRecommendations(
        setupReport: SessionAnalysisSetupDiagnosticReport?,
    ): List<SetupRecommendation> = setupReport?.diagnostics.orEmpty()
        .groupBy { diagnostic -> diagnostic.category to diagnostic.recommendation.trim() }
        .values
        .map { diagnostics ->
            val primary = diagnostics.maxByOrNull { diagnostic -> diagnostic.priorityScore() }
                ?: diagnostics.first()
            val affectedCorners = diagnostics.mapNotNull(SessionAnalysisSetupDiagnostic::cornerNumber)
                .distinct()
                .sorted()
            val affectedLapCount = diagnostics.distinctAffectedLapCount()
            SetupRecommendation(
                category = primary.category.toSetupCategory(),
                suggestedChange = primary.recommendation,
                reason = primary.toRecommendationReason(
                    diagnostics = diagnostics,
                    affectedCorners = affectedCorners,
                    affectedLapCount = affectedLapCount,
                ),
                expectedBenefit = primary.toRecommendationBenefit(affectedCorners),
                confidence = diagnostics.recommendationConfidence(),
                affectedCorners = affectedCorners,
                priority = diagnostics.resolveRecommendationPriority(),
            )
        }
        .sortedWith(
            compareByDescending<SetupRecommendation>(SetupRecommendation::confidence)
                .thenByDescending { recommendation -> recommendation.affectedCorners.size }
                .thenByDescending { recommendation -> recommendation.priority.priorityRank() },
        )

    internal fun buildNarrative(
        context: SessionContext,
        summary: SessionSummary,
        setupRecommendations: List<SetupRecommendation>,
        comparativeAnalysis: ComparativeAnalysis?,
    ): String = buildString {
        append(context.trackName.ifBlank { "Session" })
        append(": ")
        append(summary.overallGrade.name.replace('_', '+'))
        append(" overall. ")
        summary.top3ImprovementAreas.firstOrNull()?.let { area ->
            append("Largest gain is in ")
            append(area.title)
            append(" for about ")
            append(area.potentialGain)
            append(" ms. ")
        }
        comparativeAnalysis?.patternDetected?.let { pattern ->
            append("Recurring pattern: ")
            append(pattern.name.replace('_', ' ').lowercase())
            append(". ")
        }
        setupRecommendations.firstOrNull()?.let { recommendation ->
            append("Most credible setup change: ")
            append(recommendation.suggestedChange)
        }
    }

    private fun difficultyFor(deltaMs: Long, issues: List<SegmentIssue>): Difficulty = when {
        SegmentIssue.BRAKE_LOCKUP in issues || SegmentIssue.WHEEL_SPIN in issues -> Difficulty.MEDIUM
        deltaMs >= 180L -> Difficulty.HARD
        deltaMs >= 70L -> Difficulty.MEDIUM
        else -> Difficulty.EASY
    }

    private fun detectPattern(corners: List<EnhancedCornerAnalysis>): PatternType? {
        val issue = corners.mapNotNull(EnhancedCornerAnalysis::primaryIssue)
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { (_, count) -> count }
            ?.takeIf { (_, count) -> count >= 2 }
            ?.key
        return when (issue) {
            CornerIssue.LATE_BRAKING -> PatternType.CONSISTENTLY_LATE_BRAKING

            CornerIssue.EARLY_APEX -> PatternType.CONSISTENTLY_EARLY_APEX

            CornerIssue.LATE_THROTTLE,
            CornerIssue.WHEEL_SPIN_EXIT,
            -> PatternType.SLOW_EXIT_SPEED

            CornerIssue.UNDERSTEER_ENTRY -> PatternType.UNDERSTEER_ON_ENTRY

            CornerIssue.OVERSTEER_EXIT -> PatternType.OVERSTEER_ON_EXIT

            else -> null
        }
    }

    private fun resolveDrivingStyle(corners: List<EnhancedCornerAnalysis>): DrivingStyle {
        if (corners.isEmpty()) return DrivingStyle.BALANCED
        val brakeDelta = corners.map(EnhancedCornerAnalysis::brakePointDelta).averageOrNull() ?: 0f
        val smoothness = corners.map(EnhancedCornerAnalysis::throttleSmoothness).averageOrNull() ?: 0f
        val slip = corners.map { max(it.understeerRatio, it.oversteerRatio) }.averageOrNull() ?: 0f
        return when {
            brakeDelta > 0.01f || slip > 0.3f -> DrivingStyle.AGGRESSIVE
            smoothness >= 0.86f && slip < 0.18f -> DrivingStyle.SMOOTH
            brakeDelta < -0.01f -> DrivingStyle.CAUTIOUS
            else -> DrivingStyle.BALANCED
        }
    }

    private fun gradeFromScore(score: Int): Grade = when {
        score >= 95 -> Grade.A_PLUS
        score >= 88 -> Grade.A
        score >= 78 -> Grade.B
        score >= 68 -> Grade.C
        score >= 58 -> Grade.D
        else -> Grade.F
    }

    private fun SessionAnalysisHighlightCategory.toSetupCategory(): SetupCategory = when (this) {
        SessionAnalysisHighlightCategory.BrakeBalance -> SetupCategory.BRAKE_BIAS
        SessionAnalysisHighlightCategory.AeroBalance -> SetupCategory.AERO_BALANCE
        SessionAnalysisHighlightCategory.DamperIssue -> SetupCategory.DAMPER_REBOUND_FRONT
        SessionAnalysisHighlightCategory.TyrePressureImbalance -> SetupCategory.TYRE_PRESSURE_FL
        SessionAnalysisHighlightCategory.TyreTempImbalance -> SetupCategory.CAMBER_FRONT
        SessionAnalysisHighlightCategory.SetupUndersteer -> SetupCategory.ANTI_ROLL_BAR_FRONT
        SessionAnalysisHighlightCategory.SetupOversteer -> SetupCategory.ANTI_ROLL_BAR_REAR
        else -> SetupCategory.BRAKE_BIAS
    }

    private fun List<SessionAnalysisSetupDiagnostic>.recommendationConfidence(): Float = map { diagnostic ->
        when {
            diagnostic.affectedLaps.size >= 3 -> 0.9f
            diagnostic.affectedLaps.size == 2 -> 0.75f
            else -> 0.6f
        }
    }.averageOrNull() ?: 0.6f

    private fun List<SessionAnalysisSetupDiagnostic>.resolveRecommendationPriority(): Priority {
        val highestSeverity = maxByOrNull { diagnostic -> diagnostic.priorityScore() }?.severity
            ?: SessionAnalysisHighlightSeverity.Warning
        return when (highestSeverity) {
            SessionAnalysisHighlightSeverity.Critical -> Priority.HIGH
            SessionAnalysisHighlightSeverity.Warning -> Priority.MEDIUM
            SessionAnalysisHighlightSeverity.Positive -> Priority.LOW
        }
    }

    private fun List<SessionAnalysisSetupDiagnostic>.distinctAffectedLapCount(): Int = flatMap { diagnostic ->
        buildList {
            if (diagnostic.lapNumber > 0) {
                add(diagnostic.lapNumber)
            }
            addAll(diagnostic.affectedLaps.filter { lapNumber -> lapNumber > 0 })
        }
    }.distinct().size

    private fun SessionAnalysisSetupDiagnostic.toRecommendationReason(
        diagnostics: List<SessionAnalysisSetupDiagnostic>,
        affectedCorners: List<Int>,
        affectedLapCount: Int,
    ): String = buildString {
        affectedCorners.toTurnsLabel()?.let { turnsLabel ->
            append(turnsLabel)
            append(". ")
        }
        append(description)
        if (diagnostics.size > 1) {
            append(" Repeats in ")
            append(diagnostics.size)
            append(" corners.")
        }
        if (affectedLapCount > 1) {
            append(" Seen across ")
            append(affectedLapCount)
            append(" laps.")
        }
    }

    private fun SessionAnalysisSetupDiagnostic.toRecommendationBenefit(affectedCorners: List<Int>): String =
        affectedCorners.toScopedSetupTitle(title) ?: title

    private fun List<Int>.toTurnsLabel(): String? = when (size) {
        0 -> null

        1 -> "Turn ${first()}"

        2 -> "Turns ${first()} and ${last()}"

        else -> buildString {
            append("Turns ")
            append(this@toTurnsLabel.dropLast(1).joinToString(", "))
            append(" and ")
            append(this@toTurnsLabel.last())
        }
    }

    private fun List<Int>.toScopedSetupTitle(baseTitle: String): String? {
        val turnsLabel = toTurnsLabel() ?: return null
        val cleanedTitle = baseTitle
            .replace(Regex("\\b(corner|turn)s?\\s+\\d+\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .removeSuffix("in")
            .trim()
        return when {
            cleanedTitle.isBlank() -> turnsLabel
            else -> "$cleanedTitle in ${turnsLabel.lowercase()}"
        }
    }

    private fun SessionAnalysisSetupDiagnostic.priorityScore(): Int = when (severity) {
        SessionAnalysisHighlightSeverity.Critical -> 3
        SessionAnalysisHighlightSeverity.Warning -> 2
        SessionAnalysisHighlightSeverity.Positive -> 1
    }

    private fun Priority.priorityRank(): Int = when (this) {
        Priority.HIGH -> 3
        Priority.MEDIUM -> 2
        Priority.LOW -> 1
    }

    private companion object {

        private const val TYRE_SCORE_CRITICAL: Int = 45
        private const val TYRE_SCORE_WARNING: Int = 65
        private const val TYRE_SCORE_HEALTHY: Int = 85
        private const val DEFAULT_TYRE_SCORE: Int = 80
        private const val DEFAULT_CORNER_SCORE: Int = 70
        private const val FUEL_EFFICIENCY_PENALTY_MULTIPLIER: Float = 20f
        private const val MINIMUM_OVERALL_SCORE: Int = 55
    }
}
