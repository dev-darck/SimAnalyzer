package com.project.analyzer.ac.telemetry.impl.calibration

import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BundledTrackCalibrationResourcesTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun `bundled calibration files use canonical names`() {
        val mismatches = resourceFiles().mapNotNull { file ->
            val calibration = readCalibration(file)
            val expectedFileName = TrackCalibrationFileNameResolver.resolveFileName(
                trackId = calibration.trackId,
                layoutId = calibration.layoutId,
            )
            if (file.name == expectedFileName) {
                null
            } else {
                "${file.name} -> $expectedFileName"
            }
        }

        assertTrue(
            mismatches.isEmpty(),
            "Found non-canonical bundled calibration files: ${mismatches.joinToString()}",
        )
    }

    @Test
    fun `bundled calibration index matches resource files`() {
        val expectedEntries = resourceFiles()
            .map(File::getName)
            .sorted()
        val indexFile = resourcesDir().resolve(TrackCalibrationFileNameResolver.INDEX_FILE_NAME)
        val actualEntries = indexFile.readLines(StandardCharsets.UTF_8)
            .map(String::trim)
            .filter(String::isNotBlank)
            .filterNot { it.startsWith("#") }

        assertEquals(expectedEntries, actualEntries)
    }

    private fun readCalibration(file: File): TrackCalibration = json.decodeFromString(
        deserializer = TrackCalibration.serializer(),
        string = file.readText(StandardCharsets.UTF_8),
    )

    private fun resourceFiles(): List<File> = resourcesDir().listFiles()
        ?.filter { it.isFile && it.extension.equals("json", ignoreCase = true) }
        ?.sortedBy(File::getName)
        ?: emptyList()

    private fun resourcesDir(): File {
        val resource = assertNotNull(
            javaClass.getResource("/${TrackCalibrationFileNameResolver.DIRECTORY_NAME}"),
            "Missing bundled calibration resources on test classpath",
        )
        return Paths.get(resource.toURI()).toFile()
    }
}
