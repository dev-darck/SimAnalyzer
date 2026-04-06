package com.analyzer.session.analysis.presentation.components.graph.support

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.analyzer.session.analysis.presentation.components.graph.model.SessionAnalysisTooltipRow
import com.analyzer.session.analysis.presentation.formatter.formatDegrees
import com.analyzer.session.analysis.presentation.formatter.formatFuel
import com.analyzer.session.analysis.presentation.formatter.formatGear
import com.analyzer.session.analysis.presentation.formatter.formatLapTime
import com.analyzer.session.analysis.presentation.formatter.formatPercent
import com.analyzer.session.analysis.presentation.formatter.formatRpm
import com.analyzer.session.analysis.presentation.formatter.formatSpeed
import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTelemetryChartKind
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTelemetrySeriesTone
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_graph_chart_gap_vs_ref
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_graph_legend_lap
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_graph_legend_ref
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_graph_status_ahead
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_graph_status_behind
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_graph_status_on_pace
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_graph_tooltip_diff
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_graph_tooltip_status
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_hero_fuel
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_lap_label
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_metric_brake
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_metric_rpm
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_metric_speed
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_metric_steer
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_metric_throttle
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_no_selection_placeholder
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Centralizes labels and tones so telemetry strips format values consistently across chart types.
 */
@Composable
internal fun chartDisplayTitle(kind: SessionAnalysisTelemetryChartKind): String = when (kind) {
    SessionAnalysisTelemetryChartKind.Delta -> stringResource(Res.string.session_analysis_graph_chart_gap_vs_ref)
    SessionAnalysisTelemetryChartKind.Speed -> stringResource(Res.string.session_analysis_metric_speed)
    SessionAnalysisTelemetryChartKind.Throttle -> stringResource(Res.string.session_analysis_metric_throttle)
    SessionAnalysisTelemetryChartKind.Brake -> stringResource(Res.string.session_analysis_metric_brake)
    SessionAnalysisTelemetryChartKind.Steering -> stringResource(Res.string.session_analysis_metric_steer)
    SessionAnalysisTelemetryChartKind.Rpm -> stringResource(Res.string.session_analysis_metric_rpm)
    SessionAnalysisTelemetryChartKind.Fuel -> stringResource(Res.string.session_analysis_hero_fuel)
}

@Composable
internal fun chartValueLabel(
    kind: SessionAnalysisTelemetryChartKind,
    activePoint: SessionAnalysisComparisonPointUi,
): String = when (kind) {
    SessionAnalysisTelemetryChartKind.Delta -> formatLocalizedDeltaStatus(activePoint.deltaMs)

    SessionAnalysisTelemetryChartKind.Speed -> formatSpeed(activePoint.selectedSpeedKmh)

    SessionAnalysisTelemetryChartKind.Throttle -> formatPercent(activePoint.selectedThrottle)

    SessionAnalysisTelemetryChartKind.Brake -> formatPercent(activePoint.selectedBrake)

    SessionAnalysisTelemetryChartKind.Steering -> formatDegrees(activePoint.selectedSteeringAngleRad)

    SessionAnalysisTelemetryChartKind.Rpm -> buildString {
        append(formatRpm(activePoint.selectedRpm))
        append("  ")
        append(formatGear(activePoint.selectedGear))
    }

    SessionAnalysisTelemetryChartKind.Fuel -> formatFuel(activePoint.selectedFuelLiters)
}

@Composable
internal fun chartTooltipRows(
    kind: SessionAnalysisTelemetryChartKind,
    activePoint: SessionAnalysisComparisonPointUi,
    lapAccent: Color,
    refAccent: Color,
    neutralAccent: Color,
): ImmutableList<SessionAnalysisTooltipRow> = when (kind) {
    SessionAnalysisTelemetryChartKind.Delta -> deltaTooltipRows(activePoint, lapAccent, refAccent, neutralAccent)
    SessionAnalysisTelemetryChartKind.Speed -> speedTooltipRows(activePoint, lapAccent, refAccent, neutralAccent)
    SessionAnalysisTelemetryChartKind.Throttle -> throttleTooltipRows(activePoint, lapAccent, refAccent, neutralAccent)
    SessionAnalysisTelemetryChartKind.Brake -> brakeTooltipRows(activePoint, lapAccent, refAccent, neutralAccent)
    SessionAnalysisTelemetryChartKind.Steering -> steeringTooltipRows(activePoint, lapAccent, refAccent, neutralAccent)
    SessionAnalysisTelemetryChartKind.Rpm -> rpmTooltipRows(activePoint, lapAccent, refAccent)
    SessionAnalysisTelemetryChartKind.Fuel -> fuelTooltipRows(activePoint, lapAccent, refAccent, neutralAccent)
}

