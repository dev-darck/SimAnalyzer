package com.analyzer.session.data.analysis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IndexAnalyzerTest {

    @Test
    fun `build finalizes active lap when session ends without lap transition`() {
        val analyzer = IndexAnalyzer()

        analyzer.consume(
            IndexRecord(
                timestampNs = 0L,
                speedKmh = null,
                lap = 1,
                sector = 0,
                flags = 0,
            ),
        )
        analyzer.consume(
            IndexRecord(
                timestampNs = 10_000_000_000L,
                speedKmh = null,
                lap = 1,
                sector = 1,
                flags = 0,
            ),
        )
        analyzer.consume(
            IndexRecord(
                timestampNs = 20_000_000_000L,
                speedKmh = null,
                lap = 1,
                sector = 2,
                flags = 0,
            ),
        )
        analyzer.consume(
            IndexRecord(
                timestampNs = 30_000_000_000L,
                speedKmh = null,
                lap = 1,
                sector = 2,
                flags = 0,
            ),
        )

        val analysis = analyzer.build()

        val lap = analysis.laps.single()
        assertTrue(lap.complete)
        assertEquals(30_000, lap.totalTimeMs)
        assertEquals(listOf(10_000, 10_000, 10_000), lap.sectorTimesMs)
    }
}
