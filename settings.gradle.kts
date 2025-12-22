rootProject.name = "SimAnalyzer"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":composeApp")
includeSubmodulesFrom("core")

fun Settings.includeSubmodulesFrom(
    vararg roots: String,
) {
    val ignoredDirs = setOf("build", ".gradle", ".idea")

    roots.forEach { root ->
        val start = rootDir.resolve(root)
        if (!start.isDirectory) return@forEach

        start
            .walkTopDown()
            .maxDepth(3)
            .onEnter { dir ->
                if (dir != start &&
                    (dir.name in ignoredDirs || dir.name.startsWith("."))
                ) {
                    return@onEnter false
                }
                true
            }
            .filter { dir ->
                dir.isDirectory && dir.resolve("build.gradle.kts").isFile
            }
            .forEach { dir ->
                val relative = dir.relativeTo(rootDir)
                val projectPath = ":" + relative
                    .invariantSeparatorsPath
                    .split('/')
                    .joinToString(":")

                include(projectPath)
                project(projectPath).projectDir = dir
            }
    }
}
