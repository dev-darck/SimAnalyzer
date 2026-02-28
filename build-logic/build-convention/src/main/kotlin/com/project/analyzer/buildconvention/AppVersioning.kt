package com.project.analyzer.buildconvention

import org.gradle.api.Project
import java.util.Locale
import java.util.Properties

internal data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val build: Int,
) {

    val base: String
        get() = "$major.$minor.$patch"

    fun displayName(channel: VersionChannel): String =
        if (channel == VersionChannel.Release) base else "$base-dev"
}

internal enum class VersionChannel {
    Dev,
    Release,
}

internal fun Project.configureRootVersioning() {
    val appVersion = rootProject.readAppVersion()
    val versionChannel = rootProject.detectVersionChannel()
    val displayVersion = appVersion.displayName(versionChannel)

    rootProject.version = displayVersion
    rootProject.extensions.extraProperties["appVersionBase"] = appVersion.base
    rootProject.extensions.extraProperties["appVersionDisplay"] = displayVersion
    rootProject.extensions.extraProperties["appVersionBuild"] = appVersion.build
    rootProject.extensions.extraProperties["appVersionChannel"] = versionChannel.name.lowercase(Locale.US)
}

private fun Project.readAppVersion(): AppVersion {
    val props = Properties().apply {
        rootProject.file("version.properties").inputStream().use(::load)
    }

    fun requireInt(key: String): Int =
        props.getProperty(key)?.trim()?.toIntOrNull()
            ?: error("Missing integer property `$key` in version.properties")

    return AppVersion(
        major = requireInt("version.major"),
        minor = requireInt("version.minor"),
        patch = requireInt("version.patch"),
        build = requireInt("version.build"),
    )
}

private fun Project.detectReleaseBuild(): Boolean {
    val explicitBuildType = providers.gradleProperty("buildType").orNull
    if (explicitBuildType != null) {
        return explicitBuildType.equals("release", ignoreCase = true)
    }

    val requestedTasks = gradle.startParameter.taskNames.joinToString(" ").lowercase(Locale.US)
    return requestedTasks.contains("release") || requestedTasks.contains("package")
}

private fun Project.detectVersionChannel(): VersionChannel {
    providers.gradleProperty("versionChannel").orNull?.let { raw ->
        return raw.toVersionChannel()
    }

    System.getenv("VERSION_CHANNEL")?.let { raw ->
        return raw.toVersionChannel()
    }

    return if (detectReleaseBuild()) VersionChannel.Release else VersionChannel.Dev
}

private fun String.toVersionChannel(): VersionChannel =
    when (trim().lowercase(Locale.US)) {
        "dev", "debug" -> VersionChannel.Dev
        "release" -> VersionChannel.Release
        else -> error("Unsupported version channel `$this`. Expected `dev` or `release`.")
    }
