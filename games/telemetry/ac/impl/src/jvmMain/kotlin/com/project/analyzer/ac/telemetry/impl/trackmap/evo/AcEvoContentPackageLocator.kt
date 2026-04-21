package com.project.analyzer.ac.telemetry.impl.trackmap.evo

import com.project.analyzer.utils.logger.logger
import com.project.analyzer.utils.steam.SteamInstallResolver
import dev.zacsweers.metro.Inject
import java.nio.file.Files
import java.nio.file.Path

@Inject
internal class AcEvoContentPackageLocator(private val steamInstallResolver: SteamInstallResolver) {

    private val logger = logger()

    @Volatile
    private var cached: Path? = null

    fun locate(): Path? {
        cached?.takeIf(Files::isRegularFile)?.let { return it }

        readPackageOverride()?.let { packagePath ->
            cached = packagePath
            return packagePath
        }

        val resolved = steamInstallResolver.findInstallDir(
            gameInstallDirName = GAME_INSTALL_DIR,
            steamRootProperty = STEAM_ROOT_PROPERTY,
            steamRootEnv = STEAM_ROOT_ENV,
        )?.resolve(CONTENT_PACKAGE_FILE_NAME)
            ?.takeIf(Files::isRegularFile)

        if (resolved != null) {
            cached = resolved
            logger.debug { "AcEvoContentPackageLocator resolved package: ${resolved.toAbsolutePath()}" }
        }
        return resolved
    }

    private fun readPackageOverride(): Path? {
        val raw = System.getProperty(CONTENT_PACKAGE_PROPERTY)
            ?: System.getenv(CONTENT_PACKAGE_ENV)
            ?: return null
        return normalizePath(raw)?.takeIf(Files::isRegularFile)
    }

    private fun normalizePath(raw: String?): Path? {
        val value = raw?.trim()?.trim('"')?.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            Path.of(value.replace("\\\\", "\\")).normalize()
        }.getOrNull()
    }

    private companion object {

        const val CONTENT_PACKAGE_PROPERTY = "acevo.contentPackagePath"
        const val CONTENT_PACKAGE_ENV = "ACEVO_CONTENT_PACKAGE_PATH"
        const val STEAM_ROOT_PROPERTY = "acevo.steamPath"
        const val STEAM_ROOT_ENV = "ACEVO_STEAM_PATH"

        const val CONTENT_PACKAGE_FILE_NAME = "content.kspkg"
        const val GAME_INSTALL_DIR = "Assetto Corsa EVO"
    }
}
