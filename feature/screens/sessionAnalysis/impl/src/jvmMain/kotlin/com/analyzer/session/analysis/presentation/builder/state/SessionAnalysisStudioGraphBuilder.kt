package com.analyzer.session.analysis.presentation.builder.state

import androidx.compose.ui.unit.dp
import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisGraphState
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTelemetryChartKind
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTelemetryChartSeriesUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTelemetryChartUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTelemetrySeriesTone
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_lap_label
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_no_selection_placeholder
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.getString
import kotlin.math.abs

private val SessionAnalysisTelemetryChartHeight = 124.dp

/**
 * Builds the graph dock state from comparison points so the composables render a stable chart model
 * instead of repeating telemetry shaping logic during composition.
 */
internal suspend fun buildGraphState(
    comparisonPoints: ImmutableList<SessionAnalysisComparisonPointUi>,
    selectedLapNumber: Int?,
    referenceLapNumber: Int?,
): SessionAnalysisGraphState {
    val strings = SessionAnalysisGraphStrings.resolve()
    return SessionAnalysisGraphState(
        selectedLapLabel = selectedLapNumber.toLapLabel(),
        referenceLapLabel = referenceLapNumber.toLapLabel(),
        comparisonPoints = comparisonPoints,
        charts = listOfNotNull(
            buildDeltaChart(comparisonPoints, strings),
            buildSpeedChart(comparisonPoints, strings),
            buildThrottleChart(comparisonPoints, strings),
            buildBrakeChart(comparisonPoints, strings),
            buildSteeringChart(comparisonPoints, strings),
            buildRpmChart(comparisonPoints, strings),
            buildFuelChart(comparisonPoints, strings),
        ).toImmutableList(),
    )
}

private fun buildDeltaChart(
    comparisonPoints: ImmutableList<SessionAnalysisComparisonPointUi>,
    strings: SessionAnalysisGraphStrings,
): SessionAnalysisTelemetryChartUi? = buildChart(
    comparisonPoints = comparisonPoints,
    kind = SessionAnalysisTelemetryChartKind.Delta,
    title = strings.gapVsReference,
    rangeResolver = { seeds -> seeds.toSymmetricChartRange() },
    series = listOf(
        SessionAnalysisChartSeriesSpec(
            label = strings.delta,
            tone = SessionAnalysisTelemetrySeriesTone.Delta,
            dashed = false,
            smoothingWindowRadius = 2,
            selector = { point -> point.deltaMs?.toFloat() },
        ),
    ),
)

private fun buildSpeedChart(
    comparisonPoints: ImmutableList<SessionAnalysisComparisonPointUi>,
    strings: SessionAnalysisGraphStrings,
): SessionAnalysisTelemetryChartUi? = buildChart(
    comparisonPoints = comparisonPoints,
    kind = SessionAnalysisTelemetryChartKind.Speed,
    title = strings.speed,
    rangeResolver = { seeds -> seeds.toLinearChartRange() },
    series = listOf(
        SessionAnalysisChartSeriesSpec(
            label = strings.lap,
            tone = SessionAnalysisTelemetrySeriesTone.Primary,
            dashed = false,
            smoothingWindowRadius = 2,
            selector = SessionAnalysisComparisonPointUi::selectedSpeedKmh,
        ),
        SessionAnalysisChartSeriesSpec(
            label = strings.reference,
            tone = SessionAnalysisTelemetrySeriesTone.Reference,
            dashed = true,
            smoothingWindowRadius = 2,
            selector = SessionAnalysisComparisonPointUi::referenceSpeedKmh,
        ),
    ),
)

private fun buildThrottleChart(
    comparisonPoints: ImmutableList<SessionAnalysisComparisonPointUi>,
    strings: SessionAnalysisGraphStrings,
): SessionAnalysisTelemetryChartUi? = buildChart(
    comparisonPoints = comparisonPoints,
    kind = SessionAnalysisTelemetryChartKind.Throttle,
    title = strings.throttle,
    rangeResolver = { seeds -> seeds.toPercentChartRange() },
    series = listOf(
        SessionAnalysisChartSeriesSpec(
            label = strings.lap,
            tone = SessionAnalysisTelemetrySeriesTone.Throttle,
            dashed = false,
            selector = { point -> point.selectedThrottle?.times(100f) },
        ),
        SessionAnalysisChartSeriesSpec(
            label = strings.reference,
            tone = SessionAnalysisTelemetrySeriesTone.Reference,
            dashed = true,
            selector = { point -> point.referenceThrottle?.times(100f) },
        ),
    ),
)

