#!/usr/bin/env kotlin

import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.Properties

data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val build: Int,
) {

    val base: String
        get() = "$major.$minor.$patch"

    fun displayName(channel: VersionChannel): String =
        if (channel == VersionChannel.RELEASE) base else "$base-dev"

    fun tag(channel: VersionChannel): String =
        if (channel == VersionChannel.RELEASE) {
            "v$base"
        } else {
            "v$base-dev.$build"
        }

    fun artifactVersion(channel: VersionChannel): String =
        if (channel == VersionChannel.RELEASE) {
            base
        } else {
            "$base-dev.build.$build"
        }

    fun bump(part: String): AppVersion =
        when (part) {
            "build" -> copy(build = build + 1)
            "patch" -> copy(patch = patch + 1)
            "minor" -> copy(minor = minor + 1, patch = 0)
            "major" -> copy(major = major + 1, minor = 0, patch = 0)
            else -> error("Unsupported version part `$part`")
        }
}

enum class VersionChannel {
    DEV,
    RELEASE,
}

val command = args.getOrNull(0) ?: error("Missing command. Expected: bump|describe")

when (command) {
    "bump" -> runBumpCommand(args)
    "describe" -> runDescribeCommand(args)
    else -> error("Unsupported command `$command`.")
}

fun runBumpCommand(args: Array<String>) {
    val part = args.getOrNull(1) ?: error("Missing version part. Expected: major|minor|patch|build")
    val filePath = args.getOrNull(2) ?: "version.properties"
    val file = File(filePath)

    val before = readAppVersion(file)
    val after = before.bump(part)
    writeAppVersion(file, after)
    writeBumpOutputs(before, after, part)

    println("Bumped $part: ${before.base} (build ${before.build}) -> ${after.base} (build ${after.build})")
}

fun runDescribeCommand(args: Array<String>) {
    val channel = args.getOrNull(1)?.toVersionChannel()
        ?: error("Missing version channel. Expected: dev|release")
    val filePath = args.getOrNull(2) ?: "version.properties"
    val file = File(filePath)
    val version = readAppVersion(file)

    writeDescribeOutputs(version, channel)
    println("Describe ${version.base} build ${version.build} as ${channel.name.lowercase(Locale.US)}")
}

fun readAppVersion(file: File): AppVersion {
    require(file.isFile) { "Version file not found: ${file.absolutePath}" }

    val props = Properties().apply {
        file.inputStream().use(::load)
    }

    fun requireInt(key: String): Int =
        props.getProperty(key)?.trim()?.toIntOrNull()
            ?: error("Missing integer property `$key` in ${file.name}")

    return AppVersion(
        major = requireInt("version.major"),
        minor = requireInt("version.minor"),
        patch = requireInt("version.patch"),
        build = requireInt("version.build"),
    )
}

fun writeAppVersion(file: File, version: AppVersion) {
    val content = buildString {
        appendLine("version.major=${version.major}")
        appendLine("version.minor=${version.minor}")
        appendLine("version.patch=${version.patch}")
        appendLine("version.build=${version.build}")
    }

    file.writeText(content, StandardCharsets.UTF_8)
}

fun writeBumpOutputs(before: AppVersion, after: AppVersion, part: String) {
    val outputPath = System.getenv("GITHUB_OUTPUT") ?: return
    File(outputPath).appendText(
        buildString {
            appendLine("part=$part")
            appendLine("previous_base=${before.base}")
            appendLine("previous_build=${before.build}")
            appendLine("current_base=${after.base}")
            appendLine("current_build=${after.build}")
            appendLine("current_dev=${after.displayName(VersionChannel.DEV)}")
            appendLine("current_release=${after.displayName(VersionChannel.RELEASE)}")
            appendLine("current_dev_tag=${after.tag(VersionChannel.DEV)}")
            appendLine("current_release_tag=${after.tag(VersionChannel.RELEASE)}")
            appendLine("current_dev_artifact=${after.artifactVersion(VersionChannel.DEV)}")
            appendLine("current_release_artifact=${after.artifactVersion(VersionChannel.RELEASE)}")
        },
        StandardCharsets.UTF_8,
    )
}

fun writeDescribeOutputs(version: AppVersion, channel: VersionChannel) {
    val outputPath = System.getenv("GITHUB_OUTPUT") ?: return
    File(outputPath).appendText(
        buildString {
            appendLine("channel=${channel.name.lowercase(Locale.US)}")
            appendLine("base=${version.base}")
            appendLine("build=${version.build}")
            appendLine("display=${version.displayName(channel)}")
            appendLine("tag=${version.tag(channel)}")
            appendLine("artifact_version=${version.artifactVersion(channel)}")
            appendLine("prerelease=${(channel == VersionChannel.DEV)}")
        },
        StandardCharsets.UTF_8,
    )
}

fun String.toVersionChannel(): VersionChannel =
    when (trim().lowercase(Locale.US)) {
        "dev", "debug" -> VersionChannel.DEV
        "release" -> VersionChannel.RELEASE
        else -> error("Unsupported version channel `$this`. Expected `dev` or `release`.")
    }
