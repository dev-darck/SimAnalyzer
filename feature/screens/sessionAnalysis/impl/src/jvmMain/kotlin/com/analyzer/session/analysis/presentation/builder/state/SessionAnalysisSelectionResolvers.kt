package com.analyzer.session.analysis.presentation.builder.state

import com.analyzer.session.analysis.presentation.builder.lap.resolveReferenceLap
import com.analyzer.session.analysis.presentation.formatter.formatLapTime
import com.analyzer.session.analysis.presentation.model.SessionAnalysisLapOptionUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSessionOptionUi
import com.analyzer.session.analysis.presentation.utils.fastestLap
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_lap_label
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlight
import com.project.analyzer.telemetry.analysis.api.model.lap.SessionAnalysisLap
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisReport
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.segment.SessionAnalysisSegment
import org.jetbrains.compose.resources.getString
import java.time.Instant

internal fun SessionAnalysisReport.resolveSegment(selectedSegmentId: Long?): SessionAnalysisSegment? =
    segments.firstOrNull { segment ->
        segment.segmentId == selectedSegmentId
    }
        ?: segments.lastOrNull()

/**
 * Chooses the lap that should drive the screen, preferring an explicit user selection and otherwise
 * falling back to the most useful lap for analysis.
 */
internal fun SessionAnalysisReport.resolveLapNumber(
    segmentId: Long?,
    selectedLapNumber: Int?,
    segmentLaps: List<SessionAnalysisLap>,
    referenceLapNumber: Int?,
): Int? {
    if (selectedLapNumber != null && segmentLaps.any { lap -> lap.lapNumber == selectedLapNumber }) {
        return selectedLapNumber
    }
    return segmentLaps.preferredAnalysisLap(referenceLapNumber)?.lapNumber
        ?: referenceLapNumber
        ?: segmentLaps.lastOrNull()?.lapNumber
        ?: laps.filterLapsForSegment(segmentId).lastOrNull()?.lapNumber
}

internal fun resolveReferenceLapSelection(
    segmentLaps: List<SessionAnalysisLap>,
    segmentSamples: List<SessionAnalysisSample>,
    selectedReferenceLapNumber: Int?,
    selectedLapNumber: Int?,
): SessionAnalysisLap? = resolveReferenceLap(
    segmentLaps = segmentLaps,
    segmentSamples = segmentSamples,
    selectedReferenceLapNumber = selectedReferenceLapNumber,
    selectedLapNumber = selectedLapNumber,
)

internal fun SessionAnalysisSegment.toOptionUi(): SessionAnalysisSessionOptionUi = SessionAnalysisSessionOptionUi(
    segmentId = segmentId,
    primaryLabel = sessionTypeLabel.ifBlank { sessionLabel }.orDash(),
    supportingLabel = buildSessionOptionSupportingLabel(),
)

internal suspend fun List<SessionAnalysisLap>.toLapOptionUiList(): List<SessionAnalysisLapOptionUi> = buildList {
    for (lap in this@toLapOptionUiList) {
        add(
            SessionAnalysisLapOptionUi(
                lapNumber = lap.lapNumber,
                label = buildString {
                    append(getString(Res.string.session_analysis_lap_label, lap.lapNumber))
                    lap.durationMs?.let { duration ->
                        append("  ")
                        append(formatLapTime(duration))
                    }
                },
            ),
        )
    }
}

internal fun List<SessionAnalysisLap>.filterLapsForSegment(segmentId: Long?): List<SessionAnalysisLap> {
    if (segmentId == null) return this
    return filter { lap -> lap.segmentId == segmentId }
}

internal fun List<SessionAnalysisSample>.filterForSelection(
    segmentId: Long?,
    lapNumber: Int?,
): List<SessionAnalysisSample> = filter { sample ->
    (segmentId == null || sample.segmentId == segmentId) &&
        (lapNumber == null || sample.lapNumber == lapNumber)
}

internal fun List<SessionAnalysisHighlight>.filterHighlightsForSegment(
    segmentId: Long?,
): List<SessionAnalysisHighlight> {
    if (segmentId == null) return this
    return filter { highlight -> highlight.segmentId == segmentId }
}

private fun SessionAnalysisSegment.buildSessionOptionSupportingLabel(): String {
    val formattedTime = startedAtMs
        .takeIf { startedAt -> startedAt > 0L }
        ?.let { startedAt ->
            SessionAnalysisOptionTimeFormatter.format(Instant.ofEpochMilli(startedAt).atZone(SessionAnalysisZoneId))
        }
        .orEmpty()
    val secondary = trackLabel.ifBlank { carLabel }.orDash()
    return listOfNotNull(
        secondary.takeIf(String::isNotBlank),
        formattedTime.takeIf(String::isNotBlank),
    ).joinToString("  •  ").ifBlank { "--" }
}

private fun List<SessionAnalysisLap>.preferredAnalysisLap(referenceLapNumber: Int?): SessionAnalysisLap? = asSequence()
    .filter { lap ->
        lap.isValid &&
            lap.isComplete &&
            !lap.isPitLap &&
            lap.lapNumber != referenceLapNumber
    }
    .maxByOrNull(SessionAnalysisLap::lapNumber)
    ?: fastestLap()
    ?: lastOrNull { lap ->
        lap.isValid &&
            lap.isComplete &&
            !lap.isPitLap
    }