private fun buildBrakeChart(
    comparisonPoints: ImmutableList<SessionAnalysisComparisonPointUi>,
    strings: SessionAnalysisGraphStrings,
): SessionAnalysisTelemetryChartUi? = buildChart(
    comparisonPoints = comparisonPoints,
    kind = SessionAnalysisTelemetryChartKind.Brake,
    title = strings.brake,
    rangeResolver = { seeds -> seeds.toPercentChartRange() },
    series = listOf(
        SessionAnalysisChartSeriesSpec(
            label = strings.lap,
            tone = SessionAnalysisTelemetrySeriesTone.Brake,
            dashed = false,
            selector = { point -> point.selectedBrake?.times(100f) },
        ),
        SessionAnalysisChartSeriesSpec(
            label = strings.reference,
            tone = SessionAnalysisTelemetrySeriesTone.Reference,
            dashed = true,
            selector = { point -> point.referenceBrake?.times(100f) },
        ),
    ),
)

private fun buildSteeringChart(
    comparisonPoints: ImmutableList<SessionAnalysisComparisonPointUi>,
    strings: SessionAnalysisGraphStrings,
): SessionAnalysisTelemetryChartUi? = buildChart(
    comparisonPoints = comparisonPoints,
    kind = SessionAnalysisTelemetryChartKind.Steering,
    title = strings.steering,
    rangeResolver = { seeds -> seeds.toSymmetricChartRange() },
    series = listOf(
        SessionAnalysisChartSeriesSpec(
            label = strings.lap,
            tone = SessionAnalysisTelemetrySeriesTone.Steering,
            dashed = false,
            smoothingWindowRadius = 2,
            selector = { point -> point.selectedSteeringAngleRad?.let(::radiansToDegrees) },
        ),
        SessionAnalysisChartSeriesSpec(
            label = strings.reference,
            tone = SessionAnalysisTelemetrySeriesTone.Reference,
            dashed = true,
            smoothingWindowRadius = 2,
            selector = { point -> point.referenceSteeringAngleRad?.let(::radiansToDegrees) },
        ),
    ),
)

private fun buildRpmChart(
    comparisonPoints: ImmutableList<SessionAnalysisComparisonPointUi>,
    strings: SessionAnalysisGraphStrings,
): SessionAnalysisTelemetryChartUi? = buildChart(
    comparisonPoints = comparisonPoints,
    kind = SessionAnalysisTelemetryChartKind.Rpm,
    title = strings.rpm,
    rangeResolver = { seeds -> seeds.toLinearChartRange() },
    series = listOf(
        SessionAnalysisChartSeriesSpec(
            label = strings.lap,
            tone = SessionAnalysisTelemetrySeriesTone.Rpm,
            dashed = false,
            smoothingWindowRadius = 2,
            selector = SessionAnalysisComparisonPointUi::selectedRpm,
        ),
        SessionAnalysisChartSeriesSpec(
            label = strings.reference,
            tone = SessionAnalysisTelemetrySeriesTone.Reference,
            dashed = true,
            smoothingWindowRadius = 2,
            selector = SessionAnalysisComparisonPointUi::referenceRpm,
        ),
    ),
)

private fun buildFuelChart(
    comparisonPoints: ImmutableList<SessionAnalysisComparisonPointUi>,
    strings: SessionAnalysisGraphStrings,
): SessionAnalysisTelemetryChartUi? = buildChart(
    comparisonPoints = comparisonPoints,
    kind = SessionAnalysisTelemetryChartKind.Fuel,
    title = strings.fuel,
    rangeResolver = { seeds -> seeds.toLinearChartRange() },
    series = listOf(
        SessionAnalysisChartSeriesSpec(
            label = strings.lap,
            tone = SessionAnalysisTelemetrySeriesTone.Fuel,
            dashed = false,
            smoothingWindowRadius = 1,
            selector = SessionAnalysisComparisonPointUi::selectedFuelLiters,
        ),
        SessionAnalysisChartSeriesSpec(
            label = strings.reference,
            tone = SessionAnalysisTelemetrySeriesTone.Reference,
            dashed = true,
            smoothingWindowRadius = 1,
            selector = SessionAnalysisComparisonPointUi::referenceFuelLiters,
        ),
    ),
)

private fun buildChart(
    comparisonPoints: ImmutableList<SessionAnalysisComparisonPointUi>,
    kind: SessionAnalysisTelemetryChartKind,
    title: String,
    rangeResolver: (List<SessionAnalysisChartSeriesSeed>) -> SessionAnalysisChartRange?,
    series: List<SessionAnalysisChartSeriesSpec>,
): SessionAnalysisTelemetryChartUi? {
    val seededSeries = series.map { spec ->
        buildChartSeriesSeed(
            comparisonPoints = comparisonPoints,
            spec = spec,
        )
    }.filter { seed -> seed.rawPoints.isNotEmpty() }
    val resolvedRange = rangeResolver(seededSeries) ?: return null
    return SessionAnalysisTelemetryChartUi(
        kind = kind,
        title = title,
        height = SessionAnalysisTelemetryChartHeight,
        zeroBaseline = resolvedRange.zeroBaseline,
        series = seededSeries.map { seed ->
            buildChartSeries(
                range = resolvedRange,
                seed = seed,
            )
        }.toImmutableList(),
    )
}

