package com.analyzer.session.analysis.presentation.builder.coach

import com.analyzer.session.analysis.presentation.model.SessionAnalysisCoachMetricUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisCoachTone
import com.analyzer.session.analysis.presentation.model.SessionAnalysisDiagnosticSummaryUi
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.utils.ext.averageOrNull
import kotlin.math.abs
import kotlin.math.roundToInt

internal data class SessionAnalysisCoachEvaluation(
    val brakeTimingPct: Float?,
    val throttlePickupPct: Float?,
    val apexSpeedDeltaKmh: Float?,
    val lineStats: LineStats,
    val balanceStats: BalanceStats,
    val trailBrakingScore: Int?,
    val coastingScore: Int?,
    val tyreManagementScore: Int?,
    val consistencyScore: Int?,
    val setupConfidence: Int,
    val brakeTone: SessionAnalysisCoachTone,
    val throttleTone: SessionAnalysisCoachTone,
    val lineTone: SessionAnalysisCoachTone,
    val balanceTone: SessionAnalysisCoachTone,
)

internal data class SessionAnalysisCoachEvaluationInput(
    val selectedSamples: List<SessionAnalysisSample>,
    val referenceSamples: List<SessionAnalysisSample>,
    val segmentSamples: List<SessionAnalysisSample>,
    val referenceProfile: ReferenceLapProfile,
    val trackMap: SessionAnalysisTrackMap?,
    val diagnosticSummary: SessionAnalysisDiagnosticSummaryUi?,
)

private data class SessionAnalysisMatchedZoneStats(
    val brakeTimingPct: Float?,
    val throttlePickupPct: Float?,
    val apexSpeedDeltaKmh: Float?,
)

internal fun evaluateLapCoach(
    input: SessionAnalysisCoachEvaluationInput,
    strings: SessionAnalysisCoachStrings,
): SessionAnalysisCoachEvaluation {
    val matchedZoneStats = resolveMatchedZoneStats(
        selectedSamples = input.selectedSamples,
        referenceSamples = input.referenceSamples,
    )
    val lineStats = computeLineStats(
        samples = input.selectedSamples,
        referenceProfile = input.referenceProfile,
        trackMap = input.trackMap,
    )
    val balanceStats = computeBalanceStats(
        selectedSamples = input.selectedSamples,
        referenceSamples = input.referenceSamples,
        strings = strings,
    )
    val consistencyScore = resolveConsistencyScore(input.diagnosticSummary)
    val setupConfidence = resolveSetupConfidence(
        diagnosticSummary = input.diagnosticSummary,
        balanceStats = balanceStats,
    )
    return SessionAnalysisCoachEvaluation(
        brakeTimingPct = matchedZoneStats.brakeTimingPct,
        throttlePickupPct = matchedZoneStats.throttlePickupPct,
        apexSpeedDeltaKmh = matchedZoneStats.apexSpeedDeltaKmh,
        lineStats = lineStats,
        balanceStats = balanceStats,
        trailBrakingScore = extractBrakeZones(input.selectedSamples).trailBrakingScore(),
        coastingScore = computeCoastingScore(input.selectedSamples),
        tyreManagementScore = computeTyreManagementScore(input.segmentSamples),
        consistencyScore = consistencyScore,
        setupConfidence = setupConfidence,
        brakeTone = resolveBrakeTone(matchedZoneStats.brakeTimingPct),
        throttleTone = resolveThrottleTone(
            throttlePickupPct = matchedZoneStats.throttlePickupPct,
            apexSpeedDeltaKmh = matchedZoneStats.apexSpeedDeltaKmh,
        ),
        lineTone = resolveLineTone(lineStats),
        balanceTone = resolveBalanceTone(balanceStats),
    )
}

