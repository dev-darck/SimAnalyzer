package com.analyzer.session.analysis.presentation.builder.coach

import com.analyzer.session.analysis.presentation.formatter.formatDelta
import com.analyzer.session.analysis.presentation.model.SessionAnalysisCoachInsightUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisCoachTone
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlight
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun buildCoachFocus(
    selectedSamples: List<SessionAnalysisSample>,
    referenceProfile: ReferenceLapProfile,
    evaluation: SessionAnalysisCoachEvaluation,
    strings: SessionAnalysisCoachStrings,
): FocusSummary {
    val biggestLoss = resolveBiggestLossContext(
        selectedSamples = selectedSamples,
        referenceProfile = referenceProfile,
    )
    if (biggestLoss != null) {
        return FocusSummary(
            title = biggestLoss.resolvePhase(strings),
            description = strings.focusLossDescription(
                formatTrackPositionLabel(biggestLoss.sample.trackPosition, strings),
                formatDelta(biggestLoss.sample.deltaToBestMs ?: 0),
                biggestLoss.speedDelta,
            ),
        )
    }

    return when {
        evaluation.throttleTone == SessionAnalysisCoachTone.Warning -> FocusSummary(
            title = strings.exitDrive,
            description = strings.exitDriveDescription,
        )

        evaluation.brakeTone == SessionAnalysisCoachTone.Warning -> FocusSummary(
            title = strings.brakeTiming,
            description = strings.brakeTimingDescription,
        )

        evaluation.lineTone == SessionAnalysisCoachTone.Warning -> FocusSummary(
            title = strings.linePlacement,
            description = strings.linePlacementDescription,
        )

        evaluation.balanceTone == SessionAnalysisCoachTone.Warning -> FocusSummary(
            title = strings.balanceControl,
            description = strings.balanceControlDescription,
        )

        else -> FocusSummary(
            title = strings.referenceIsClose,
            description = strings.referenceIsCloseDescription,
        )
    }
}

internal fun buildCoachInsights(
    selectedSamples: List<SessionAnalysisSample>,
    referenceProfile: ReferenceLapProfile,
    evaluation: SessionAnalysisCoachEvaluation,
    strings: SessionAnalysisCoachStrings,
): List<SessionAnalysisCoachInsightUi> {
    val biggestLoss = resolveBiggestLossContext(
        selectedSamples = selectedSamples,
        referenceProfile = referenceProfile,
    )
    return listOfNotNull(
        biggestLoss?.toInsight(strings),
        buildBrakeInsight(evaluation.brakeTimingPct, strings),
        buildCornerExitInsight(evaluation, strings),
        buildLineInsight(evaluation.lineStats, strings),
        buildBalanceInsight(evaluation.balanceStats, strings),
    ).sortedByDescending(SessionAnalysisCoachInsightUi::tone)
}

internal fun buildHighlightInsights(
    highlights: List<SessionAnalysisHighlight>,
    selectedLapNumber: Int,
    strings: SessionAnalysisCoachStrings,
): List<SessionAnalysisCoachInsightUi> = highlights
    .asSequence()
    .filter { highlight ->
        highlight.lapNumber == selectedLapNumber || selectedLapNumber in highlight.affectedLaps
    }
    .filter { highlight -> highlight.recommendation.isNotBlank() }
    .sortedWith(
        compareByDescending(SessionAnalysisHighlight::priority)
            .thenByDescending { highlight -> highlight.deltaMs ?: 0 },
    )
    .map { highlight ->
        SessionAnalysisCoachInsightUi(
            title = highlight.title,
            description = buildString {
                highlight.cornerNumber?.let { cornerNumber ->
                    append(strings.cornerPrefix(cornerNumber, ""))
                }
                append(highlight.recommendation)
            },
            tone = when {
                highlight.priority >= 9 -> SessionAnalysisCoachTone.Critical
                highlight.priority >= 6 -> SessionAnalysisCoachTone.Warning
                else -> SessionAnalysisCoachTone.Neutral
            },
        )
    }
    .take(3)
    .toList()

private fun buildBrakeInsight(
    brakeTimingPct: Float?,
    strings: SessionAnalysisCoachStrings,
): SessionAnalysisCoachInsightUi? = brakeTimingPct?.let { resolvedBrakeTimingPct ->
    SessionAnalysisCoachInsightUi(
        title = strings.brakeMarkerTrend,
        description = strings.brakeTimingInsight(
            formatTrackPositionDelta(abs(resolvedBrakeTimingPct)),
            resolvedBrakeTimingPct < 0f,
        ),
        tone = if (abs(resolvedBrakeTimingPct) >= COACH_BRAKE_TIMING_WARN_PCT) {
            SessionAnalysisCoachTone.Warning
        } else {
            SessionAnalysisCoachTone.Positive
        },
    )
}

