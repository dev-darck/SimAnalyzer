@file:Suppress(
    "LongMethod",
    "MaximumLineLength",
)

package com.analyzer.session.analysis.presentation.builder.diagnostic

import com.analyzer.session.analysis.presentation.model.CornerScoreUi
import com.analyzer.session.analysis.presentation.model.DiagnosticIssueUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisDiagnosticSummaryUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisLapSummaryUi
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightSeverity
import com.project.analyzer.utils.ext.averageOrNull
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.abs
import kotlin.math.roundToInt

private const val diagnosticPenaltyScale: Float = 160f
private const val diagnosticPenaltyDecay: Float = 0.76f
private const val diagnosticPenaltyDepth: Int = 6
private const val cornerPenaltyScale: Float = 78f
private const val cornerPenaltyDepth: Int = 4
private const val cornerPenaltyDecay: Float = 0.88f

/**
 * Collapses raw highlights into stable driving and setup issues, plus per-corner scores, so the UI
 * can surface root causes instead of repeating every individual telemetry event.
 */
internal fun buildDiagnosticSummary(
    highlights: List<SessionAnalysisHighlightUi>,
): SessionAnalysisDiagnosticSummaryUi? {
    val rankedHighlights = highlights
        .filterNot(SessionAnalysisHighlightUi::isPositiveSignal)
        .sortedWith(
            compareByDescending<SessionAnalysisHighlightUi>(SessionAnalysisHighlightUi::priority)
                .thenByDescending { highlight -> highlight.deltaMs ?: 0 }
                .thenByDescending { highlight -> highlight.severity.rank() },
        )
    if (rankedHighlights.isEmpty()) return null

    val drivingHighlights = rankedHighlights.filter { highlight ->
        highlight.diagnosisSource != SessionAnalysisDiagnosisSource.CarSetup
    }
    val setupHighlights = rankedHighlights.filter(SessionAnalysisHighlightUi::isSetupRelevant)
    val drivingScore = resolveDiagnosticScore(
        highlights = drivingHighlights,
        contribution = { highlight ->
            if (highlight.diagnosisSource == SessionAnalysisDiagnosisSource.Mixed) 0.55f else 1f
        },
    )
    val setupScore = resolveDiagnosticScore(
        highlights = setupHighlights,
        contribution = { highlight ->
            if (highlight.diagnosisSource == SessionAnalysisDiagnosisSource.Mixed) 0.55f else 1f
        },
    )
    val overallScore = ((drivingScore * 0.6f) + (setupScore * 0.4f)).roundToInt().coerceIn(0, 100)

    return SessionAnalysisDiagnosticSummaryUi(
        overallScore = overallScore,
        drivingScore = drivingScore,
        setupScore = setupScore,
        topDrivingIssues = drivingHighlights
            .toDrivingDiagnosticIssues()
            .take(3)
            .toImmutableList(),
        topSetupIssues = setupHighlights
            .toSetupDiagnosticIssues()
            .take(4)
            .toImmutableList(),
        cornerScores = rankedHighlights
            .filter { highlight -> highlight.cornerNumber != null }
            .groupBy { highlight -> highlight.cornerNumber!! }
            .mapNotNull { (cornerNumber, cornerHighlights) ->
                val issue = cornerHighlights.toDrivingDiagnosticIssue() ?: return@mapNotNull null
                val score = cornerHighlights
                    .mapNotNull(SessionAnalysisHighlightUi::score)
                    .minOrNull()
                    ?: computeCornerScore(cornerHighlights)
                CornerScoreUi(
                    cornerNumber = cornerNumber,
                    score = score.coerceIn(0, 100),
                    trackPosition = cornerHighlights.mapNotNull(SessionAnalysisHighlightUi::trackPosition)
                        .averageOrNull()
                        ?: return@mapNotNull null,
                    mainIssue = issue.title,
                    detail = issue.description,
                    timeVsReferenceMs = cornerHighlights.mapNotNull(SessionAnalysisHighlightUi::deltaMs).maxOrNull()
                        ?: 0,
                    recommendation = issue.recommendation,
                    source = issue.source,
                    category = issue.category,
                )
            }
            .sortedWith(
                compareBy<CornerScoreUi>(CornerScoreUi::score)
                    .thenByDescending(CornerScoreUi::timeVsReferenceMs)
                    .thenBy(CornerScoreUi::cornerNumber),
            )
            .toImmutableList(),
    )
}