internal fun buildCoachMetrics(
    evaluation: SessionAnalysisCoachEvaluation,
    strings: SessionAnalysisCoachStrings,
): List<SessionAnalysisCoachMetricUi> = buildList {
    add(
        SessionAnalysisCoachMetricUi(
            label = strings.brakeTiming,
            value = formatTrackPositionDelta(evaluation.brakeTimingPct),
            tone = evaluation.brakeTone,
        ),
    )
    add(
        SessionAnalysisCoachMetricUi(
            label = strings.throttlePickup,
            value = buildThrottleMetricValue(
                throttlePickupPct = evaluation.throttlePickupPct,
                apexSpeedDeltaKmh = evaluation.apexSpeedDeltaKmh,
            ),
            tone = evaluation.throttleTone,
        ),
    )
    add(
        SessionAnalysisCoachMetricUi(
            label = strings.lineDeviation,
            value = buildLineMetricValue(evaluation.lineStats),
            tone = evaluation.lineTone,
        ),
    )
    add(
        SessionAnalysisCoachMetricUi(
            label = strings.balance,
            value = evaluation.balanceStats.label,
            tone = evaluation.balanceTone,
        ),
    )
    addScoreMetric(strings.trailBraking, evaluation.trailBrakingScore, 58, 78)
    addScoreMetric(strings.coasting, evaluation.coastingScore, 62, 82)
    addScoreMetric(strings.tyreManagement, evaluation.tyreManagementScore, 60, 82)
    addScoreMetric(strings.consistency, evaluation.consistencyScore, 65, 84)
    addScoreMetric(strings.setupConfidence, evaluation.setupConfidence, 62, 82)
}

internal fun scoreTone(score: Int?, warnThreshold: Int, goodThreshold: Int): SessionAnalysisCoachTone = when {
    score == null -> SessionAnalysisCoachTone.Neutral
    score < warnThreshold -> SessionAnalysisCoachTone.Warning
    score >= goodThreshold -> SessionAnalysisCoachTone.Positive
    else -> SessionAnalysisCoachTone.Neutral
}

private fun resolveMatchedZoneStats(
    selectedSamples: List<SessionAnalysisSample>,
    referenceSamples: List<SessionAnalysisSample>,
): SessionAnalysisMatchedZoneStats {
    val matchedZones = matchZones(
        selectedZones = extractBrakeZones(selectedSamples),
        referenceZones = extractBrakeZones(referenceSamples),
    )
    return SessionAnalysisMatchedZoneStats(
        brakeTimingPct = matchedZones
            .map { match -> match.selected.startTrackPosition - match.reference.startTrackPosition }
            .averageOrNull(),
        throttlePickupPct = matchedZones.mapNotNull { match ->
            val selectedPickup = match.selected.fullThrottleTrackPosition
            val referencePickup = match.reference.fullThrottleTrackPosition
            if (selectedPickup == null || referencePickup == null) null else selectedPickup - referencePickup
        }.averageOrNull(),
        apexSpeedDeltaKmh = matchedZones.mapNotNull { match ->
            val selectedSpeed = match.selected.minimumSpeedKmh
            val referenceSpeed = match.reference.minimumSpeedKmh
            if (selectedSpeed == null || referenceSpeed == null) null else selectedSpeed - referenceSpeed
        }.averageOrNull(),
    )
}

private fun resolveConsistencyScore(diagnosticSummary: SessionAnalysisDiagnosticSummaryUi?): Int? =
    diagnosticSummary?.cornerScores
        ?.map { corner -> corner.score.toFloat() }
        ?.takeIf(List<Float>::isNotEmpty)
        ?.average()
        ?.roundToInt()

private fun resolveSetupConfidence(
    diagnosticSummary: SessionAnalysisDiagnosticSummaryUi?,
    balanceStats: BalanceStats,
): Int = diagnosticSummary?.setupScore
    ?: ((1f - (balanceStats.dominantRatio ?: 0f)) * 100f).roundToInt()

private fun resolveBrakeTone(brakeTimingPct: Float?): SessionAnalysisCoachTone = when {
    brakeTimingPct == null -> SessionAnalysisCoachTone.Neutral
    abs(brakeTimingPct) >= COACH_BRAKE_TIMING_WARN_PCT -> SessionAnalysisCoachTone.Warning
    abs(brakeTimingPct) <= COACH_BRAKE_TIMING_WARN_PCT * 0.4f -> SessionAnalysisCoachTone.Positive
    else -> SessionAnalysisCoachTone.Neutral
}

private fun resolveThrottleTone(throttlePickupPct: Float?, apexSpeedDeltaKmh: Float?): SessionAnalysisCoachTone = when {
    throttlePickupPct == null && apexSpeedDeltaKmh == null -> SessionAnalysisCoachTone.Neutral

    (throttlePickupPct ?: 0f) >= COACH_THROTTLE_WARN_PCT || (apexSpeedDeltaKmh ?: 0f) <= -4f ->
        SessionAnalysisCoachTone.Warning

    (throttlePickupPct ?: Float.MAX_VALUE) <= COACH_THROTTLE_WARN_PCT * 0.35f &&
        (apexSpeedDeltaKmh ?: -1f) >= 0f ->
        SessionAnalysisCoachTone.Positive

    else -> SessionAnalysisCoachTone.Neutral
}

