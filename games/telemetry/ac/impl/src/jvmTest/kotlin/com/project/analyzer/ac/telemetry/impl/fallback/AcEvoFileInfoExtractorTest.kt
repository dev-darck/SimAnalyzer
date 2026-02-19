package com.project.analyzer.ac.telemetry.impl.fallback

import com.project.analyzer.ac.telemetry.impl.fallback.logfile.AcEvoFileInfoExtractor
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.AcEvoLogLocator
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoFileInfo
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoSessionType
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AcEvoFileInfoExtractorTest {

    private fun writeLines(file: File, lines: List<String>) {
        file.parentFile?.mkdirs()
        file.writeText(lines.joinToString(separator = "\n", postfix = "\n"))
    }

    private fun appendLines(file: File, lines: List<String>) {
        file.appendText(lines.joinToString(separator = "\n", postfix = "\n"))
    }

    @Test
    fun `primeFromTail parses track car driver without consuming new lines`() {
        val dir = Files.createTempDirectory("acevo-extractor").toFile()
        val log = File(dir, "log.txt")

        writeLines(
            log,
            listOf(
                "[2026-01-31 12:00:00.123] Creating physics track: Spa Francorchamps",
                "[2026-01-31 12:00:00.124] TRACK NAME spa gp",
                "[2026-01-31 12:00:00.126] connecting gamecar foo (John Doe | 123456)",
                "[2026-01-31 12:00:00.125] Game Started! | Spa Francorchamps practice | ks_ferrari_488_gt3",
            )
        )

        val locator = mockk<AcEvoLogLocator>()
        every { locator.locateLogFile() } returns log

        val ex = AcEvoFileInfoExtractor(locator)

        val info: EvoFileInfo = ex.poll()

        assertTrue(info.trackId!!.contains("gp") || info.trackId == "spa_francorchamps", "trackId=${info.trackId}")
        val tn = info.trackName.orEmpty()
        assertTrue(tn.contains("Spa") || tn == info.trackId, "trackName=${info.trackName} trackId=${info.trackId}")
        assertEquals("ks_ferrari_488_gt3", info.carModel)
        assertEquals("John Doe", info.driverName)
        assertEquals("123456", info.driverSteamId)
        assertTrue(info.sessionEpoch > 0)
    }

    @Test
    fun `hard boundary bumps session epoch`() {
        val dir = Files.createTempDirectory("acevo-extractor2").toFile()
        val log = File(dir, "log.txt")

        writeLines(
            log,
            listOf(
                "[2026-01-31 12:00:00.123] Creating physics track: Spa Francorchamps",
                "[2026-01-31 12:00:00.124] TRACK NAME spa gp"
            )
        )

        val locator = mockk<AcEvoLogLocator>()
        every { locator.locateLogFile() } returns log
        val ex = AcEvoFileInfoExtractor(locator)

        val i1 = ex.poll()
        val e1 = i1.sessionEpoch

        appendLines(
            log,
            listOf(
                "[2026-01-31 12:01:00.000] Reset session requested"
            )
        )

        val i2 = ex.poll()
        val e2 = i2.sessionEpoch

        assertTrue(e2 > e1, "epoch did not bump: e1=$e1 e2=$e2")
    }

    @Test
    fun `penalty lines are grouped within window`() {
        val dir = Files.createTempDirectory("acevo-extractor3").toFile()
        val log = File(dir, "log.txt")

        writeLines(
            log,
            listOf(
                "[2026-01-31 12:00:00.000] Creating physics track: Spa Francorchamps",
                "[2026-01-31 12:00:00.001] TRACK NAME spa gp"
            )
        )

        val locator = mockk<AcEvoLogLocator>()
        every { locator.locateLogFile() } returns log
        val ex = AcEvoFileInfoExtractor(locator)

        ex.poll()

        appendLines(
            file = log,
            lines = listOf(
                "[2026-01-31 12:00:05.000] UiNotificationType_SessionPenalty something"
            )
        )
        val p1 = ex.poll()
        assertTrue(p1.hasPenalty)
        val id1 = p1.penaltyId
        assertNotNull(id1)

        appendLines(
            file = log,
            lines = listOf(
                "[2026-01-31 12:00:07.000] penalty type PenaltyType_StopAndGo something"
            )
        )
        val p2 = ex.poll()
        val id2 = p2.penaltyId
        assertEquals(id1, id2)
    }

    @Test
    fun `track change updates track id but does not bump epoch without boundary`() {
        val dir = Files.createTempDirectory("acevo-extractor4").toFile()
        val log = File(dir, "log.txt")

        writeLines(
            file = log,
            lines = listOf(
                "[2026-01-31 12:00:00.000] Creating physics track: Spa Francorchamps",
                "[2026-01-31 12:00:00.001] TRACK NAME spa gp"
            )
        )

        val locator = mockk<AcEvoLogLocator>()
        every { locator.locateLogFile() } returns log
        val ex = AcEvoFileInfoExtractor(locator)

        val i1 = ex.poll()
        val e1 = i1.sessionEpoch

        appendLines(
            file = log,
            lines = listOf(
                "[2026-01-31 12:10:00.000] Creating physics track: Monza",
                "[2026-01-31 12:10:00.001] TRACK NAME monza gp"
            )
        )

        val i2 = ex.poll()
        val e2 = i2.sessionEpoch

        assertEquals("epoch should not bump on track change alone: e1=$e1 e2=$e2", e1, e2)
        assertNotNull(i2.trackId)
        assertTrue(i2.trackId.contains("monza"), "trackId=${i2.trackId}")
    }

    @Test
    fun `dynamic track preset line resolves unknown track without hardcoded map`() {
        val dir = Files.createTempDirectory("acevo-extractor5").toFile()
        val log = File(dir, "log.txt")

        writeLines(
            file = log,
            lines = listOf(
                "[2026-01-31 12:00:00.000] Creating physics track: Some Fancy Track",
                "[2026-01-31 12:00:00.010] Loading DynamicTrack preset: content\\tracks\\my_custom_track\\dynamic_track\\Sprint.dynamictrackpresetcompressed"
            )
        )

        val locator = mockk<AcEvoLogLocator>()
        every { locator.locateLogFile() } returns log
        val ex = AcEvoFileInfoExtractor(locator)

        val info = ex.poll()

        assertEquals("my_custom_track_sprint", info.trackId)
    }

    @Test
    fun `layout track file line resolves track id when container or slug is missing`() {
        val dir = Files.createTempDirectory("acevo-extractor6").toFile()
        val log = File(dir, "log.txt")

        writeLines(
            file = log,
            lines = listOf(
                "[2026-01-31 12:00:00.000] Creating physics track: New Fantasy Track",
                "[2026-01-31 12:00:00.050] Loaded 0 TrackLayoutBezier points from content\\tracks\\new_fantasy_track\\layouts\\layout_2.track_layout"
            )
        )

        val locator = mockk<AcEvoLogLocator>()
        every { locator.locateLogFile() } returns log
        val ex = AcEvoFileInfoExtractor(locator)

        val info = ex.poll()

        assertEquals("new_fantasy_track_2", info.trackId)
    }

    @Test
    fun `game started line sets session type from game mode marker`() {
        val dir = Files.createTempDirectory("acevo-extractor7").toFile()
        val log = File(dir, "log.txt")

        writeLines(
            file = log,
            lines = listOf(
                "[2026-01-31 12:00:00.000] Game Started! GameModeType_QUALIFYING | Monza GP Qualifying 1200 seconds @2014/8/15 10:45:0 | ks_bmw_m4_gt3"
            )
        )

        val locator = mockk<AcEvoLogLocator>()
        every { locator.locateLogFile() } returns log
        val ex = AcEvoFileInfoExtractor(locator)

        val info = ex.poll()

        assertEquals(EvoSessionType.QUALIFYING, info.sessionType)
    }

    @Test
    fun `track name refreshes from new track id after session boundary even without physics line`() {
        val dir = Files.createTempDirectory("acevo-extractor8").toFile()
        val log = File(dir, "log.txt")

        writeLines(
            file = log,
            lines = listOf(
                "[2026-01-31 12:00:00.000] Creating physics track: Old Track",
                "[2026-01-31 12:00:00.010] TRACK NAME old_track gp"
            )
        )

        val locator = mockk<AcEvoLogLocator>()
        every { locator.locateLogFile() } returns log
        val ex = AcEvoFileInfoExtractor(locator)

        val first = ex.poll()
        assertEquals("old_track_gp", first.trackId)
        assertNotNull(first.trackName)

        appendLines(
            file = log,
            lines = listOf(
                "[2026-01-31 12:09:00.000] Reset session requested",
                "[2026-01-31 12:10:00.000] Loading DynamicTrack preset: content\\tracks\\new_track\\dynamic_track\\Sprint.dynamictrackpresetcompressed"
            )
        )

        val second = ex.poll()
        assertEquals("new_track_sprint", second.trackId)
        assertEquals("new_track_sprint", second.trackName)
    }
}