internal fun applyLapDiagnosticScores(
    laps: List<SessionAnalysisLapSummaryUi>,
    highlights: List<SessionAnalysisHighlightUi>,
): List<SessionAnalysisLapSummaryUi> {
    if (laps.isEmpty()) return emptyList()
    return laps.map { lap ->
        if (!lap.isValid || !lap.isComplete || lap.isPitLap) {
            lap.copy(diagnosticScore = null)
        } else {
            val lapHighlights = highlights.filter { highlight ->
                highlight.lapNumber == lap.lapNumber || lap.lapNumber in highlight.affectedLaps
            }
            lap.copy(
                diagnosticScore = if (lapHighlights.isEmpty()) {
                    100
                } else {
                    computeCornerScore(lapHighlights)
                },
            )
        }
    }
}

private fun List<SessionAnalysisHighlightUi>.toDrivingDiagnosticIssues(): List<DiagnosticIssueUi> =
    groupBy(SessionAnalysisHighlightUi::drivingDiagnosticGroupKey)
        .values
        .mapNotNull(List<SessionAnalysisHighlightUi>::toDrivingDiagnosticIssue)
        .sortedWith(
            compareByDescending<DiagnosticIssueUi>(DiagnosticIssueUi::potentialTimeGainMs)
                .thenByDescending(DiagnosticIssueUi::priority)
                .thenBy { issue -> issue.cornerNumber ?: Int.MAX_VALUE },
        )

private fun List<SessionAnalysisHighlightUi>.toSetupDiagnosticIssues(): List<DiagnosticIssueUi> =
    groupBy(SessionAnalysisHighlightUi::setupDiagnosticGroupKey)
        .values
        .mapNotNull(List<SessionAnalysisHighlightUi>::toSetupDiagnosticIssue)
        .sortedWith(
            compareByDescending<DiagnosticIssueUi>(DiagnosticIssueUi::potentialTimeGainMs)
                .thenByDescending { issue -> issue.source.setupPriorityRank() }
                .thenByDescending(DiagnosticIssueUi::priority)
                .thenBy { issue -> issue.cornerNumber ?: Int.MAX_VALUE },
        )

private fun List<SessionAnalysisHighlightUi>.toDrivingDiagnosticIssue(): DiagnosticIssueUi? {
    if (isEmpty()) return null
    val rootCause = primaryRootCauseHighlight()
    val impact = primaryImpactHighlight()
    val affectedLapCount = distinctAffectedLapCount()
    return DiagnosticIssueUi(
        title = rootCause.drivingIssueTitle(),
        description = rootCause.toDrivingDiagnosticDescription(
            impact = impact,
            affectedLapCount = affectedLapCount,
        ),
        recommendation = rootCause.recommendation.ifBlank { impact.recommendation },
        priority = maxOf(rootCause.priority, impact.priority),
        source = rootCause.diagnosisSource,
        potentialTimeGainMs = maxOf(
            rootCause.potentialTimeGainMs(),
            impact.potentialTimeGainMs(),
        ),
        category = rootCause.category,
        cornerNumber = rootCause.cornerNumber ?: impact.cornerNumber,
        trackPosition = rootCause.trackPosition
            ?: impact.trackPosition
            ?: mapNotNull(SessionAnalysisHighlightUi::trackPosition).averageOrNull(),
    )
}

