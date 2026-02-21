package com.project.analyzer.buildconvention

import org.gradle.api.initialization.Settings

fun Settings.includeSubmodulesFrom(vararg roots: String) {
    val ignoredDirs = setOf("build", ".gradle", ".idea")
    val buildFileName = "build.gradle.kts"

    roots.forEach { root ->
        val start = rootDir.resolve(root).takeIf { it.isDirectory } ?: return@forEach

        start
            .walkTopDown()
            .maxDepth(3)
            .onEnter { dir ->
                if (dir == start) return@onEnter true
                val name = dir.name
                name !in ignoredDirs && !name.startsWith(".")
            }
            .filter { dir ->
                dir.isDirectory && dir.resolve(buildFileName).isFile
            }
            .forEach { dir ->
                val relative = dir.relativeTo(rootDir)
                val projectPath = ":" + relative.invariantSeparatorsPath.replace('/', ':')

                include(projectPath)
                project(projectPath).projectDir = dir
            }
    }
}
