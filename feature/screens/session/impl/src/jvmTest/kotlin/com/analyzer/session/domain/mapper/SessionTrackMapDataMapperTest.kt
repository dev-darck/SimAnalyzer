package com.analyzer.session.domain.mapper

import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMap
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapBounds
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapPoint
import com.project.analyzer.utils.trackmap.TrackMapPreparationUtil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SessionTrackMapDataMapperTest {

    private val trackMapPreparationUtil = TrackMapPreparationUtil()

    @Test
    fun `toTrackMapData keeps track widths for minimap line rendering`() {
        val source = TrackMap(
            gameId = "ace",
            trackId = "suzuka_gp",
            trackName = "Suzuka GP",
            layoutId = "gp",
            createdAtEpochMs = 1L,
            points = listOf(
                TrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 7f),
                TrackMapPoint(x = 100f, y = 50f, leftWidthMeters = 5f, rightWidthMeters = 8f),
            ),
            bounds = TrackMapBounds(
                minX = 0f,
                minY = 0f,
                maxX = 100f,
                maxY = 50f,
            ),
        )

        val mapped = source.toTrackMapData(trackMapPreparationUtil)

        assertNotNull(mapped)
        assertEquals(6f, mapped.points.first().leftWidthMeters)
        assertEquals(7f, mapped.points.first().rightWidthMeters)
        assertEquals(5f, mapped.points.last().leftWidthMeters)
        assertEquals(8f, mapped.points.last().rightWidthMeters)
    }

    @Test
    fun `toTrackMapData prefers ideal line and ignores outlier bounds`() {
        val source = TrackMap(
            gameId = "ace",
            trackId = "suzuka_gp",
            trackName = "Suzuka GP",
            layoutId = "gp",
            createdAtEpochMs = 1L,
            points = listOf(
                TrackMapPoint(x = -500f, y = -500f),
                TrackMapPoint(x = 500f, y = 500f),
            ),
            pitPoints = listOf(
                TrackMapPoint(x = 10_000f, y = 10_000f),
            ),
            idealLinePoints = listOf(
                TrackMapPoint(x = 0f, y = 0f),
                TrackMapPoint(x = 20f, y = 10f),
                TrackMapPoint(x = 40f, y = 0f),
            ),
            bounds = TrackMapBounds(
                minX = -1000f,
                minY = -1000f,
                maxX = 10_000f,
                maxY = 10_000f,
            ),
        )

        val mapped = source.toTrackMapData(trackMapPreparationUtil)

        assertNotNull(mapped)
        assertEquals(3, mapped.points.size)
        assertEquals(0f, mapped.bounds.minX)
        assertEquals(0f, mapped.bounds.minY)
        assertEquals(40f, mapped.bounds.maxX)
        assertEquals(10f, mapped.bounds.maxY)
    }
}