private fun List<SessionAnalysisHighlightUi>.toSetupDiagnosticIssue(): DiagnosticIssueUi? {
    if (isEmpty()) return null
    val rootCause = primaryRootCauseHighlight()
    val impact = primaryImpactHighlight()
    val affectedTurns = mapNotNull(SessionAnalysisHighlightUi::cornerNumber).distinct().sorted()
    val affectedLapCount = distinctAffectedLapCount()
    return DiagnosticIssueUi(
        title = rootCause.toSetupIssueTitle(affectedTurns),
        description = rootCause.toSetupDiagnosticDescription(
            impact = impact,
            affectedTurns = affectedTurns,
            affectedLapCount = affectedLapCount,
        ),
        recommendation = rootCause.recommendation.ifBlank { impact.recommendation },
        priority = maxOf(rootCause.priority, impact.priority),
        source = rootCause.diagnosisSource,
        potentialTimeGainMs = mapNotNull(SessionAnalysisHighlightUi::deltaMs).maxOrNull()
            ?: maxOf(rootCause.potentialTimeGainMs(), impact.potentialTimeGainMs()),
        category = rootCause.category,
        cornerNumber = affectedTurns.firstOrNull() ?: rootCause.cornerNumber ?: impact.cornerNumber,
        trackPosition = rootCause.trackPosition
            ?: impact.trackPosition
            ?: mapNotNull(SessionAnalysisHighlightUi::trackPosition).averageOrNull(),
    )
}

private fun List<SessionAnalysisHighlightUi>.primaryRootCauseHighlight(): SessionAnalysisHighlightUi = maxWithOrNull(
    compareBy<SessionAnalysisHighlightUi> { highlight -> highlight.category.rootCauseRank() }
        .thenBy { highlight -> highlight.priority }
        .thenBy { highlight -> highlight.potentialTimeGainMs() }
        .thenBy { highlight -> highlight.severity.rank() },
) ?: first()

private fun List<SessionAnalysisHighlightUi>.primaryImpactHighlight(): SessionAnalysisHighlightUi = maxWithOrNull(
    compareBy<SessionAnalysisHighlightUi> { highlight -> highlight.potentialTimeGainMs() }
        .thenBy { highlight -> highlight.priority }
        .thenBy { highlight -> highlight.severity.rank() },
) ?: first()

private fun SessionAnalysisHighlightUi.drivingDiagnosticGroupKey(): String =
    cornerNumber?.let { cornerNumber -> "corner-$cornerNumber" }
        ?: id.ifBlank { "${category.name}-$lapNumber-$title" }

