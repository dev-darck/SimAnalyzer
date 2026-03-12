package com.project.analyzer.versioncatalog.checker.model

internal object DependencyWatcherConstants {

    const val DEFAULT_TOML_PATH = "gradle/libs.versions.toml"
    const val MAVEN_CENTRAL = "https://repo.maven.apache.org/maven2/"
    const val GOOGLE_MAVEN = "https://dl.google.com/dl/android/maven2/"
    const val GRADLE_PLUGIN_PORTAL = "https://plugins.gradle.org/m2/"
    const val JETBRAINS_COMPOSE_DEV = "https://maven.pkg.jetbrains.space/public/p/compose/dev/"
    const val CONNECTION_TIMEOUT_SECONDS = 10L
    const val REQUEST_TIMEOUT_SECONDS = 20L

    val versionTagRegex = Regex("<version>\\s*([^<]+?)\\s*</version>")
}
