package com.project.analyzer.ac.telemetry.impl.trackmap.evo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AcEvoTrackCenterLineResolverTest {

    private val resolver = AcEvoTrackCenterLineResolver()

    @Test
    fun `resolve prefers ideal line when control points produce implausible path length`() {
        val idealLine = listOf(
            AcEvoTrackSample(x = 0f, y = 0f),
            AcEvoTrackSample(x = 50f, y = 0f),
            AcEvoTrackSample(x = 100f, y = 0f),
            AcEvoTrackSample(x = 150f, y = 0f),
        )
        val controlPoints = listOf(
            AcEvoTrackSample(x = 0f, y = 0f),
            AcEvoTrackSample(x = 1_000f, y = 0f),
            AcEvoTrackSample(x = 50f, y = 0f),
            AcEvoTrackSample(x = 150f, y = 0f),
        )

        val resolved = resolver.resolve(
            splineSamples = emptyList(),
            controlPoints = controlPoints,
            idealLine = idealLine,
        )

        assertSame(idealLine, resolved)
    }

    @Test
    fun `resolve keeps interpolated control points when they stay plausible against ideal line`() {
        val idealLine = listOf(
            AcEvoTrackSample(x = 0f, y = 0f),
            AcEvoTrackSample(x = 100f, y = 0f),
            AcEvoTrackSample(x = 200f, y = 0f),
        )
        val controlPoints = listOf(
            AcEvoTrackSample(x = 0f, y = 0f, forwardX = 1f, forwardY = 0f),
            AcEvoTrackSample(x = 100f, y = 5f, forwardX = 1f, forwardY = 0f),
            AcEvoTrackSample(x = 200f, y = 0f, forwardX = 1f, forwardY = 0f),
        )

        val resolved = resolver.resolve(
            splineSamples = emptyList(),
            controlPoints = controlPoints,
            idealLine = idealLine,
        )

        assertTrue(resolved.size > controlPoints.size)
        assertEquals(0f, resolved.first().x, 0.001f)
        assertEquals(200f, resolved.last().x, 0.001f)
    }
}