private fun resolveLineTone(lineStats: LineStats): SessionAnalysisCoachTone = when {
    lineStats.normalizedGap == null && lineStats.averageGapMeters == null -> SessionAnalysisCoachTone.Neutral

    (lineStats.normalizedGap ?: Float.MAX_VALUE) >= COACH_LINE_WARN_RATIO ||
        (lineStats.averageGapMeters ?: 0f) >= 1.5f ->
        SessionAnalysisCoachTone.Warning

    (lineStats.normalizedGap ?: Float.MAX_VALUE) <= COACH_LINE_GOOD_RATIO -> SessionAnalysisCoachTone.Positive

    else -> SessionAnalysisCoachTone.Neutral
}

private fun resolveBalanceTone(balanceStats: BalanceStats): SessionAnalysisCoachTone = when {
    balanceStats.dominantRatio == null -> SessionAnalysisCoachTone.Neutral

    balanceStats.dominantRatio >= COACH_BALANCE_WARN_RATIO &&
        balanceStats.deltaToReference >= 0.08f ->
        SessionAnalysisCoachTone.Warning

    balanceStats.dominantRatio <= 0.12f -> SessionAnalysisCoachTone.Positive

    else -> SessionAnalysisCoachTone.Neutral
}

private fun MutableList<SessionAnalysisCoachMetricUi>.addScoreMetric(
    label: String,
    score: Int?,
    warnThreshold: Int,
    goodThreshold: Int,
) {
    add(
        SessionAnalysisCoachMetricUi(
            label = label,
            value = score?.let { resolvedScore -> "$resolvedScore/100" } ?: "--",
            tone = scoreTone(
                score = score,
                warnThreshold = warnThreshold,
                goodThreshold = goodThreshold,
            ),
        ),
    )
}

private fun computeCoastingScore(samples: List<SessionAnalysisSample>): Int? {
    val loadedSamples = samples.filter { sample ->
        (sample.speedKmh ?: 0f) >= 50f
    }
    if (loadedSamples.isEmpty()) return null
    val idleRatio = loadedSamples.count { sample ->
        (sample.throttle ?: 0f) <= 0.08f &&
            (sample.brake ?: 0f) <= 0.08f
    }.toFloat() / loadedSamples.size.toFloat()
    return (100f - idleRatio * 150f).roundToInt().coerceIn(0, 100)
}

private fun computeTyreManagementScore(segmentSamples: List<SessionAnalysisSample>): Int? {
    val lapAverages = segmentSamples
        .groupBy(SessionAnalysisSample::lapNumber)
        .filterKeys { lapNumber -> lapNumber > 0 }
        .mapNotNull { (_, lapSamples) ->
            lapSamples.flatMap { sample ->
                listOfNotNull(
                    sample.tyreFl?.coreTempC,
                    sample.tyreFr?.coreTempC,
                    sample.tyreRl?.coreTempC,
                    sample.tyreRr?.coreTempC,
                )
            }.averageOrNull()
        }
    if (lapAverages.size < 2) return null
    val stdDev = lapAverages.standardDeviation() ?: return null
    return (100f - stdDev * 12f).roundToInt().coerceIn(0, 100)
}

internal fun buildThrottleMetricValue(throttlePickupPct: Float?, apexSpeedDeltaKmh: Float?): String = buildString {
    if (throttlePickupPct == null && apexSpeedDeltaKmh == null) {
        append("--")
        return@buildString
    }
    throttlePickupPct?.let { delta ->
        append(formatTrackPositionDelta(delta))
    }
    apexSpeedDeltaKmh?.let { delta ->
        if (isNotEmpty()) append("  /  ")
        append(if (delta >= 0f) "+" else "")
        append(delta.roundToInt())
        append(" km/h")
    }
}

internal fun buildLineMetricValue(lineStats: LineStats): String = when {
    lineStats.normalizedGap != null -> "${(lineStats.normalizedGap * 100f).roundToInt()}% of half-width"
    lineStats.averageGapMeters != null -> "${"%.1f".format(lineStats.averageGapMeters)} m"
    else -> "--"
}

internal fun formatTrackPositionDelta(deltaTrackPosition: Float?): String = deltaTrackPosition
    ?.let { delta -> "${"%.1f".format(abs(delta) * 100f)}% lap" }
    ?: "--"

private fun List<Float>.standardDeviation(): Float? {
    if (size < 2) return null
    val mean = average()
    val variance = sumOf { value ->
        val delta = value - mean
        delta * delta
    } / size.toDouble()
    return kotlin.math.sqrt(variance).toFloat()
}