internal fun toneColor(tone: SessionAnalysisTelemetrySeriesTone, palette: SessionAnalysisTelemetryTonePalette): Color =
    when (tone) {
        SessionAnalysisTelemetrySeriesTone.Primary -> palette.primaryToneColor
        SessionAnalysisTelemetrySeriesTone.Reference -> palette.referenceToneColor
        SessionAnalysisTelemetrySeriesTone.Throttle -> palette.throttleToneColor
        SessionAnalysisTelemetrySeriesTone.Brake -> palette.brakeToneColor
        SessionAnalysisTelemetrySeriesTone.Steering -> palette.steeringToneColor
        SessionAnalysisTelemetrySeriesTone.Rpm -> palette.cyanToneColor
        SessionAnalysisTelemetrySeriesTone.Fuel -> palette.cyanToneColor
        SessionAnalysisTelemetrySeriesTone.Delta -> palette.primaryToneColor
    }

@Composable
internal fun Int?.toGraphLapLabel(): String = this?.let { lapNumber ->
    stringResource(Res.string.session_analysis_lap_label, lapNumber)
} ?: stringResource(Res.string.session_analysis_no_selection_placeholder)

@Composable
internal fun formatLocalizedDeltaStatus(deltaMs: Int?): String = when {
    deltaMs == null -> stringResource(Res.string.session_analysis_no_selection_placeholder)
    deltaMs > 0 -> stringResource(Res.string.session_analysis_graph_status_behind, formatAbsoluteDeltaSeconds(deltaMs))
    deltaMs < 0 -> stringResource(Res.string.session_analysis_graph_status_ahead, formatAbsoluteDeltaSeconds(deltaMs))
    else -> stringResource(Res.string.session_analysis_graph_status_on_pace)
}

internal fun formatDeltaStatus(deltaMs: Int?): String = when {
    deltaMs == null -> "--"
    deltaMs > 0 -> "Behind ${formatAbsoluteDeltaSeconds(deltaMs)}"
    deltaMs < 0 -> "Ahead ${formatAbsoluteDeltaSeconds(deltaMs)}"
    else -> "On pace"
}

private fun formatElapsedForTooltip(valueMs: Int?): String = valueMs?.let(::formatLapTime) ?: "--"

@Composable
private fun deltaTooltipRows(
    activePoint: SessionAnalysisComparisonPointUi,
    lapAccent: Color,
    refAccent: Color,
    neutralAccent: Color,
): ImmutableList<SessionAnalysisTooltipRow> = persistentListOf(
    SessionAnalysisTooltipRow(
        stringResource(Res.string.session_analysis_graph_tooltip_status),
        formatLocalizedDeltaStatus(activePoint.deltaMs),
        neutralAccent,
    ),
    SessionAnalysisTooltipRow(
        stringResource(Res.string.session_analysis_graph_legend_lap),
        formatElapsedForTooltip(activePoint.selectedElapsedMs),
        lapAccent,
    ),
    SessionAnalysisTooltipRow(
        stringResource(Res.string.session_analysis_graph_legend_ref),
        formatElapsedForTooltip(activePoint.referenceElapsedMs),
        refAccent,
    ),
)

@Composable
private fun speedTooltipRows(
    activePoint: SessionAnalysisComparisonPointUi,
    lapAccent: Color,
    refAccent: Color,
    neutralAccent: Color,
): ImmutableList<SessionAnalysisTooltipRow> = persistentListOf(
    SessionAnalysisTooltipRow(
        stringResource(Res.string.session_analysis_graph_legend_lap),
        formatSpeed(activePoint.selectedSpeedKmh),
        lapAccent,
    ),
    SessionAnalysisTooltipRow(
        stringResource(Res.string.session_analysis_graph_legend_ref),
        formatSpeed(activePoint.referenceSpeedKmh),
        refAccent,
    ),
    SessionAnalysisTooltipRow(
        stringResource(Res.string.session_analysis_graph_tooltip_diff),
        formatUnitDelta(activePoint.selectedSpeedKmh, activePoint.referenceSpeedKmh, "km/h"),
        neutralAccent,
    ),
)

@Composable
private fun throttleTooltipRows(
    activePoint: SessionAnalysisComparisonPointUi,
    lapAccent: Color,
    refAccent: Color,
    neutralAccent: Color,
): ImmutableList<SessionAnalysisTooltipRow> = persistentListOf(
    SessionAnalysisTooltipRow(
        stringResource(Res.string.session_analysis_graph_legend_lap),
        formatPercent(activePoint.selectedThrottle),
        lapAccent,
    ),
    SessionAnalysisTooltipRow(
        stringResource(Res.string.session_analysis_graph_legend_ref),
        formatPercent(activePoint.referenceThrottle),
        refAccent,
    ),
    SessionAnalysisTooltipRow(
        stringResource(Res.string.session_analysis_graph_tooltip_diff),
        formatPercentDelta(activePoint.selectedThrottle, activePoint.referenceThrottle),
        neutralAccent,
    ),
)