private fun SessionAnalysisHighlightUi.setupDiagnosticGroupKey(): String = buildString {
    append(diagnosisSource.name)
    append(':')
    append(category.name)
    append(':')
    append(
        recommendation.ifBlank {
            title
                .lowercase()
                .replace(Regex("\\bcorner\\s+\\d+\\b"), "")
                .replace(Regex("\\bturn\\s+\\d+\\b"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
        },
    )
}

private fun SessionAnalysisHighlightUi.isSetupRelevant(): Boolean = when (category) {
    SessionAnalysisHighlightCategory.SetupUndersteer,
    SessionAnalysisHighlightCategory.SetupOversteer,
    SessionAnalysisHighlightCategory.TyrePressureImbalance,
    SessionAnalysisHighlightCategory.TyreTempImbalance,
    SessionAnalysisHighlightCategory.TyreOverheat,
    SessionAnalysisHighlightCategory.TyreCold,
    SessionAnalysisHighlightCategory.BrakeBalance,
    SessionAnalysisHighlightCategory.AeroBalance,
    SessionAnalysisHighlightCategory.DamperIssue,
        -> diagnosisSource != SessionAnalysisDiagnosisSource.DrivingStyle

    SessionAnalysisHighlightCategory.WheelLockup,
    SessionAnalysisHighlightCategory.Understeer,
    SessionAnalysisHighlightCategory.Oversteer,
        -> diagnosisSource == SessionAnalysisDiagnosisSource.Mixed ||
        diagnosisSource == SessionAnalysisDiagnosisSource.CarSetup

    else -> diagnosisSource == SessionAnalysisDiagnosisSource.CarSetup
}

private fun SessionAnalysisHighlightUi.toDrivingDiagnosticDescription(
    impact: SessionAnalysisHighlightUi,
    affectedLapCount: Int,
): String {
    val impactGainMs = impact
        .takeUnless { resolvedImpact ->
            resolvedImpact.deltaMs == null || resolvedImpact.deltaMs == deltaMs
        }
        ?.potentialTimeGainMs()
    return buildString {
        append(description.trim())
        appendDiagnosticEvidence(
            impactGainMs = impactGainMs,
            affectedLapCount = affectedLapCount,
        )
    }
}

private fun SessionAnalysisHighlightUi.drivingIssueTitle(): String {
    val turnLabel = cornerNumber?.let { corner -> "Turn $corner: " }.orEmpty()
    return when {
        category == SessionAnalysisHighlightCategory.TrailBrakingMissing ->
            "${turnLabel}brake release ends too early".trim()

        category == SessionAnalysisHighlightCategory.BrakePoint ->
            "${turnLabel}braking point is inconsistent".trim()

        category == SessionAnalysisHighlightCategory.WheelLockup ->
            "${turnLabel}front tyre locks on entry".trim()

        category == SessionAnalysisHighlightCategory.EarlyApexEntry ->
            "${turnLabel}apex comes too early".trim()

        category == SessionAnalysisHighlightCategory.LateApexEntry ->
            "${turnLabel}apex comes too late".trim()

        category == SessionAnalysisHighlightCategory.InconsistentLine ->
            "${turnLabel}line is not repeatable".trim()

        category == SessionAnalysisHighlightCategory.CoastingZone ->
            "${turnLabel}there is a coasting phase before throttle".trim()

        category == SessionAnalysisHighlightCategory.WheelSpin ->
            "${turnLabel}rear tyres spin on exit".trim()

        category == SessionAnalysisHighlightCategory.ThrottleCommitment ->
            "${turnLabel}throttle comes in too late".trim()

        category == SessionAnalysisHighlightCategory.Understeer ->
            "${turnLabel}front pushes wide".trim()

        category == SessionAnalysisHighlightCategory.Oversteer ->
            "${turnLabel}rear gets unstable".trim()

        category != SessionAnalysisHighlightCategory.TimeLoss ->
            title

        recommendation.contains("brake", ignoreCase = true) ||
            description.contains("entry", ignoreCase = true) ||
            description.contains("trail", ignoreCase = true) ->
            "${turnLabel}entry braking is costing time".trim()

        recommendation.contains("turn-in", ignoreCase = true) ||
            recommendation.contains("apex", ignoreCase = true) ||
            description.contains("apex", ignoreCase = true) ->
            "${turnLabel}mid-corner line is costing time".trim()

        recommendation.contains("throttle", ignoreCase = true) ||
            description.contains("exit", ignoreCase = true) ->
            "${turnLabel}exit throttle timing is costing time".trim()

        cornerNumber != null -> "Turn $cornerNumber is losing time versus reference"

        else -> "Primary time-loss zone"
    }
}

private fun SessionAnalysisHighlightUi.toSetupDiagnosticDescription(
    impact: SessionAnalysisHighlightUi,
    affectedTurns: List<Int>,
    affectedLapCount: Int,
): String = buildString {
    affectedTurns.toTurnsLabel()?.let { turnsLabel ->
        append(turnsLabel)
        append(". ")
    }
    append(description.trim())
    appendDiagnosticEvidence(
        impactGainMs = impact.potentialTimeGainMs().takeIf { gain -> gain > 0 },
        affectedLapCount = affectedLapCount,
    )
}

private fun StringBuilder.appendDiagnosticEvidence(impactGainMs: Int?, affectedLapCount: Int) {
    impactGainMs?.takeIf { gain -> gain > 0 }?.let { gain ->
        append(" Peak loss is ")
        append(gain)
        append(" ms.")
    }
    if (affectedLapCount > 1) {
        append(" Seen across ")
        append(affectedLapCount)
        append(" laps.")
    }
}

private fun SessionAnalysisHighlightUi.toSetupIssueTitle(affectedTurns: List<Int>): String = when (category) {
    SessionAnalysisHighlightCategory.SetupUndersteer ->
        affectedTurns.toTurnScopedTitle(base = "Car pushes wide") ?: "Car pushes wide"

    SessionAnalysisHighlightCategory.SetupOversteer ->
        affectedTurns.toTurnScopedTitle(base = "Rear is unstable") ?: "Rear is unstable"

    SessionAnalysisHighlightCategory.Understeer ->
        affectedTurns.toTurnScopedTitle(base = "Car still pushes wide") ?: "Car still pushes wide"

    SessionAnalysisHighlightCategory.Oversteer ->
        affectedTurns.toTurnScopedTitle(base = "Rear is still unstable") ?: "Rear is still unstable"

    SessionAnalysisHighlightCategory.AeroBalance,
    SessionAnalysisHighlightCategory.BrakeBalance,
    SessionAnalysisHighlightCategory.TyrePressureImbalance,
    SessionAnalysisHighlightCategory.TyreTempImbalance,
    SessionAnalysisHighlightCategory.DamperIssue,
    SessionAnalysisHighlightCategory.WheelLockup,
        -> title

    else -> title
}

private fun SessionAnalysisDiagnosisSource.setupPriorityRank(): Int = when (this) {
    SessionAnalysisDiagnosisSource.CarSetup -> 3
    SessionAnalysisDiagnosisSource.Mixed -> 2
    SessionAnalysisDiagnosisSource.DrivingStyle -> 1
}

private fun List<SessionAnalysisHighlightUi>.distinctAffectedLapCount(): Int = flatMap { highlight ->
    buildList {
        if (highlight.lapNumber > 0) add(highlight.lapNumber)
        addAll(highlight.affectedLaps.filter { lapNumber -> lapNumber > 0 })
    }
}.distinct().size

private fun List<Int>.toTurnsLabel(prefix: String = "Turns "): String? {
    if (isEmpty()) return null
    val turns = this
    return buildString {
        when (turns.size) {
            1 -> {
                if (prefix == "Turns ") {
                    append("Turn ")
                } else {
                    append(prefix)
                }
                append(turns.first())
            }

            2 -> {
                append(prefix)
                append(turns.first())
                append(" and ")
                append(turns.last())
            }

            else -> {
                append(prefix)
                turns.dropLast(1).forEachIndexed { index, turn ->
                    if (index > 0) append(", ")
                    append(turn)
                }
                append(" and ")
                append(turns.last())
            }
        }
    }
}

private fun List<Int>.toTurnScopedTitle(base: String): String? {
    if (isEmpty()) return null
    val turns = this
    return buildString {
        append(base)
        append(" in ")
        if (turns.size == 1) {
            append("turn ")
            append(turns.first())
        } else {
            append("turns ")
            turns.dropLast(1).forEachIndexed { index, turn ->
                if (index > 0) append(", ")
                append(turn)
            }
            append(" and ")
            append(turns.last())
        }
    }
}

private fun SessionAnalysisHighlightUi.potentialTimeGainMs(): Int = abs(deltaMs ?: (priority * 22))

private fun SessionAnalysisHighlightCategory.rootCauseRank(): Int = when (this) {
    SessionAnalysisHighlightCategory.SetupUndersteer,
    SessionAnalysisHighlightCategory.SetupOversteer,
    SessionAnalysisHighlightCategory.TyrePressureImbalance,
    SessionAnalysisHighlightCategory.TyreTempImbalance,
    SessionAnalysisHighlightCategory.BrakeBalance,
    SessionAnalysisHighlightCategory.AeroBalance,
    SessionAnalysisHighlightCategory.DamperIssue,
    SessionAnalysisHighlightCategory.TrailBrakingMissing,
    SessionAnalysisHighlightCategory.EarlyApexEntry,
    SessionAnalysisHighlightCategory.LateApexEntry,
    SessionAnalysisHighlightCategory.CoastingZone,
    SessionAnalysisHighlightCategory.WheelLockup,
    SessionAnalysisHighlightCategory.WheelSpin,
    SessionAnalysisHighlightCategory.InconsistentLine,
        -> 4

    SessionAnalysisHighlightCategory.Understeer,
    SessionAnalysisHighlightCategory.Oversteer,
    SessionAnalysisHighlightCategory.BrakePoint,
    SessionAnalysisHighlightCategory.ThrottleCommitment,
    SessionAnalysisHighlightCategory.TyreOverheat,
    SessionAnalysisHighlightCategory.TyreCold,
        -> 3

    SessionAnalysisHighlightCategory.TimeLoss -> 1

    SessionAnalysisHighlightCategory.TopSpeed -> 0
}

private fun resolveDiagnosticScore(
    highlights: List<SessionAnalysisHighlightUi>,
    contribution: (SessionAnalysisHighlightUi) -> Float,
): Int {
    if (highlights.isEmpty()) return 100
    val penalty = aggregatePenalty(
        highlights = highlights,
        depth = diagnosticPenaltyDepth,
        decayStep = diagnosticPenaltyDecay,
        contribution = contribution,
    )
    return penaltyToScore(
        penalty = penalty,
        scale = diagnosticPenaltyScale,
    )
}

private fun computeCornerScore(highlights: List<SessionAnalysisHighlightUi>): Int {
    val penalty = aggregatePenalty(
        highlights = highlights,
        depth = cornerPenaltyDepth,
        decayStep = cornerPenaltyDecay,
        contribution = { 1f },
    )
    return penaltyToScore(
        penalty = penalty,
        scale = cornerPenaltyScale,
    )
}

private fun SessionAnalysisHighlightUi.penaltyWeight(): Float {
    val deltaWeight = ((deltaMs ?: 0).coerceAtLeast(0) / 32f).coerceAtMost(28f)
    val priorityWeight = priority * 4.8f
    val severityWeight = when (severity) {
        SessionAnalysisHighlightSeverity.Positive -> 0f
        SessionAnalysisHighlightSeverity.Warning -> 1f
        SessionAnalysisHighlightSeverity.Critical -> 1.25f
    }
    return (priorityWeight + deltaWeight) * severityWeight
}

private fun SessionAnalysisHighlightUi.isPositiveSignal(): Boolean =
    severity == SessionAnalysisHighlightSeverity.Positive && priority <= 2

private fun aggregatePenalty(
    highlights: List<SessionAnalysisHighlightUi>,
    depth: Int,
    decayStep: Float,
    contribution: (SessionAnalysisHighlightUi) -> Float,
): Float {
    var decay = 1f
    var total = 0f
    highlights
        .sortedByDescending(SessionAnalysisHighlightUi::penaltyWeight)
        .take(depth)
        .forEach { highlight ->
            total += highlight.penaltyWeight() * contribution(highlight) * decay
            decay *= decayStep
        }
    return total
}

private fun penaltyToScore(penalty: Float, scale: Float): Int {
    if (penalty <= 0f) return 100
    val normalizedPenalty = penalty / (penalty + scale)
    return ((1f - normalizedPenalty) * 100f)
        .roundToInt()
        .coerceIn(0, 100)
}

private fun SessionAnalysisHighlightSeverity.rank(): Int = when (this) {
    SessionAnalysisHighlightSeverity.Positive -> 1
    SessionAnalysisHighlightSeverity.Warning -> 2
    SessionAnalysisHighlightSeverity.Critical -> 3
}
