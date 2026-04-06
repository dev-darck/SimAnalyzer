package com.analyzer.session.analysis.presentation.components.map

import com.analyzer.session.analysis.presentation.components.map.support.resolveTrackMapStrokeWidths
import kotlin.test.Test
import kotlin.test.assertTrue

class SessionAnalysisTrackMapStrokeWidthsTest {

    @Test
    fun `collapsed viewport resolves thinner telemetry strokes`() {
        val expanded = resolveTrackMapStrokeWidths(
            surfaceStrokePx = 28f,
            zoom = 1f,
        )
        val collapsed = resolveTrackMapStrokeWidths(
            surfaceStrokePx = 14f,
            zoom = 1f,
        )

        assertTrue(collapsed.referenceCore < expanded.referenceCore)
        assertTrue(collapsed.idealCore < expanded.idealCore)
        assertTrue(collapsed.selectedTrailCore < expanded.selectedTrailCore)
    }

    @Test
    fun `focus zoom keeps telemetry strokes bounded`() {
        val focus = resolveTrackMapStrokeWidths(
            surfaceStrokePx = 16f,
            zoom = 3.2f,
        )

        assertTrue(focus.referenceCore <= 2.2f)
        assertTrue(focus.idealCore <= 4.2f)
        assertTrue(focus.selectedTrailCore <= 3.6f)
    }
}