@Composable
private fun brakeTooltipRows(
    activePoint: SessionAnalysisComparisonPointUi,
    lapAccent: Color,
    refAccent: Color,
    neutralAccent: Color,
): ImmutableList<SessionAnalysisTooltipRow> = persistentListOf(
    SessionAnalysisTooltipRow(
        stringResource(Res.string.session_analysis_graph_legend_lap),
        formatPercent(activePoint.selectedBrake),
        lapAccent,
    ),
    SessionAnalysisTooltipRow(
        stringResource(Res.string.session_analysis_graph_legend_ref),
        formatPercent(activePoint.referenceBrake),
        refAccent,
    ),
    SessionAnalysisTooltipRow(
        stringResource(Res.string.session_analysis_graph_tooltip_diff),
        formatPercentDelta(activePoint.selectedBrake, activePoint.referenceBrake),
        neutralAccent,
    ),
)

@Composable
private fun steeringTooltipRows(
    activePoint: SessionAnalysisComparisonPointUi,
    lapAccent: Color,
    refAccent: Color,
    neutralAccent: Color,
): ImmutableList<SessionAnalysisTooltipRow> = persistentListOf(
    SessionAnalysisTooltipRow(
        stringResource(Res.string.session_analysis_graph_legend_lap),
        formatDegrees(activePoint.selectedSteeringAngleRad),
        lapAccent,
    ),
    SessionAnalysisTooltipRow(
        stringResource(Res.string.session_analysis_graph_legend_ref),
        formatDegrees(activePoint.referenceSteeringAngleRad),
        refAccent,
    ),
    SessionAnalysisTooltipRow(
        stringResource(Res.string.session_analysis_graph_tooltip_diff),
        formatDegreesDelta(activePoint.selectedSteeringAngleRad, activePoint.referenceSteeringAngleRad),
        neutralAccent,
    ),
)

@Composable
private fun rpmTooltipRows(
    activePoint: SessionAnalysisComparisonPointUi,
    lapAccent: Color,
    refAccent: Color,
): ImmutableList<SessionAnalysisTooltipRow> = persistentListOf(
    SessionAnalysisTooltipRow(
        stringResource(Res.string.session_analysis_graph_legend_lap),
        "${formatRpm(activePoint.selectedRpm)}  ${formatGear(activePoint.selectedGear)}",
        lapAccent,
    ),
    SessionAnalysisTooltipRow(
        stringResource(Res.string.session_analysis_graph_legend_ref),
        "${formatRpm(activePoint.referenceRpm)}  ${formatGear(activePoint.referenceGear)}",
        refAccent,
    ),
)

@Composable
private fun fuelTooltipRows(
    activePoint: SessionAnalysisComparisonPointUi,
    lapAccent: Color,
    refAccent: Color,
    neutralAccent: Color,
): ImmutableList<SessionAnalysisTooltipRow> = persistentListOf(
    SessionAnalysisTooltipRow(
        stringResource(Res.string.session_analysis_graph_legend_lap),
        formatFuel(activePoint.selectedFuelLiters),
        lapAccent,
    ),
    SessionAnalysisTooltipRow(
        stringResource(Res.string.session_analysis_graph_legend_ref),
        formatFuel(activePoint.referenceFuelLiters),
        refAccent,
    ),
    SessionAnalysisTooltipRow(
        stringResource(Res.string.session_analysis_graph_tooltip_diff),
        formatUnitDelta(activePoint.selectedFuelLiters, activePoint.referenceFuelLiters, "L"),
        neutralAccent,
    ),
)

private fun formatUnitDelta(selected: Float?, reference: Float?, unit: String): String {
    if (selected == null || reference == null) return "--"
    val delta = selected - reference
    val prefix = if (delta > 0f) "+" else ""
    return when (unit) {
        "km/h" -> "$prefix${delta.roundToInt()} $unit"
        "L" -> String.format(Locale.US, "%.2f %s", delta, unit)
        else -> String.format(Locale.US, "%s%.1f %s", prefix, delta, unit)
    }
}

private fun formatDegreesDelta(selected: Float?, reference: Float?): String {
    if (selected == null || reference == null) return "--"
    val delta = radiansToDegrees(selected) - radiansToDegrees(reference)
    val prefix = if (delta > 0f) "+" else ""
    return String.format(Locale.US, "%s%.0fdeg", prefix, delta)
}

private fun radiansToDegrees(value: Float): Float = Math.toDegrees(value.toDouble()).toFloat()

private fun formatPercentDelta(selected: Float?, reference: Float?): String {
    if (selected == null || reference == null) return "--"
    val delta = (selected - reference) * 100f
    val prefix = if (delta > 0f) "+" else ""
    return String.format(Locale.US, "%s%.0f%%", prefix, delta)
}

private fun formatAbsoluteDeltaSeconds(deltaMs: Int): String = String.format(Locale.US, "%.3fs", abs(deltaMs) / 1000f)
