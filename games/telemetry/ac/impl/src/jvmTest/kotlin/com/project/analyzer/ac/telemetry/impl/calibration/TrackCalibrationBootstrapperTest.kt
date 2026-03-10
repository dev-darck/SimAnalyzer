package com.project.analyzer.ac.telemetry.impl.calibration

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

class TrackCalibrationBootstrapperTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun `bootstrap copies bundled calibrations into app folder as game source`() = runTest {
        val root = createTempDirectory("track-calibration-bootstrap").toFile()
        try {
            val repository = TrackCalibrationStoreRepository(
                json = json,
                appDirectories = root.asAppDirectories(),
                ioDispatcher = StandardTestDispatcher(testScheduler),
            )
            val bootstrapper = TrackCalibrationBootstrapper(
                json = json,
                repository = repository,
                ioDispatcher = StandardTestDispatcher(testScheduler),
            )

            bootstrapper.ensureBundledCalibrationsInstalled()

            val resolved = repository.load(trackId = "imola_gp", layoutId = "gp")

            assertNotNull(resolved)
            assertEquals("imola_gp", resolved.trackId)
            assertEquals(TrackCalibrationSource.GAME, resolved.source)
        } finally {
            root.deleteRecursively()
        }
    }

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
