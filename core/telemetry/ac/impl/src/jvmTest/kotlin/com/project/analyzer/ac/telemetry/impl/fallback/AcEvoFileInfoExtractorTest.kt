package com.project.analyzer.ac.telemetry.impl.fallback

import com.project.analyzer.ac.telemetry.impl.fallback.logfile.AcEvoFileInfoExtractor
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.AcEvoLogLocator
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoFileInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
                "[2026-01-31 12:00:00.125] Creating car: ks_ferrari_488_gt3",
                "[2026-01-31 12:00:00.126] connecting gamecar foo (John Doe | 123456)"
            )
        )

        val locator = mockk<AcEvoLogLocator>()
        every { locator.locateLogFile() } returns log

        val ex = AcEvoFileInfoExtractor(locator)

        val info: EvoFileInfo = ex.poll()

        assertNotNull(info.trackId)
        assertTrue(info.trackId.contains("spa_francorchamps"), "trackId=${info.trackId}")
        assertTrue(info.trackId.contains("gp"), "trackId=${info.trackId}")
        assertTrue((info.trackName ?: "").contains("Spa"), "trackName=${info.trackName}")
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
    fun `track change triggers epoch bump`() {
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

        assertTrue(e2 > e1, "epoch did not bump on track change: e1=$e1 e2=$e2")
        assertNotNull(i2.trackId)
        assertTrue(i2.trackId.contains("monza"), "trackId=${i2.trackId}")
    }
}
