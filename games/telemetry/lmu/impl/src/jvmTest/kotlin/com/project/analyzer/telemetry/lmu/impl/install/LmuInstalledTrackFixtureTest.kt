package com.project.analyzer.telemetry.lmu.impl.install

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LmuInstalledTrackFixtureTest {

    private val manifestParser = LmuInstalledTrackManifestParser()
    private val traceParser = LmuSceneLoadTraceParser()

    @Test
    fun `real LeMans manifest fixture exposes component and layout archives`() {
        val manifest = assertNotNull(
            manifestParser.parse(fixturePath("lemans_2023.mft")),
        )

        assertEquals("LeMans_2023", manifest.componentName)
        assertEquals("1.27", manifest.version)
        assertEquals(
            listOf("layoutLeMans.mas", "layoutMulsanne.mas", "shared.mas"),
            manifest.masFiles,
        )
    }

    @Test
    fun `real LeMans trace fixture links installed component to runtime track name`() {
        val manifest = assertNotNull(
            manifestParser.parse(fixturePath("lemans_2023.mft")),
        )
        val trace = assertNotNull(
            traceParser.parse(fixturePath("lemans_scene_load_trace.txt")),
        )

        assertEquals("LeMans_2023", trace.componentName)
        assertEquals("1.27", trace.version)
        assertEquals("layoutLeMans.mas", trace.layoutMasFileName)
        assertEquals("Circuit de la Sarthe", trace.runtimeTrackName)
        assertTrue(trace.layoutMasFileName in manifest.masFiles)
    }

    private fun fixturePath(fileName: String): Path = Path.of(
        requireNotNull(javaClass.getResource("/fixtures/lmu/lemans_2023/$fileName")) {
            "Missing fixture: $fileName"
        }.toURI(),
    )
}
