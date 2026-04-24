package com.project.analyzer.telemetry.lmu.impl.install

import java.nio.file.Files
import java.nio.file.Path

internal data class LmuInstalledTrackManifest(
    val componentName: String,
    val version: String,
    val masFiles: List<String>,
)

internal class LmuInstalledTrackManifestParser {

    fun parse(path: Path): LmuInstalledTrackManifest? = parseLines(Files.readAllLines(path))

    internal fun parseLines(lines: List<String>): LmuInstalledTrackManifest? {
        var componentName: String? = null
        var version: String? = null
        val masFiles = mutableListOf<String>()

        lines.forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("Name=", ignoreCase = true) -> {
                    componentName = line.substringAfter('=').trim().takeIf(String::isNotBlank)
                }

                line.startsWith("Version=", ignoreCase = true) -> {
                    version = line.substringAfter('=').trim().takeIf(String::isNotBlank)
                }

                line.startsWith("MASFile=", ignoreCase = true) -> {
                    val fileName = line.substringAfter('=')
                        .trim()
                        .substringBefore(' ')
                        .trim()
                        .takeIf(String::isNotBlank)
                    if (fileName != null) {
                        masFiles += fileName
                    }
                }
            }
        }

        val resolvedName = componentName ?: return null
        val resolvedVersion = version ?: return null
        return LmuInstalledTrackManifest(
            componentName = resolvedName,
            version = resolvedVersion,
            masFiles = masFiles,
        )
    }
}
