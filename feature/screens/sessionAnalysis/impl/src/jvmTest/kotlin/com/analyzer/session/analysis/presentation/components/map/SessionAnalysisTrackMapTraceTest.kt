package com.analyzer.session.analysis.presentation.components.map

import androidx.compose.ui.geometry.Offset
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackMapPointerState
import com.analyzer.session.analysis.presentation.components.map.support.SessionAnalysisTrackMapPointerPurpose
import com.analyzer.session.analysis.presentation.components.map.support.buildTraceLookup
import com.analyzer.session.analysis.presentation.components.map.support.isClosedTelemetryLoop
import com.analyzer.session.analysis.presentation.components.map.support.resolveTrackMapPointerTarget
import com.analyzer.session.analysis.presentation.components.map.support.sampleDirectionAtFraction
import com.analyzer.session.analysis.presentation.components.map.support.telemetryPathBreakIndices
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionAnalysisTrackMapTraceTest {

    @Test
    fun `sampleDirectionAtFraction skips duplicated closure point at start finish seam`() {
        val line = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 0.25f, x = 20f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 0.5f, x = 20f, y = 20f),
            SessionAnalysisFractionPointUi(fraction = 0.75f, x = 0f, y = 20f),
            SessionAnalysisFractionPointUi(fraction = 1f, x = 0f, y = 0f),
        )

        val direction = line.sampleDirectionAtFraction(0f)

        assertEquals(1f, direction?.x ?: 0f, 0.001f)
        assertEquals(0f, direction?.y ?: 0f, 0.001f)
    }

    @Test
    fun `isClosedTelemetryLoop rejects unfinished lap even when endpoints are close`() {
        val trace = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 0.24f, x = 18f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 0.48f, x = 18f, y = 16f),
            SessionAnalysisFractionPointUi(fraction = 0.72f, x = 0.2f, y = 0.2f),
        )

        assertFalse(trace.isClosedTelemetryLoop())
    }

    @Test
    fun `isClosedTelemetryLoop accepts full lap with sampled start finish seam`() {
        val trace = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0.01f, x = 0f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 0.25f, x = 20f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 0.5f, x = 20f, y = 20f),
            SessionAnalysisFractionPointUi(fraction = 0.75f, x = 0f, y = 20f),
            SessionAnalysisFractionPointUi(fraction = 0.99f, x = 0.2f, y = 0.2f),
        )

        assertTrue(trace.isClosedTelemetryLoop())
    }

    @Test
    fun `telemetryPathBreakIndices splits missing reference data chunk`() {
        val trace = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 0.05f, x = 5f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 0.10f, x = 10f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 0.42f, x = 12f, y = 1f),
            SessionAnalysisFractionPointUi(fraction = 0.47f, x = 17f, y = 1f),
        )

        assertEquals(setOf(3), trace.telemetryPathBreakIndices())
    }

    @Test
    fun `telemetryPathBreakIndices keeps sparse reference samples connected`() {
        val trace = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 0.2f, x = 20f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 0.45f, x = 20f, y = 20f),
            SessionAnalysisFractionPointUi(fraction = 0.7f, x = 0f, y = 20f),
            SessionAnalysisFractionPointUi(fraction = 0.95f, x = 0f, y = 2f),
        )

        assertEquals(emptySet<Int>(), trace.telemetryPathBreakIndices())
    }

    @Test
    fun `resolveTrackMapPointerTarget returns frame id for selected trace hover`() {
        val trace = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = 0f, frameId = 10L),
            SessionAnalysisFractionPointUi(fraction = 0.5f, x = 10f, y = 0f, frameId = 20L),
        )

        val target = resolveTrackMapPointerTarget(
            context = trackMapPointerState(
                selectedTraceAvailable = true,
                projectedSelectedTrace = trace,
                hoverTrace = trace,
                hoverDistancePx = 12f,
                hoverPreferenceIndex = 1,
                hoverLocalIndexWindow = 1,
                hoverPreferenceFraction = 0.5f,
                focusMode = false,
            ),
            pointer = Offset(8f, 0f),
            purpose = SessionAnalysisTrackMapPointerPurpose.Hover,
        )

        assertEquals(0.4f, target.first ?: 0f, 0.001f)
        assertEquals(20L, target.second)
    }

    @Test
    fun `resolveTrackMapPointerTarget ignores missing telemetry chunk segment`() {
        val trace = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = 0f, frameId = 1L),
            SessionAnalysisFractionPointUi(fraction = 0.05f, x = 5f, y = 0f, frameId = 2L),
            SessionAnalysisFractionPointUi(fraction = 0.10f, x = 10f, y = 0f, frameId = 3L),
            SessionAnalysisFractionPointUi(fraction = 0.42f, x = 12f, y = 16f, frameId = 4L),
            SessionAnalysisFractionPointUi(fraction = 0.47f, x = 17f, y = 16f, frameId = 5L),
        )

        val target = resolveTrackMapPointerTarget(
            context = trackMapPointerState(
                selectedTraceAvailable = true,
                projectedSelectedTrace = trace,
                hoverTrace = trace,
                hoverDistancePx = 2f,
                hoverPreferenceIndex = 3,
                hoverLocalIndexWindow = 2,
                hoverPreferenceFraction = 0.42f,
                focusMode = true,
            ),
            pointer = Offset(x = 11f, y = 8f),
            purpose = SessionAnalysisTrackMapPointerPurpose.Hover,
        )

        assertEquals(null, target.first)
        assertEquals(null, target.second)
    }

    @Test
    fun `resolveTrackMapPointerTarget keeps local branch in focus mode hover`() {
        val trace = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = 0f, frameId = 1L),
            SessionAnalysisFractionPointUi(fraction = 0.25f, x = 10f, y = 10f, frameId = 2L),
            SessionAnalysisFractionPointUi(fraction = 0.5f, x = 0f, y = 10f, frameId = 3L),
            SessionAnalysisFractionPointUi(fraction = 0.75f, x = 10f, y = 0f, frameId = 4L),
            SessionAnalysisFractionPointUi(fraction = 1f, x = 0f, y = 0f, frameId = 5L),
        )

        val focusTarget = resolveTrackMapPointerTarget(
            context = trackMapPointerState(
                selectedTraceAvailable = true,
                projectedSelectedTrace = trace,
                hoverTrace = trace,
                hoverDistancePx = 16f,
                hoverPreferenceIndex = 3,
                hoverLocalIndexWindow = 1,
                hoverPreferenceFraction = 0.75f,
                focusMode = true,
            ),
            pointer = Offset(5f, 5f),
            purpose = SessionAnalysisTrackMapPointerPurpose.Hover,
        )
        val normalTarget = resolveTrackMapPointerTarget(
            context = trackMapPointerState(
                selectedTraceAvailable = true,
                projectedSelectedTrace = trace,
                hoverTrace = trace,
                hoverDistancePx = 16f,
                hoverPreferenceIndex = 3,
                hoverLocalIndexWindow = 1,
                hoverPreferenceFraction = 0.75f,
                focusMode = false,
            ),
            pointer = Offset(5f, 5f),
            purpose = SessionAnalysisTrackMapPointerPurpose.Hover,
        )

        assertEquals(4L, focusTarget.second)
        assertEquals(2L, normalTarget.second)
        assertNotEquals(normalTarget.second, focusTarget.second)
    }

    private fun trackMapPointerState(
        selectedTraceAvailable: Boolean,
        projectedSelectedTrace: List<SessionAnalysisFractionPointUi>,
        hoverTrace: List<SessionAnalysisFractionPointUi>,
        hoverDistancePx: Float,
        hoverPreferenceIndex: Int?,
        hoverLocalIndexWindow: Int,
        hoverPreferenceFraction: Float?,
        focusMode: Boolean,
    ): SessionAnalysisTrackMapPointerState = SessionAnalysisTrackMapPointerState(
        selectedTraceAvailable = selectedTraceAvailable,
        projectedSelectedTrace = projectedSelectedTrace.toPersistentList(),
        projectedSelectedTraceLookup = projectedSelectedTrace.buildTraceLookup(),
        hoverTrace = hoverTrace.toPersistentList(),
        hoverTraceLookup = hoverTrace.buildTraceLookup(),
        hoverDistancePx = hoverDistancePx,
        hoverPreferenceIndex = hoverPreferenceIndex,
        hoverLocalIndexWindow = hoverLocalIndexWindow,
        hoverPreferenceFraction = hoverPreferenceFraction,
        focusMode = focusMode,
    )
}
