package com.project.analyzer.telemetry.lmu.impl.install

import java.nio.file.Files
import java.nio.file.Path

internal data class LmuSceneLoadTrace(
    val componentName: String,
    val version: String,
    val layoutMasFileName: String,
    val runtimeTrackName: String,
)

internal class LmuSceneLoadTraceParser {

    fun parse(path: Path): LmuSceneLoadTrace? = parseLines(Files.readAllLines(path))

    internal fun parseLines(lines: List<String>): LmuSceneLoadTrace? {
        var componentName: String? = null
        var version: String? = null
        var layoutMasFileName: String? = null
        var runtimeTrackName: String? = null

        lines.forEach { rawLine ->
            val line = rawLine.trim()
            when {
                componentName == null && line.contains("Component:", ignoreCase = true) -> {
                    componentName = line.substringAfter("Component:").trim().takeIf(String::isNotBlank)
                }

                version == null && line.contains("vidman.cpp") && line.contains("Version:", ignoreCase = true) -> {
                    version = line.substringAfter("Version:").trim().takeIf(String::isNotBlank)
                }

                layoutMasFileName == null && line.contains("Layout:", ignoreCase = true) -> {
                    layoutMasFileName = line.substringAfter("Layout:")
                        .trim()
                        .substringAfterLast('\\')
                        .substringAfterLast('/')
                        .takeIf(String::isNotBlank)
                }

                runtimeTrackName == null && line.contains("Appling Config for Track:", ignoreCase = true) -> {
                    runtimeTrackName = line.substringAfter("Appling Config for Track:")
                        .trim()
                        .takeIf(String::isNotBlank)
                }
            }
        }

        return LmuSceneLoadTrace(
            componentName = componentName ?: return null,
            version = version ?: return null,
            layoutMasFileName = layoutMasFileName ?: return null,
            runtimeTrackName = runtimeTrackName ?: return null,
        )
    }
}
