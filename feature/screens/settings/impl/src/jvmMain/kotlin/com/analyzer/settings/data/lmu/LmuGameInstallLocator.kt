package com.analyzer.settings.data.lmu

import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.utils.logger.logger
import com.project.analyzer.utils.steam.SteamInstallResolver
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.nio.file.Files
import java.nio.file.Path

@Inject
@SingleIn(ScreenScope::class)
internal class LmuGameInstallLocator(private val steamInstallResolver: SteamInstallResolver) {

    private val logger = logger()

    @Volatile
    private var cachedInstallDir: Path? = null

    fun locate(): Path? {
        cachedInstallDir?.takeIf(Files::isDirectory)?.let { return it }

        readInstallOverride()?.let { installDir ->
            cachedInstallDir = installDir
            return installDir
        }

        val resolved = steamInstallResolver.findInstallDir(
            gameInstallDirName = GAME_INSTALL_DIR,
            steamRootProperty = STEAM_ROOT_PROPERTY,
            steamRootEnv = STEAM_ROOT_ENV,
        )
        if (resolved != null) {
            cachedInstallDir = resolved
            logger.info { "LMU install resolved: ${resolved.toAbsolutePath()}" }
        }
        return resolved
    }

    private fun readInstallOverride(): Path? {
        val raw = System.getProperty(INSTALL_DIR_PROPERTY)
            ?: System.getenv(INSTALL_DIR_ENV)
            ?: return null
        return normalizePath(raw)?.takeIf(Files::isDirectory)
    }

    private fun normalizePath(raw: String?): Path? {
        val value = raw?.trim()?.trim('"')?.takeIf(String::isNotBlank) ?: return null
        return runCatching {
            Path.of(value.replace("\\\\", "\\")).normalize()
        }.getOrNull()
    }

    private companion object {

        const val INSTALL_DIR_PROPERTY = "lmu.installDir"
        const val INSTALL_DIR_ENV = "LMU_INSTALL_DIR"
        const val STEAM_ROOT_PROPERTY = "lmu.steamPath"
        const val STEAM_ROOT_ENV = "LMU_STEAM_PATH"

        const val GAME_INSTALL_DIR = "Le Mans Ultimate"
    }
}
