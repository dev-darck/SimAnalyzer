package com.project.analyzer.ac.telemetry.impl.fallback.logfile

import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoFileInfo
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoSessionType
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class FileInfoExtractorStabilizerTest {

    @Test
    fun `poll stabilizes track identity before promoting a new candidate`() {
        val upstream = mockk<EvoFileInfoSource>(relaxUnitFun = true)
        every { upstream.poll() } returnsMany listOf(
            EvoFileInfo(
                trackId = "paul_ricard_3c",
                layoutId = "3c",
                trackName = "Paul Ricard 3C",
                sessionType = EvoSessionType.PRACTICE,
            ),
            EvoFileInfo(
                trackId = "brands_hatch_indy",
                layoutId = "indy",
                trackName = "Brands Hatch Indy",
                sessionType = EvoSessionType.PRACTICE,
            ),
            EvoFileInfo(
                trackId = "brands_hatch_indy",
                layoutId = "indy",
                trackName = "Brands Hatch Indy",
                sessionType = EvoSessionType.PRACTICE,
            ),
            EvoFileInfo(
                trackId = "brands_hatch_indy",
                layoutId = "indy",
                trackName = "Brands Hatch Indy",
                sessionType = EvoSessionType.PRACTICE,
            ),
        )

        val stabilizer = FileInfoExtractorStabilizer(upstream)

        val first = stabilizer.poll()
        val second = stabilizer.poll()
        val third = stabilizer.poll()
        val fourth = stabilizer.poll()

        assertEquals("paul_ricard_3c", first.trackId)
        assertEquals("paul_ricard_3c", second.trackId)
        assertEquals("paul_ricard_3c", third.trackId)
        assertEquals("brands_hatch_indy", fourth.trackId)
        assertEquals(EvoSessionType.PRACTICE, fourth.sessionType)
    }
}
