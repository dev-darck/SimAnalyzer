package com.analyzer.session.analysis.presentation.builder.state

import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTelemetryChartKind
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionAnalysisStudioGraphBuilderTest {

    @Test
    fun `buildGraphState ignores non finite throttle and brake values`() = runBlocking {
        val graph = buildGraphState(
            comparisonPoints = persistentListOf(
                SessionAnalysisComparisonPointUi(
                    fraction = 0f,
                    trackPosition = 0f,
                    selectedThrottle = 0f,
                    referenceThrottle = 0.1f,
                    selectedBrake = Float.NaN,
                    referenceBrake = 0.15f,
                ),
                SessionAnalysisComparisonPointUi(
                    fraction = 0.5f,
                    trackPosition = 0.5f,
                    selectedThrottle = Float.NaN,
                    referenceThrottle = 0.55f,
                    selectedBrake = 0.62f,
                    referenceBrake = Float.NaN,
                ),
                SessionAnalysisComparisonPointUi(
                    fraction = 1f,
                    trackPosition = 1f,
                    selectedThrottle = 1f,
                    referenceThrottle = 0.92f,
                    selectedBrake = 0f,
                    referenceBrake = 0.02f,
                ),
            ),
            selectedLapNumber = 7,
            referenceLapNumber = 3,
        )

        val throttleChart = graph.charts.first { chart -> chart.kind == SessionAnalysisTelemetryChartKind.Throttle }
        val brakeChart = graph.charts.first { chart -> chart.kind == SessionAnalysisTelemetryChartKind.Brake }

        assertEquals(2, throttleChart.series.count { series -> series.points.isNotEmpty() })
        assertEquals(2, brakeChart.series.count { series -> series.points.isNotEmpty() })
        assertTrue(
            throttleChart.series.flatMap { series -> series.points }.all { point ->
                point.x.isFinite() && point.y.isFinite()
            },
        )
        assertTrue(
            brakeChart.series.flatMap { series -> series.points }.all { point ->
                point.x.isFinite() && point.y.isFinite()
            },
        )
    }
}
