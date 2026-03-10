package com.project.analyzer.ac.telemetry.impl.calibration

import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.telemetry.ac.api.model.calibration.SectorCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibrationSource
import com.project.analyzer.utils.AppDirectories
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TrackCalibrationStoreRepositoryTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun `save writes calibration into app folder and load reads it back`() = runTest {
        val root = createTempDirectory("track-calibration-store").toFile()
        try {
            val repository = TrackCalibrationStoreRepository(
                json = json,
                appDirectories = root.asAppDirectories(),
                ioDispatcher = StandardTestDispatcher(testScheduler),
            )
            val calibration = calibration(
                trackId = "brands_hatch_gp",
                trackName = "Brands Hatch GP",
                layoutId = "gp",
                source = TrackCalibrationSource.USER,
            )

            repository.save(calibration, TrackCalibrationSource.USER)

            val resolved = repository.load(trackId = "brands_hatch_gp", layoutId = "gp")
            val storedFile = root
                .resolve("user")
                .resolve(TrackCalibrationFileNameResolver.DIRECTORY_NAME)
                .resolve("brands_hatch_gp.json")

            assertNotNull(resolved)
            assertEquals("brands_hatch_gp", resolved.trackId)
            assertEquals("gp", resolved.layoutId)
            assertEquals(TrackCalibrationSource.USER, resolved.source)
            assertEquals(true, storedFile.isFile)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `load resolves layout specific file from base track id`() = runTest {
        val root = createTempDirectory("track-calibration-layout").toFile()
        try {
            val repository = TrackCalibrationStoreRepository(
                json = json,
                appDirectories = root.asAppDirectories(),
                ioDispatcher = StandardTestDispatcher(testScheduler),
            )
            repository.save(
                calibration = calibration(
                    trackId = "paul_ricard_3c",
                    trackName = "Paul Ricard 3C",
                    layoutId = "3c",
                    source = TrackCalibrationSource.GAME,
                ),
                source = TrackCalibrationSource.GAME,
            )

            val resolved = repository.load(trackId = "paul_ricard", layoutId = "3c")

            assertNotNull(resolved)
            assertEquals("paul_ricard_3c", resolved.trackId)
            assertEquals("3c", resolved.layoutId)
            assertEquals(TrackCalibrationSource.GAME, resolved.source)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `loadBySource filters stored calibration by source`() = runTest {
        val root = createTempDirectory("track-calibration-source").toFile()
        try {
            val repository = TrackCalibrationStoreRepository(
                json = json,
                appDirectories = root.asAppDirectories(),
                ioDispatcher = StandardTestDispatcher(testScheduler),
            )
            repository.save(
                calibration = calibration(
                    trackId = "imola_gp",
                    trackName = "Imola GP",
                    layoutId = "gp",
                    source = TrackCalibrationSource.USER,
                ),
                source = TrackCalibrationSource.USER,
            )

            val resolvedUser = repository.loadBySource(
                trackId = "imola_gp",
                source = TrackCalibrationSource.USER,
                layoutId = "gp",
            )
            val resolvedGame = repository.loadBySource(
                trackId = "imola_gp",
                source = TrackCalibrationSource.GAME,
                layoutId = "gp",
            )

            assertNotNull(resolvedUser)
            assertEquals(TrackCalibrationSource.USER, resolvedUser.source)
            assertNull(resolvedGame)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `load resolves telemetry alias to stored calibration`() = runTest {
        val root = createTempDirectory("track-calibration-alias").toFile()
        try {
            val repository = TrackCalibrationStoreRepository(
                json = json,
                appDirectories = root.asAppDirectories(),
                ioDispatcher = StandardTestDispatcher(testScheduler),
            )
            repository.save(
                calibration = calibration(
                    trackId = "watkins_glen_short_inner_loop",
                    trackName = "Watkins Glen Short Inner Loop",
                    layoutId = "short_inner_loop",
                    source = TrackCalibrationSource.USER,
                ),
                source = TrackCalibrationSource.USER,
            )

            val resolved = repository.load(
                trackId = "watkins_glen_international_short_inner_loop",
                layoutId = null,
            )

            assertNotNull(resolved)
            assertEquals("watkins_glen_short_inner_loop", resolved.trackId)
            assertEquals("short_inner_loop", resolved.layoutId)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `loadAll returns calibrations from app folder only`() = runTest {
        val root = createTempDirectory("track-calibration-load-all").toFile()
        try {
            val repository = TrackCalibrationStoreRepository(
                json = json,
                appDirectories = root.asAppDirectories(),
                ioDispatcher = StandardTestDispatcher(testScheduler),
            )
            repository.save(
                calibration = calibration(
                    trackId = "brands_hatch_gp",
                    trackName = "Brands Hatch GP",
                    layoutId = "gp",
                    source = TrackCalibrationSource.GAME,
                ),
                source = TrackCalibrationSource.GAME,
            )
            repository.save(
                calibration = calibration(
                    trackId = "brands_hatch_indy",
                    trackName = "Brands Hatch Indy",
                    layoutId = "indy",
                    source = TrackCalibrationSource.USER,
                ),
                source = TrackCalibrationSource.USER,
            )

            assertEquals(2, repository.loadAll().size)
            assertEquals(1, repository.loadAll(TrackCalibrationSource.GAME).size)
            assertEquals(1, repository.loadAll(TrackCalibrationSource.USER).size)
        } finally {
            root.deleteRecursively()
        }
    }

    private fun calibration(
        trackId: String,
        trackName: String,
        layoutId: String?,
        source: TrackCalibrationSource,
    ): TrackCalibration = TrackCalibration(
        trackId = trackId,
        trackName = trackName,
        layoutId = layoutId,
        createdAtEpochMs = 1_000L,
        source = source,
        referencePoint = ReferencePoint.FRONT_AXLE,
        startFinish = gate(centerX = 0f),
        sectors = listOf(
            SectorCalibration(
                index = 1,
                start = gate(centerX = 0f),
                finish = gate(centerX = 20f),
            ),
        ),
    )

    private fun gate(centerX: Float): Gate = Gate.create(
        center = Vec2(centerX, 0f),
        forward = Vec2(1f, 0f),
        normal = Vec2(0f, 1f),
        halfWidthMeters = 6f,
    )

    private fun File.asAppDirectories(): AppDirectories = object : AppDirectories {
        override val dataDir = this@asAppDirectories
        override val preferencesDir = resolve("preferences").apply { mkdirs() }
        override val cacheDir = resolve("cache").apply { mkdirs() }
        override val logsDir = resolve("logs").apply { mkdirs() }
        override val userDataDir = resolve("user").apply { mkdirs() }
        override val runtimeDir = resolve("runtime").apply { mkdirs() }
        override val lockFile = runtimeDir.resolve("app.lock")
    }
}
