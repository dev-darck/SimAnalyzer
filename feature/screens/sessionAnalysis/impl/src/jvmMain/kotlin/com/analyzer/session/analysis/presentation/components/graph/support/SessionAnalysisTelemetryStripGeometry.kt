package com.analyzer.session.analysis.presentation.components.graph.support

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTelemetryChartSeriesUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTelemetryChartUi
import kotlinx.collections.immutable.ImmutableList

/**
 * Converts normalized telemetry values into draw-space coordinates for the strip charts.
 */
internal fun List<SessionAnalysisFractionPointUi>.projectChartPoints(
    chartSize: IntSize,
    domainStart: Float,
    domainEnd: Float,
): List<Offset> = map { point ->
    Offset(
        x = normalizeChartFraction(
            fraction = point.fraction,
            domainStart = domainStart,
            domainEnd = domainEnd,
        ) * chartSize.width.toFloat(),
        y = point.y * chartSize.height.toFloat(),
    )
}

internal fun SessionAnalysisTelemetryChartUi.resolveFractionDomain(): Pair<Float, Float> {
    val starts = series.map(SessionAnalysisTelemetryChartSeriesUi::coverageStartFraction)
    val ends = series.map(SessionAnalysisTelemetryChartSeriesUi::coverageEndFraction)
    val start = starts.minOrNull() ?: 0f
    val end = ends.maxOrNull() ?: 1f
    return start to if (end - start <= 0.0001f) start + 1f else end
}

internal fun resolveChartCursorX(fraction: Float, chartWidth: Float, domainStart: Float, domainEnd: Float): Float? {
    if (chartWidth <= 0f) return null
    return normalizeChartFraction(
        fraction = fraction,
        domainStart = domainStart,
        domainEnd = domainEnd,
    ) * chartWidth
}

internal fun normalizeChartFraction(fraction: Float, domainStart: Float, domainEnd: Float): Float {
    val domainSpan = (domainEnd - domainStart).takeIf { it > 0.0001f } ?: return 0.5f
    return ((fraction - domainStart) / domainSpan).coerceIn(0f, 1f)
}

internal fun ImmutableList<SessionAnalysisComparisonPointUi>.resolveFraction(
    pointerX: Float,
    width: Float,
    domainStart: Float,
    domainEnd: Float,
): Float? {
    if (isEmpty() || width <= 0f) return null
    val chartFraction = (pointerX / width).coerceIn(0f, 1f)
    val domainFraction = domainStart + ((domainEnd - domainStart) * chartFraction)
    return minByOrNull { point ->
        kotlin.math.abs(point.fraction - domainFraction)
    }?.fraction
}