private fun buildChartSeriesSeed(
    comparisonPoints: ImmutableList<SessionAnalysisComparisonPointUi>,
    spec: SessionAnalysisChartSeriesSpec,
): SessionAnalysisChartSeriesSeed {
    val rawPoints = buildList {
        comparisonPoints.forEach { point ->
            val rawValue = spec.selector(point)?.takeIf(Float::isFinite) ?: return@forEach
            add(
                SessionAnalysisRawChartPoint(
                    fraction = point.fraction,
                    value = rawValue,
                ),
            )
        }
    }
    return SessionAnalysisChartSeriesSeed(
        label = spec.label,
        tone = spec.tone,
        dashed = spec.dashed,
        smoothingWindowRadius = spec.smoothingWindowRadius,
        rawPoints = rawPoints,
    )
}

private fun buildChartSeries(
    range: SessionAnalysisChartRange,
    seed: SessionAnalysisChartSeriesSeed,
): SessionAnalysisTelemetryChartSeriesUi {
    val rawPoints = seed.rawPoints.map { point ->
        SessionAnalysisFractionPointUi(
            fraction = point.fraction,
            x = point.fraction,
            y = 1f - ((point.value - range.minValue) / (range.maxValue - range.minValue)).coerceIn(0f, 1f),
        )
    }
    val chartPoints = rawPoints.smoothChartPoints(windowRadius = seed.smoothingWindowRadius)
    return SessionAnalysisTelemetryChartSeriesUi(
        label = seed.label,
        tone = seed.tone,
        dashed = seed.dashed,
        coverageStartFraction = rawPoints.firstOrNull()?.fraction ?: 0f,
        coverageEndFraction = rawPoints.lastOrNull()?.fraction ?: 1f,
        points = chartPoints.toImmutableList(),
    )
}

private fun List<SessionAnalysisChartSeriesSeed>.toLinearChartRange(): SessionAnalysisChartRange? {
    var minValue = Float.POSITIVE_INFINITY
    var maxValue = Float.NEGATIVE_INFINITY
    forEach { seed ->
        seed.rawPoints.forEach { point ->
            minValue = minOf(minValue, point.value)
            maxValue = maxOf(maxValue, point.value)
        }
    }
    if (minValue.isInfinite() || maxValue.isInfinite()) return null
    val resolvedMaxValue = if (maxValue == minValue) maxValue + 1f else maxValue
    return SessionAnalysisChartRange(
        minValue = minValue,
        maxValue = resolvedMaxValue,
    )
}

private fun List<SessionAnalysisChartSeriesSeed>.toPercentChartRange(): SessionAnalysisChartRange? {
    var maxValue = 100f
    var hasValues = false
    forEach { seed ->
        seed.rawPoints.forEach { point ->
            hasValues = true
            maxValue = maxOf(maxValue, point.value)
        }
    }
    if (!hasValues) return null
    return SessionAnalysisChartRange(
        minValue = 0f,
        maxValue = maxValue,
    )
}

private fun List<SessionAnalysisChartSeriesSeed>.toSymmetricChartRange(
    minimumEdge: Float = 1f,
): SessionAnalysisChartRange? {
    var hasValues = false
    var edge = minimumEdge
    forEach { seed ->
        seed.rawPoints.forEach { point ->
            hasValues = true
            edge = maxOf(edge, abs(point.value))
        }
    }
    if (!hasValues) return null
    return SessionAnalysisChartRange(
        minValue = -edge,
        maxValue = edge,
        zeroBaseline = resolveZeroBaseline(minValue = -edge, maxValue = edge),
    )
}

private fun resolveZeroBaseline(minValue: Float, maxValue: Float): Float? {
    if (maxValue <= minValue) return null
    return 1f - ((0f - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
}

private fun List<SessionAnalysisFractionPointUi>.smoothChartPoints(
    windowRadius: Int,
): List<SessionAnalysisFractionPointUi> {
    if (windowRadius <= 0 || size < 5) return this
    return mapIndexed { index, point ->
        if (index == 0 || index == lastIndex) {
            point
        } else {
            val startIndex = (index - windowRadius).coerceAtLeast(0)
            val endIndex = (index + windowRadius).coerceAtMost(lastIndex)
            var ySum = 0f
            var count = 0
            for (sampleIndex in startIndex..endIndex) {
                ySum += this[sampleIndex].y
                count++
            }
            point.copy(y = ySum / count.toFloat())
        }
    }
}

private suspend fun Int?.toLapLabel(): String = this
    ?.let { lapNumber -> getString(Res.string.session_analysis_lap_label, lapNumber) }
    ?: getString(Res.string.session_analysis_no_selection_placeholder)

private fun radiansToDegrees(value: Float): Float = Math.toDegrees(value.toDouble()).toFloat()
