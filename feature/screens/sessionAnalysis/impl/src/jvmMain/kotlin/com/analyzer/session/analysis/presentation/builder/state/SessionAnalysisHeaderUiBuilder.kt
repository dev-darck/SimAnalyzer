package com.analyzer.session.analysis.presentation.builder.state

import com.analyzer.session.analysis.presentation.formatter.formatLapTime
import com.analyzer.session.analysis.presentation.formatter.formatTemperature
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHeaderUi
import com.analyzer.session.analysis.presentation.utils.fastestLap
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_lap_label
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_no_selection_placeholder
import com.project.analyzer.telemetry.analysis.api.model.header.SessionAnalysisHeader
import com.project.analyzer.telemetry.analysis.api.model.lap.SessionAnalysisLap
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisReport
import com.project.analyzer.telemetry.analysis.api.model.segment.SessionAnalysisSegment
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreProfile
import org.jetbrains.compose.resources.getString
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Builds the compact header summary so the top of the workspace reflects the current session context.
 */
internal val SessionAnalysisOptionTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(
    "dd MMM, HH:mm",
    Locale.US,
)

private val SessionAnalysisHeaderTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(
    "EEE, dd MMM yyyy  HH:mm",
    Locale.US,
)

internal val SessionAnalysisZoneId: ZoneId = ZoneId.systemDefault()

internal suspend fun SessionAnalysisReport.buildHeaderUi(
    reportHeader: SessionAnalysisHeader,
    segment: SessionAnalysisSegment?,
    segmentLaps: List<SessionAnalysisLap>,
    selectedLap: SessionAnalysisLap?,
    tyreProfile: SessionAnalysisTyreProfile?,
    vehicleClassLabel: String,
): SessionAnalysisHeaderUi {
    val bestLap = segmentLaps.fastestLap()
    val validLapCount = segmentLaps.count { lap -> lap.isValid && lap.isComplete && !lap.isPitLap }
    val topSpeed = segmentLaps.mapNotNull(SessionAnalysisLap::maxSpeedKmh).maxOrNull()
    val peakBrake = segmentLaps.mapNotNull(SessionAnalysisLap::peakBrake).maxOrNull()
    val peakCore = segmentLaps.mapNotNull(SessionAnalysisLap::peakCoreTempC).maxOrNull()
    val startedAtMs = segment?.startedAtMs
        ?.takeIf { it > 0L }
        ?: reportHeader.startedAtMs.takeIf { it > 0L }

    return SessionAnalysisHeaderUi(
        sessionTypeLabel = segment?.sessionTypeLabel.orDash(),
        carLabel = segment?.carLabel.orDash(),
        trackLabel = segment?.trackLabel.orDash(),
        startedAtLabel = startedAtMs?.let(::formatHeaderStartedAt) ?: "--",
        vehicleClassLabel = vehicleClassLabel,
        compoundLabel = tyreProfile?.compoundLabel
            ?.takeIf(String::isNotBlank)
            ?: tyreProfile?.compoundFamily?.name.orDash(),
        airTempLabel = formatTemperature(reportHeader.airTempC),
        trackTempLabel = formatTemperature(reportHeader.trackTempC),
        bestLapLabel = bestLap?.durationMs?.let(::formatLapTime) ?: "--",
        selectedLapLabel = selectedLap?.lapNumber?.let { lapNumber ->
            getString(Res.string.session_analysis_lap_label, lapNumber)
        } ?: getString(Res.string.session_analysis_no_selection_placeholder),
        lapTimeLabel = selectedLap?.durationMs?.let(::formatLapTime) ?: "--",
        validLapsLabel = validLapCount.toString(),
        topSpeedLabel = topSpeed?.let { value -> "${value.toInt()} km/h" } ?: "--",
        peakBrakeLabel = peakBrake?.let { value -> "${(value * 100f).toInt()}%" } ?: "--",
        peakCoreLabel = formatTemperature(peakCore),
        surfaceWindowLabel = tyreProfile.rangeLabel(
            minValue = tyreProfile?.surfaceOptimalMinC,
            maxValue = tyreProfile?.surfaceOptimalMaxC,
        ),
        coreWindowLabel = tyreProfile.rangeLabel(
            minValue = tyreProfile?.coreOptimalMinC,
            maxValue = tyreProfile?.coreOptimalMaxC,
        ),
        brakeWindowLabel = tyreProfile.rangeLabel(
            minValue = tyreProfile?.brakeOptimalMinC,
            maxValue = tyreProfile?.brakeOptimalMaxC,
        ),
    )
}

internal fun String?.orDash(): String = this?.takeIf(String::isNotBlank) ?: "--"

internal fun Any?.toDisplayLabel(): String = toString()
    .split('_')
    .joinToString(" ") { part ->
        part.lowercase(Locale.US).replaceFirstChar { char -> char.titlecase(Locale.US) }
    }

private fun formatHeaderStartedAt(startedAtMs: Long): String =
    SessionAnalysisHeaderTimeFormatter.format(Instant.ofEpochMilli(startedAtMs).atZone(SessionAnalysisZoneId))

private fun SessionAnalysisTyreProfile?.rangeLabel(minValue: Float?, maxValue: Float?): String {
    if (this == null || minValue == null || maxValue == null) return "--"
    return "${formatTemperature(minValue)} - ${formatTemperature(maxValue)}"
}
