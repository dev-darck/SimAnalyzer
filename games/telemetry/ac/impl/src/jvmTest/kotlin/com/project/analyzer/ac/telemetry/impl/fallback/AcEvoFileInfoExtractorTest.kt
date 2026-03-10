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
    fun `asset-backed watkins identity wins over ui slug alias`() {
        val dir = Files.createTempDirectory("acevo-extractor-watkins-canonical").toFile()
        val log = File(dir, "log.txt")

        writeLines(
            file = log,
            lines = listOf(
                "[2026-03-08 01:15:39.025] [dataUtils] [info] Loading scene Watkins Glen International",
                "[2026-03-08 01:15:39.029] [physics] [info] Loading DynamicTrack preset: content\\tracks\\watkins_glen\\dynamic_track\\Short Inner Loop.dynamictrackpresetcompressed",
                "[2026-03-08 01:15:39.208] [platformCore] [info] Container \"content\\tracks\\watkins_glen\\containers\\layout_short_inner_loop.scene\" activated correctly",
                "[2026-03-08 01:15:48.729] [gameface] [info] TRACK NAME watkins_glen_international short_inner_loop",
            )
        )

        val locator = mockk<AcEvoLogLocator>()
        every { locator.locateLogFile() } returns log
        val ex = AcEvoFileInfoExtractor(locator)

        val info = ex.poll()

        assertEquals("watkins_glen_short_inner_loop", info.trackId)
        assertEquals("short_inner_loop", info.layoutId)
    }

    @Test
    fun `later ui slug alias does not override canonical asset-backed track id`() {
        val dir = Files.createTempDirectory("acevo-extractor-watkins-stable").toFile()
        val log = File(dir, "log.txt")

        writeLines(
            file = log,
            lines = listOf(
                "[2026-03-08 01:15:39.029] [physics] [info] Loading DynamicTrack preset: content\\tracks\\watkins_glen\\dynamic_track\\Short Inner Loop.dynamictrackpresetcompressed",
                "[2026-03-08 01:15:39.208] [platformCore] [info] Container \"content\\tracks\\watkins_glen\\containers\\layout_short_inner_loop.scene\" activated correctly",
            )
        )

        val locator = mockk<AcEvoLogLocator>()
        every { locator.locateLogFile() } returns log
        val ex = AcEvoFileInfoExtractor(locator)

        val first = ex.poll()
        assertEquals("watkins_glen_short_inner_loop", first.trackId)

        appendLines(
            file = log,
            lines = listOf(
                "[2026-03-08 01:15:48.729] [gameface] [info] TRACK NAME watkins_glen_international short_inner_loop",
            )
        )

        val second = ex.poll()

        assertEquals("watkins_glen_short_inner_loop", second.trackId)
        assertEquals("short_inner_loop", second.layoutId)
        assertEquals(first.sessionEpoch, second.sessionEpoch)
    }

    @Test
    fun `non layout containers do not override actual spa layout`() {
        val dir = Files.createTempDirectory("acevo-extractor-spa-layout").toFile()
        val log = File(dir, "log.txt")

        writeLines(
            file = log,
            lines = listOf(
                "[2026-03-08 02:09:03.134] [gameplay] [info] Game Started! GameModeType_PRACTICE | Circuit de Spa Francorchamps GP Time Attack Practice  5400 seconds @2014/8/15 10:45:0 | ks_bmw_m4_gt3",
                "[2026-03-08 02:09:05.231] [physics] [info] Loading DynamicTrack preset: content\\tracks\\spa\\dynamic_track\\GP.dynamictrackpresetcompressed",
                "[2026-03-08 02:09:05.548] [platformCore] [info] Container \"content\\tracks\\spa\\containers\\camera_sequence_practice.scene\" activated correctly",
                "[2026-03-08 02:09:05.549] [platformCore] [info] Container \"content\\tracks\\spa\\containers\\layout_gp.scene\" activated correctly",
                "[2026-03-08 02:09:06.000] [gameface] [info] TRACK NAME circuit_de_spa_francorchamps gp",
            )
        )

        val locator = mockk<AcEvoLogLocator>()
        every { locator.locateLogFile() } returns log
        val ex = AcEvoFileInfoExtractor(locator)

        val info = ex.poll()

        assertEquals("spa_gp", info.trackId)
        assertEquals("gp", info.layoutId)
    }

    @Test
    fun `track aliases from log are canonicalized to imported asset ids`() {
        val dir = Files.createTempDirectory("acevo-extractor-aliases").toFile()
        val log = File(dir, "log.txt")

        writeLines(
            file = log,
            lines = listOf(
                "[2026-03-08 02:12:00.000] [gameface] [info] TRACK NAME red_bull_ring gp",
                "[2026-03-08 02:12:00.100] [gameplay] [info] Game Started! GameModeType_PRACTICE | Red Bull Ring GP Time Attack Practice 5400 seconds @2014/8/15 10:45:0 | ks_bmw_m4_gt3",
            )
        )

        val locator = mockk<AcEvoLogLocator>()
        every { locator.locateLogFile() } returns log
        val ex = AcEvoFileInfoExtractor(locator)

        val info = ex.poll()

        assertEquals("redbull_ring_gp", info.trackId)
        assertEquals("gp", info.layoutId)
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
    fun `split lines do not affect extracted identity or session epoch`() {
        val dir = Files.createTempDirectory("acevo-extractor-splits").toFile()
        val log = File(dir, "log.txt")

        writeLines(
            file = log,
            lines = listOf(
                "[2026-03-07 00:35:26.956] [gameplay] [info] Game Started! GameModeType_PRACTICE | Paul Ricard Layout 3A Time Attack Practice 5400 seconds | ks_bmw_m4_gt3",
                "[2026-03-07 00:35:38.190] [gameface] [info] TRACK NAME paul_ricard layout_3a",
            )
        )

        val locator = mockk<AcEvoLogLocator>()
        every { locator.locateLogFile() } returns log
        val ex = AcEvoFileInfoExtractor(locator)

        val before = ex.poll()

        appendLines(
            file = log,
            lines = listOf(
                "[2026-03-07 00:36:10.587] [gameplay] [info] Unexpected On Split",
                "[2026-03-07 00:36:38.220] [gameplay] [info] Unexpected On Split",
                "[2026-03-07 00:37:10.474] [gameplay] [info] split now 2 expected split 0 is_end 1",
                "[2026-03-07 00:37:10.474] [gameplay] [info] On Split start 1 end 1 id 2 splittime 32253",
                "[2026-03-07 00:37:10.475] [gameplay] [error] Couldn't create lap from opensplits (carId foo): Splitcollection 1/3, split times: 2,",
            )
        )

        val after = ex.poll()

        assertEquals(before.sessionEpoch, after.sessionEpoch)
        assertEquals("paul_ricard_3a", after.trackId)
        assertEquals(EvoSessionType.PRACTICE, after.sessionType)
        assertEquals("ks_bmw_m4_gt3", after.carModel)
    }

    @Test
    fun `real main menu marker marks the next session epoch as a new recording group`() {
        val dir = Files.createTempDirectory("acevo-extractor-main-menu").toFile()
        val log = File(dir, "log.txt")

        writeLines(
            file = log,
            lines = listOf(
                "[2026-01-31 12:00:00.000] Game Started! GameModeType_PRACTICE | Monza GP Practice 1200 seconds | ks_bmw_m4_gt3"
            )
        )

        val locator = mockk<AcEvoLogLocator>()
        every { locator.locateLogFile() } returns log
        val ex = AcEvoFileInfoExtractor(locator)

        val first = ex.poll()

        appendLines(
            file = log,
            lines = listOf(
                "[2026-01-31 12:05:00.000] UIGameModeEvent [object Object] goto menu.html,main/main",
                "[2026-01-31 12:05:10.000] Game Started! GameModeType_QUALIFYING | Monza GP Qualifying 900 seconds | ks_bmw_m4_gt3",
            )
        )

        val second = ex.poll()

        assertTrue(second.sessionEpoch > first.sessionEpoch)
        assertTrue(second.sessionEpochStartedFromMainMenu)
        assertEquals(EvoSessionType.QUALIFYING, second.sessionType)
    }

    @Test
    fun `stale tail does not force session type from previous run`() {
        val dir = Files.createTempDirectory("acevo-extractor7-stale").toFile()
        val log = File(dir, "log.txt")

        writeLines(
            file = log,
            lines = listOf(
                "[2026-01-31 12:00:00.000] InstantRaceRemote Race created"
            )
        )
        assertTrue(
            log.setLastModified(System.currentTimeMillis() - 10 * 60 * 1000),
            "failed to set stale mtime for test log",
        )

        val locator = mockk<AcEvoLogLocator>()
        every { locator.locateLogFile() } returns log
        val ex = AcEvoFileInfoExtractor(locator)

        val info = ex.poll()

        assertEquals(EvoSessionType.UNKNOWN, info.sessionType)
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

    @Test
    fun `generic game started track does not downgrade precise stable track id after boundary`() {
        val dir = Files.createTempDirectory("acevo-extractor-track-stability").toFile()
        val log = File(dir, "log.txt")

        writeLines(
            file = log,
            lines = listOf(
                "[2026-01-31 12:00:00.000] Creating physics track: Donington Park",
                "[2026-01-31 12:00:00.010] TRACK NAME donington_park national",
            ),
        )

        val locator = mockk<AcEvoLogLocator>()
        every { locator.locateLogFile() } returns log
        val ex = AcEvoFileInfoExtractor(locator)

        val first = ex.poll()
        assertEquals("donington_park_national", first.trackId)

        appendLines(
            file = log,
            lines = listOf(
                "[2026-01-31 12:05:00.000] Reset session requested",
                "[2026-01-31 12:05:01.000] Game Started! | Donington National practice | ks_bmw_m4_gt3",
            ),
        )

        val second = ex.poll()
        assertEquals("donington_park_national", second.trackId)
    }

    @Test
    fun `selected session lines drive weekend session transitions`() {
        val dir = Files.createTempDirectory("acevo-extractor9").toFile()
        val log = File(dir, "log.txt")

        writeLines(
            file = log,
            lines = listOf(
                "[2026-01-31 12:00:00.000] [gameface] [info] Selected session Practice true false",
                "[2026-01-31 12:00:00.000] [gameface] [info] Selected session Qualifying false false",
                "[2026-01-31 12:00:00.000] [gameface] [info] Selected session Race false false",
            )
        )

        val locator = mockk<AcEvoLogLocator>()
        every { locator.locateLogFile() } returns log
        val ex = AcEvoFileInfoExtractor(locator)

        val practice = ex.poll()
        assertEquals(EvoSessionType.PRACTICE, practice.sessionType)

        appendLines(
            file = log,
            lines = listOf(
                "[2026-01-31 12:05:00.000] [gameface] [info] Selected session Practice false true",
                "[2026-01-31 12:05:00.000] [gameface] [info] Selected session Qualifying true false",
                "[2026-01-31 12:05:00.000] [gameface] [info] Selected session Race false false",
            ),
        )

        val qualifying = ex.poll()
        assertEquals(EvoSessionType.QUALIFYING, qualifying.sessionType)

        appendLines(
            file = log,
            lines = listOf(
                "[2026-01-31 12:10:00.000] [gameface] [info] Selected session Practice false true",
                "[2026-01-31 12:10:00.000] [gameface] [info] Selected session Qualifying false true",
                "[2026-01-31 12:10:00.000] [gameface] [info] Selected session Race true false",
            ),
        )

        val race = ex.poll()
        assertEquals(EvoSessionType.RACE, race.sessionType)
    }

    @Test
    fun `goto_loadingpage lines update session type`() {
        val dir = Files.createTempDirectory("acevo-extractor10").toFile()
        val log = File(dir, "log.txt")

        writeLines(
            file = log,
            lines = listOf(
                "[2026-01-31 12:00:00.000] [gameface] [info] UIGameModeEvent [object Object] goto_loadingpage qualifying",
            ),
        )

        val locator = mockk<AcEvoLogLocator>()
        every { locator.locateLogFile() } returns log
        val ex = AcEvoFileInfoExtractor(locator)

        val qualifying = ex.poll()
        assertEquals(EvoSessionType.QUALIFYING, qualifying.sessionType)

        appendLines(
            file = log,
            lines = listOf(
                "[2026-01-31 12:01:00.000] [gameface] [info] UIGameModeEvent [object Object] goto_loadingpage race",
            ),
        )

        val race = ex.poll()
        assertEquals(EvoSessionType.RACE, race.sessionType)
    }

    @Test
    fun `remote created session type is not downgraded by later goto loading page`() {
        val dir = Files.createTempDirectory("acevo-extractor11").toFile()
        val log = File(dir, "log.txt")

        writeLines(
            file = log,
            lines = listOf(
                "[2026-01-31 12:00:00.000] [gameface] [info] Selected session Practice true false",
                "[2026-01-31 12:00:00.000] [gameface] [info] Selected session Qualifying false false",
                "[2026-01-31 12:00:05.000] TimeAttackRemote Qualifying created",
            ),
        )

        val locator = mockk<AcEvoLogLocator>()
        every { locator.locateLogFile() } returns log
        val ex = AcEvoFileInfoExtractor(locator)

        val qualifying = ex.poll()
        assertEquals(EvoSessionType.QUALIFYING, qualifying.sessionType)

        appendLines(
            file = log,
            lines = listOf(
                "[2026-01-31 12:01:00.000] [gameface] [info] UIGameModeEvent [object Object] goto_loadingpage timeattack",
            ),
        )

        val stillQualifying = ex.poll()
        assertEquals(EvoSessionType.QUALIFYING, stillQualifying.sessionType)
    }
}
