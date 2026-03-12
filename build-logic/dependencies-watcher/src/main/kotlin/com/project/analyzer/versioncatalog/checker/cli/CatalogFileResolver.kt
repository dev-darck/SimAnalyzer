package com.project.analyzer.versioncatalog.checker.cli

import com.project.analyzer.versioncatalog.checker.model.DependencyWatcherConstants
import java.io.File

internal object CatalogFileResolver {

    fun resolve(rawPath: String, startDir: File): File {
        val direct = File(rawPath)
        if (direct.isAbsolute) return direct.normalize()

        val candidates = buildList {
            add(File(startDir, rawPath))

            var current: File? = startDir
            while (current != null) {
                add(File(current, rawPath))
                if (File(current, "settings.gradle.kts").isFile) {
                    add(File(current, DependencyWatcherConstants.DEFAULT_TOML_PATH))
                }
                current = current.parentFile
            }
        }

        return candidates
            .firstOrNull(File::isFile)
            ?.normalize()
            ?: direct.normalize()
    }
}
