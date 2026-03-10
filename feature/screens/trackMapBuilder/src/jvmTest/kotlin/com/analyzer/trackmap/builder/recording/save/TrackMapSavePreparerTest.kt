package com.analyzer.trackmap.builder.recording.save

import com.analyzer.trackmap.data.library.TrackMapStatsCalculator
import com.analyzer.trackmap.builder.recording.runtime.TrackMapRecorderRuntime
import com.analyzer.trackmap.builder.recording.runtime.TrackMapRecorderState
import com.analyzer.trackmap.builder.recording.width.TrackMapWidthProfiler
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapBounds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrackMapSavePreparerTest {

    @Test
    fun `prepareSaveRequest rejects blank track id`() {
        val fixture = fixture(
            initialState = TrackMapRecorderState(
                gameId = "ac",
                recording = false,
            ),
        )
        fixture.runtime.lapPoints += Vec2(0f, 0f)
        fixture.runtime.lapPoints += Vec2(1f, 0f)

        val request = fixture.preparer.prepareSaveRequest()

        assertNull(request)
        assertEquals("Track id is missing", fixture.state.value.message)
        assertFalseSaving(fixture.state.value.isSaving)
    }

    @Test
    fun `prepareSaveRequest rejects insufficient points`() {
        val fixture = fixture(
            initialState = TrackMapRecorderState(
                gameId = "ac",
                trackId = "spa_gp",
                trackName = "Spa GP",
            ),
        )
        fixture.runtime.lapPoints += Vec2(0f, 0f)

        val request = fixture.preparer.prepareSaveRequest()

        assertNull(request)
        assertEquals("Not enough points to save", fixture.state.value.message)
        assertFalseSaving(fixture.state.value.isSaving)
    }

    @Test
    fun `prepareSaveRequest prefers centerline points and builds calibration`() {
        val centerline = listOf(
            Vec2(0f, 0f),
            Vec2(10f, 0f),
            Vec2(20f, 0f),
        )
        val fixture = fixture(
            initialState = TrackMapRecorderState(
                gameId = "ac",
                trackId = "spa_gp",
                trackName = "Spa GP",
                fallbackHalfWidthMeters = 5.5f,
            ),
            minPointsToSave = 2,
        )
        fixture.runtime.centerlinePoints += centerline
        fixture.runtime.mapPoints += Vec2(99f, 99f)
        fixture.runtime.lapPoints += Vec2(77f, 77f)
        fixture.runtime.ensureEdgeCapacity(centerline.size)
        for (index in centerline.indices) {
            fixture.runtime.leftEdgeWidthMeters[index] = 4f
            fixture.runtime.rightEdgeWidthMeters[index] = 4.5f
        }
        fixture.runtime.bounds = TrackMapBounds(minX = 0f, minY = 0f, maxX = 20f, maxY = 0f)
        fixture.runtime.sectorCount = 1
        fixture.runtime.recordSectorSample(
            sectorStartIndex = 1,
            lapIndex = 1,
            point = centerline.first(),
        )
        fixture.runtime.pitPoints += Vec2(-1f, -1f)
        fixture.runtime.pitEntryPoint = centerline.first()
        fixture.runtime.pitExitPoint = centerline.last()

        val request = fixture.preparer.prepareSaveRequest()

        assertNotNull(request)
        assertEquals(centerline, request.payload.points)
        assertEquals("spa_gp", request.payload.trackId)
        assertNotNull(request.calibration)
        assertEquals(1, request.calibration.sectors.size)
        assertTrue(fixture.state.value.isSaving)
        assertEquals("Saving track map...", fixture.state.value.message)
    }

    private fun fixture(
        initialState: TrackMapRecorderState,
        minPointsToSave: Int = 2,
    ): Fixture {
        val runtime = TrackMapRecorderRuntime()
        val state = MutableStateFlow(initialState)
        val widthProfiler = TrackMapWidthProfiler(
            stats = TrackMapStatsCalculator(),
            minPointsToSave = minPointsToSave,
        )
        return Fixture(
            runtime = runtime,
            state = state,
            preparer = TrackMapSavePreparer(
                runtime = runtime,
                runtimeState = state,
                widthProfiler = widthProfiler,
                minPointsToSave = minPointsToSave,
            ),
        )
    }

    private fun assertFalseSaving(value: Boolean) {
        assertEquals(false, value)
    }

    private data class Fixture(
        val runtime: TrackMapRecorderRuntime,
        val state: MutableStateFlow<TrackMapRecorderState>,
        val preparer: TrackMapSavePreparer,
    )
}