private fun buildCornerExitInsight(
    evaluation: SessionAnalysisCoachEvaluation,
    strings: SessionAnalysisCoachStrings,
): SessionAnalysisCoachInsightUi? {
    if (evaluation.throttlePickupPct == null && evaluation.apexSpeedDeltaKmh == null) return null
    return SessionAnalysisCoachInsightUi(
        title = strings.cornerExitTrend,
        description = buildString {
            evaluation.throttlePickupPct?.let { delta ->
                append(
                    if (delta < 0f) {
                        strings.throttleCommitEarlier(formatTrackPositionDelta(abs(delta)))
                    } else {
                        strings.throttleCommitLater(formatTrackPositionDelta(abs(delta)))
                    },
                )
            }
            evaluation.apexSpeedDeltaKmh?.let { delta ->
                if (isNotEmpty()) append(", ")
                append(strings.apexSpeed)
                append(" ")
                append(if (delta >= 0f) "+" else "")
                append(delta.roundToInt())
                append(" km/h")
            }
            append(".")
        },
        tone = when {
            (evaluation.throttlePickupPct ?: 0f) >= COACH_THROTTLE_WARN_PCT ||
                (evaluation.apexSpeedDeltaKmh ?: 0f) <= -4f ->
                SessionAnalysisCoachTone.Warning

            else -> SessionAnalysisCoachTone.Positive
        },
    )
}

private fun buildLineInsight(
    lineStats: LineStats,
    strings: SessionAnalysisCoachStrings,
): SessionAnalysisCoachInsightUi? {
    if (lineStats.averageGapMeters == null && lineStats.normalizedGap == null) return null
    return SessionAnalysisCoachInsightUi(
        title = strings.lineDiscipline,
        description = strings.lineGapDescription(buildLineMetricValue(lineStats)),
        tone = if ((lineStats.normalizedGap ?: Float.MAX_VALUE) >= COACH_LINE_WARN_RATIO) {
            SessionAnalysisCoachTone.Warning
        } else {
            SessionAnalysisCoachTone.Positive
        },
    )
}

private fun buildBalanceInsight(
    balanceStats: BalanceStats,
    strings: SessionAnalysisCoachStrings,
): SessionAnalysisCoachInsightUi? {
    if (balanceStats.dominantRatio == null) return null
    return SessionAnalysisCoachInsightUi(
        title = strings.balancePattern,
        description = balanceStats.description,
        tone = if (balanceStats.dominantRatio >= COACH_BALANCE_WARN_RATIO) {
            SessionAnalysisCoachTone.Warning
        } else {
            SessionAnalysisCoachTone.Positive
        },
    )
}

private data class BiggestLossContext(val sample: SessionAnalysisSample, val speedDelta: Float?)

private fun resolveBiggestLossContext(
    selectedSamples: List<SessionAnalysisSample>,
    referenceProfile: ReferenceLapProfile,
): BiggestLossContext? {
    val maxLossSample = selectedSamples.maxByOrNull { sample -> sample.deltaToBestMs ?: Int.MIN_VALUE }
        ?: return null
    if ((maxLossSample.deltaToBestMs ?: 0) < COACH_DELTA_ALERT_MS) return null
    val referenceSpeed = referenceProfile.speedAt(maxLossSample.trackPosition)
    return BiggestLossContext(
        sample = maxLossSample,
        speedDelta = if (referenceSpeed == null || maxLossSample.speedKmh == null) {
            null
        } else {
            maxLossSample.speedKmh?.minus(referenceSpeed)
        },
    )
}

private fun BiggestLossContext.resolvePhase(strings: SessionAnalysisCoachStrings): String = when {
    (sample.brake ?: 0f) >= 0.25f -> strings.entryTiming
    (sample.throttle ?: 0f) >= 0.55f -> strings.exitDrive
    else -> strings.midCornerSpeed
}

private fun BiggestLossContext.toInsight(strings: SessionAnalysisCoachStrings): SessionAnalysisCoachInsightUi =
    SessionAnalysisCoachInsightUi(
        title = strings.biggestLossZone,
        description = buildString {
            append(formatTrackPositionLabel(sample.trackPosition, strings))
            append(": ")
            append(formatDelta(sample.deltaToBestMs ?: 0))
            append(" ")
            append(strings.toReference)
            speedDelta?.let { delta ->
                append(", ")
                append(abs(delta).roundToInt())
                append(" km/h ")
                append(if (delta < 0f) strings.slower else strings.faster)
            }
            append(".")
        },
        tone = SessionAnalysisCoachTone.Critical,
    )

internal fun formatTrackPositionLabel(trackPosition: Float?, strings: SessionAnalysisCoachStrings): String =
    trackPosition
        ?.let { value -> "${(value.coerceIn(0f, 1f) * 100f).roundToInt()}% lap" }
        ?: strings.thisSection
