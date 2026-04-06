package com.analyzer.session.analysis.presentation.components.map

import com.analyzer.session.analysis.presentation.components.map.support.buildSelectedTrailTrace
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionAnalysisTrackMapFocusStateTest {

    @Test
    fun `buildSelectedTrailTrace starts from wrap point after first start finish crossing`() {
        val trace = listOf(
            point(fraction = 0.00f, x = 0f),
            point(fraction = 0.12f, x = 12f),
            point(fraction = 0.26f, x = 26f),
            point(fraction = 0.40f, x = 40f),
            point(fraction = 0.62f, x = 62f),
            point(fraction = 0.78f, x = 78f),
            point(fraction = 1.00f, x = 100f),
        ).toImmutableList()

        val trail = trace.buildSelectedTrailTrace(
            trailFraction = 0.90f,
            trailStartFraction = 0.62f,
        )

        assertEquals(listOf(0.62f, 0.78f, 1.00f), trail.map(SessionAnalysisFractionPointUi::fraction))
    }

    @Test
    fun `buildSelectedTrailTrace keeps pit exit segment before first start finish crossing`() {
        val trace = listOf(
            point(fraction = 0.00f, x = 0f),
            point(fraction = 0.12f, x = 12f),
            point(fraction = 0.26f, x = 26f),
            point(fraction = 0.40f, x = 40f),
            point(fraction = 0.62f, x = 62f),
            point(fraction = 0.78f, x = 78f),
            point(fraction = 1.00f, x = 100f),
        ).toImmutableList()

        val trail = trace.buildSelectedTrailTrace(
            trailFraction = 0.40f,
            trailStartFraction = 0.62f,
        )

        assertEquals(listOf(0.00f, 0.12f, 0.26f, 0.40f), trail.map(SessionAnalysisFractionPointUi::fraction))
    }

    private fun point(fraction: Float, x: Float): SessionAnalysisFractionPointUi = SessionAnalysisFractionPointUi(
        fraction = fraction,
        x = x,
        y = 0f,
    )
}
