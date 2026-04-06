package com.analyzer.session.analysis.presentation.components.graph.preview

import androidx.compose.ui.unit.dp
import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisGraphState
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTelemetryChartKind
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTelemetryChartSeriesUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTelemetryChartUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTelemetrySeriesTone
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal fun sessionAnalysisGraphPreviewComparisonPoints(): ImmutableList<SessionAnalysisComparisonPointUi> =
    persistentListOf(
        comparisonPoint(0.00f, 118f, 120f, 0.18f, 0.21f),
        comparisonPoint(0.10f, 126f, 128f, 0.24f, 0.26f),
        comparisonPoint(0.20f, 138f, 136f, 0.42f, 0.37f),
        comparisonPoint(0.30f, 152f, 149f, 0.68f, 0.62f),
        comparisonPoint(0.40f, 146f, 142f, 0.84f, 0.79f),
        comparisonPoint(0.50f, 132f, 130f, 0.58f, 0.60f),
        comparisonPoint(0.60f, 121f, 124f, 0.31f, 0.36f),
        comparisonPoint(0.70f, 134f, 136f, 0.46f, 0.49f),
        comparisonPoint(0.80f, 148f, 151f, 0.72f, 0.75f),
        comparisonPoint(0.90f, 156f, 153f, 0.88f, 0.81f),
        comparisonPoint(1.00f, 118f, 120f, 0.18f, 0.21f),
    )

internal fun sessionAnalysisGraphPreviewActivePoint(): SessionAnalysisComparisonPointUi =
    sessionAnalysisGraphPreviewComparisonPoints()[4]

internal fun sessionAnalysisGraphPreviewState(): SessionAnalysisGraphState = SessionAnalysisGraphState(
    selectedLapLabel = "Lap 7",
    referenceLapLabel = "Lap 3",
    comparisonPoints = sessionAnalysisGraphPreviewComparisonPoints(),
    charts = persistentListOf(
        sessionAnalysisGraphPreviewSpeedChart(),
        sessionAnalysisGraphPreviewThrottleChart(),
    ),
)

internal fun sessionAnalysisGraphPreviewSpeedChart(): SessionAnalysisTelemetryChartUi = SessionAnalysisTelemetryChartUi(
    kind = SessionAnalysisTelemetryChartKind.Speed,
    title = "Speed",
    height = 112.dp,
    series = persistentListOf(
        SessionAnalysisTelemetryChartSeriesUi(
            label = "Lap",
            tone = SessionAnalysisTelemetrySeriesTone.Primary,
            dashed = false,
            points = chartPoints(
                0.74f, 0.67f, 0.56f, 0.42f, 0.48f, 0.62f, 0.71f, 0.60f, 0.44f, 0.36f, 0.74f,
            ),
        ),
        SessionAnalysisTelemetryChartSeriesUi(
            label = "Ref",
            tone = SessionAnalysisTelemetrySeriesTone.Reference,
            dashed = true,
            points = chartPoints(
                0.72f, 0.65f, 0.58f, 0.45f, 0.52f, 0.64f, 0.69f, 0.58f, 0.41f, 0.39f, 0.72f,
            ),
        ),
    ),
)

internal fun sessionAnalysisGraphPreviewThrottleChart(): SessionAnalysisTelemetryChartUi =
    SessionAnalysisTelemetryChartUi(
        kind = SessionAnalysisTelemetryChartKind.Throttle,
        title = "Throttle",
        height = 96.dp,
        series = persistentListOf(
            SessionAnalysisTelemetryChartSeriesUi(
                label = "Lap",
                tone = SessionAnalysisTelemetrySeriesTone.Throttle,
                dashed = false,
                points = chartPoints(
                    0.82f, 0.76f, 0.58f, 0.31f, 0.16f, 0.39f, 0.66f, 0.52f, 0.26f, 0.12f, 0.82f,
                ),
            ),
            SessionAnalysisTelemetryChartSeriesUi(
                label = "Ref",
                tone = SessionAnalysisTelemetrySeriesTone.Reference,
                dashed = true,
                points = chartPoints(
                    0.79f, 0.73f, 0.61f, 0.36f, 0.21f, 0.41f, 0.62f, 0.49f, 0.29f, 0.18f, 0.79f,
                ),
            ),
        ),
    )

private fun chartPoints(vararg yValues: Float): ImmutableList<SessionAnalysisFractionPointUi> = persistentListOf(
    *yValues.mapIndexed { index, value ->
        val fraction = index.toFloat() / yValues.lastIndex.coerceAtLeast(1)
        SessionAnalysisFractionPointUi(
            fraction = fraction,
            x = fraction,
            y = value,
        )
    }.toTypedArray(),
)

private fun comparisonPoint(
    fraction: Float,
    selectedSpeedKmh: Float,
    referenceSpeedKmh: Float,
    selectedThrottle: Float,
    referenceThrottle: Float,
): SessionAnalysisComparisonPointUi = SessionAnalysisComparisonPointUi(
    fraction = fraction,
    trackPosition = fraction,
    selectedFrameId = (fraction * 1_000).toLong(),
    selectedElapsedMs = (fraction * 82_000).toInt(),
    referenceElapsedMs = (fraction * 82_120).toInt(),
    deltaMs = ((referenceSpeedKmh - selectedSpeedKmh) * 6f).toInt(),
    selectedSpeedKmh = selectedSpeedKmh,
    referenceSpeedKmh = referenceSpeedKmh,
    selectedThrottle = selectedThrottle,
    referenceThrottle = referenceThrottle,
    selectedBrake = 1f - selectedThrottle,
    referenceBrake = 1f - referenceThrottle,
    selectedGear = 4,
    referenceGear = 4,
    selectedRpm = 6_800f + (selectedSpeedKmh * 12f),
    referenceRpm = 6_760f + (referenceSpeedKmh * 12f),
    selectedFuelLiters = 34.6f,
    referenceFuelLiters = 34.9f,
)
