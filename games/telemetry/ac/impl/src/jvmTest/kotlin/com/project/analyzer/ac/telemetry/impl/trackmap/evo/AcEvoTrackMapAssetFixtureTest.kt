package com.project.analyzer.ac.telemetry.impl.trackmap.evo

import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AcEvoTrackMapAssetFixtureTest {

    private val parsers = AcEvoTrackMapAssetParsers(
        json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        },
    )

    @Test
    fun `real sebring spline fixture parses expected point cloud`() {
        val points = parsers.parseSplineJson(fixturePath("content/tracks/sebring/containers/layout_gp.splinedata.json"))

        assertEquals(3325, points.size)
        assertEquals(-158.021f, points.first().x, 0.01f)
        assertEquals(149.095f, points.first().y, 0.01f)
        assertTrue(points.minOf { point -> point.x } < -590f)
        assertTrue(points.minOf { point -> point.y } < -620f)
        assertTrue(points.maxOf { point -> point.x } > 590f)
        assertTrue(points.maxOf { point -> point.y } > 360f)
    }

    @Test
    fun `real sebring ideal line fixture parses expected racing line`() {
        val points = parsers.parseAiSpline(fixturePath("content/tracks/sebring/layouts/layout_gp.ideal_line.aisplinedata"))

        assertEquals(3313, points.size)
        assertEquals(-158.021f, points.first().x, 0.01f)
        assertEquals(149.095f, points.first().y, 0.01f)
        assertTrue(points.minOf { point -> point.x } < -590f)
        assertTrue(points.minOf { point -> point.y } < -620f)
        assertTrue(points.maxOf { point -> point.x } > 590f)
        assertTrue(points.maxOf { point -> point.y } > 360f)
    }

    @Test
    fun `real sebring control points fixture preserves widths and markers`() {
        val points = parsers.parseTrackControlPoints(fixturePath("content/tracks/sebring/layouts/layout_gp.trackcontrolpoints"))

        assertEquals(62, points.size)
        assertEquals(-57.831f, points.first().x, 0.01f)
        assertEquals(150.636f, points.first().y, 0.01f)
        assertEquals(2.551f, points.first().leftWidthMeters, 0.01f)
        assertEquals(2.551f, points.first().rightWidthMeters, 0.01f)
        assertEquals(62, points.count { point -> point.leftWidthMeters > 0f && point.rightWidthMeters > 0f })
        assertEquals(62, points.count { point -> point.forwardX != null && point.forwardY != null })
        assertEquals(17, points.count(AcEvoTrackSample::hasMarker17))
        assertEquals(2, points.count(AcEvoTrackSample::hasMarker18))
    }

    @Test
    fun `real sebring svg fixture stays under runtime alias name`() {
        val svgPath = fixturePath("uiresources/images/trackmaps/sebring_international_raceway-gp.svg")

        assertTrue(Files.isRegularFile(svgPath))
        val svg = svgPath.readText()
        assertTrue(svg.contains("<svg", ignoreCase = true))
        assertTrue(svgPath.fileName.toString().contains("sebring_international_raceway", ignoreCase = true))
    }

    private fun fixturePath(relativePath: String): Path = Path.of(
        requireNotNull(javaClass.getResource("/fixtures/acevo/sebring/$relativePath")) {
            "Missing fixture: $relativePath"
        }.toURI(),
    )
}
