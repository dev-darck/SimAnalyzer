package com.analyzer.trackmap.builder.recording.engine

import com.analyzer.trackmap.data.library.TrackMapStatsCalculator
import com.analyzer.trackmap.builder.recording.lap.TrackMapLapTracker
import com.analyzer.trackmap.builder.recording.lap.TrackMapMerger
import com.analyzer.trackmap.builder.recording.point.TrackMapPointFilter
import com.analyzer.trackmap.builder.recording.runtime.TrackMapRecorderRuntime
import com.analyzer.trackmap.builder.recording.runtime.TrackMapRecorderState
import com.analyzer.trackmap.builder.recording.width.TrackMapWidthProfiler
import com.project.analyzer.math.Vec2
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrackMapFrameProcessorTest {

    @Test
    fun `processFrame updates session info and publishes accepted point`() {
        val fixture = fixture()

        fixture.processor.processFrame(
            frameContext(
                timestampNs = 1L,
                currentPos = Vec2(1f, 2f),
                trackId = "spa_gp",
                trackName = "Spa GP",
                lapIndex = 1,
                safeSpeed = 120f,
            ),
        )

        val state = fixture.state.value
        assertEquals("spa_gp", state.trackId)
        assertEquals("Spa GP", state.trackName)
        assertEquals(Vec2(1f, 2f), state.currentPosition)
        assertEquals(120f, state.currentSpeedKmh)
        assertEquals(1, state.pointCount)
        assertEquals(listOf(Vec2(1f, 2f)), state.points)
    }

    @Test
    fun `processFrame stops recording on teleport`() {
        val fixture = fixture()

        fixture.processor.processFrame(
            frameContext(
                timestampNs = 1L,
                currentPos = Vec2(0f, 0f),
                lapIndex = 1,
                safeSpeed = 100f,
            ),
        )
        fixture.processor.processFrame(
            frameContext(
                timestampNs = 2L,
                currentPos = Vec2(100f, 0f),
                lapIndex = 1,
                safeSpeed = 100f,
            ),
        )

        val state = fixture.state.value
        assertFalse(state.recording)
        assertEquals("Teleport detected, recording stopped", state.message)
        assertEquals(1, state.pointCount)
    }

    @Test
    fun `processFrame accepts lap and captures single sector start`() {
        val fixture = fixture(minPointsToSave = 2)

        fixture.processor.processFrame(
            frameContext(
                timestampNs = 1L,
                currentPos = Vec2(0f, 0f),
                lapIndex = 1,
                safeSpeed = 120f,
                sectorCount = 1,
                currentSectorIndex = 0,
            ),
        )
        fixture.processor.processFrame(
            frameContext(
                timestampNs = 2L,
                currentPos = Vec2(10f, 0f),
                lapIndex = 1,
                safeSpeed = 120f,
                sectorCount = 1,
                currentSectorIndex = 0,
            ),
        )
        fixture.processor.processFrame(
            frameContext(
                timestampNs = 3L,
                currentPos = Vec2(20f, 0f),
                lapIndex = 2,
                safeSpeed = 120f,
                sectorCount = 1,
                currentSectorIndex = 0,
            ),
        )

        val state = fixture.state.value
        assertEquals(1, state.lapsRecorded)
        assertEquals(2, state.pointCount)
        assertEquals(1, state.sectorCount)
        assertEquals(1, state.capturedSectorCount)
        assertEquals(1, state.sectorMarkers.single().index)
        assertTrue(fixture.runtime.centerlinePoints.isNotEmpty())
    }

    @Test
    fun `setManualPit records entry point and pit samples`() {
        val position = Vec2(5f, 6f)
        val fixture = fixture(
            initialState = TrackMapRecorderState(
                recording = true,
                currentPosition = position,
            ),
        )

        fixture.processor.setManualPit(inPitLane = true, message = "Pit entry marked")

        val state = fixture.state.value
        assertEquals(position, state.pitEntryPoint)
        assertEquals(1, state.pitPointCount)
        assertEquals(listOf(position), state.pitPoints)
        assertEquals("Pit entry marked", state.message)
        assertEquals(true, fixture.runtime.manualPitLane)
        assertTrue(fixture.runtime.sawPitInLap)
    }

    private fun fixture(
        minPointsToSave: Int = 2,
        initialState: TrackMapRecorderState = TrackMapRecorderState(recording = true),
    ): Fixture {
        val runtime = TrackMapRecorderRuntime()
        val state = MutableStateFlow(initialState)
        val stats = TrackMapStatsCalculator()
        val widthProfiler = TrackMapWidthProfiler(stats = stats, minPointsToSave = minPointsToSave)
        val lapTracker = TrackMapLapTracker(
            merger = TrackMapMerger(
                insertDistanceMeters = 0.8f,
                minInsertSpacingMeters = 0.3f,
                searchWindow = 120,
            ),
            stats = stats,
            minPointsToSave = minPointsToSave,
        )
        val pointFilter = TrackMapPointFilter(
            stats = stats,
            teleportDistanceMeters = 80f,
            refinementSpacingMultiplier = 0.5f,
            minRefinedSpacingMeters = 0.2f,
        )
        return Fixture(
            runtime = runtime,
            state = state,
            processor = TrackMapFrameProcessor(
                runtime = runtime,
                runtimeState = state,
                widthProfiler = widthProfiler,
                pointFilter = pointFilter,
                lapTracker = lapTracker,
                config = TrackMapFrameProcessor.Config(
                    pitMinSpacingMeters = 0.75f,
                    uiUpdateIntervalNs = 0L,
                    infoUpdateIntervalNs = 0L,
                ),
            ),
        )
    }

    private fun frameContext(
        timestampNs: Long,
        currentPos: Vec2?,
        trackId: String? = null,
        trackName: String? = null,
        layoutId: String? = null,
        safeSpeed: Float? = null,
        sectorCount: Int? = null,
        currentSectorIndex: Int? = null,
        lapIndex: Int? = null,
        inPitLaneAuto: Boolean = false,
    ): TrackMapFrameContext = TrackMapFrameContext(
        safeSpeed = safeSpeed,
        sectorCount = sectorCount,
        currentSectorIndex = currentSectorIndex,
        inPitLaneAuto = inPitLaneAuto,
        lapIndex = lapIndex,
        timestampNs = timestampNs,
        currentPos = currentPos,
        trackId = trackId,
        trackName = trackName,
        layoutId = layoutId,
    )

    private data class Fixture(
        val runtime: TrackMapRecorderRuntime,
        val state: MutableStateFlow<TrackMapRecorderState>,
        val processor: TrackMapFrameProcessor,
    )
}
