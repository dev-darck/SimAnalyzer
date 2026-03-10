package com.project.analyzer.ac.telemetry.impl.trackmap.evo

import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.Inject
import java.nio.file.Files
import java.nio.file.Path

@Inject
internal class AcEvoContentPackageLocator {

    private val logger = logger()

    @Volatile
    private var cached: Path? = null

    fun locate(): Path? {
        cached?.takeIf(Files::isRegularFile)?.let { return it }

        readPackageOverride()?.let { packagePath ->
            cached = packagePath
            return packagePath
        }

        val steamRoots = buildSteamRoots()
        val candidatePackages = linkedSetOf<Path>()
        steamRoots.forEach { steamRoot ->
            candidatePackages.add(
                steamRoot.resolve(STEAM_APPS_DIR).resolve(GAME_INSTALL_DIR).resolve(CONTENT_PACKAGE_FILE_NAME),
            )
            parseLibraryFolders(
                steamRoot.resolve(STEAM_APPS_DIR).resolve(LIBRARY_FOLDERS_FILE_NAME),
            ).forEach { libraryRoot ->
                candidatePackages.add(
                    libraryRoot.resolve(STEAM_APPS_DIR).resolve(GAME_INSTALL_DIR).resolve(CONTENT_PACKAGE_FILE_NAME),
                )
            }
        }

        val resolved = candidatePackages
            .asSequence()
            .filter(Files::isRegularFile)
            .maxByOrNull { path ->
                runCatching { Files.getLastModifiedTime(path).toMillis() }.getOrDefault(0L)
            }

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

    private fun buildSteamRoots(): Set<Path> {
        val roots = linkedSetOf<Path>()

        normalizePath(System.getProperty(STEAM_ROOT_PROPERTY))?.let(roots::add)
        normalizePath(System.getenv(STEAM_ROOT_ENV))?.let(roots::add)

        val programFilesX86 = System.getenv("ProgramFiles(x86)")
        val programFiles = System.getenv("ProgramFiles")
        val userHome = System.getProperty("user.home")

        listOf(
            "D:/steam",
            "D:/Steam",
            "C:/steam",
            "C:/Steam",
            programFilesX86?.let { "$it/Steam" },
            programFiles?.let { "$it/Steam" },
            userHome?.let { "$it/AppData/Local/Steam" },
        ).forEach { raw ->
            normalizePath(raw)?.let(roots::add)
        }

        return roots.filter(Files::isDirectory).toCollection(linkedSetOf())
    }

    private fun parseLibraryFolders(vdfPath: Path): Set<Path> {
        if (!Files.isRegularFile(vdfPath)) return emptySet()
        val pathRegex = Regex("""\"path\"\s+\"([^\"]+)\"""")
        return runCatching {
            Files.readAllLines(vdfPath)
                .asSequence()
                .mapNotNull { line ->
                    pathRegex.find(line)?.groupValues?.getOrNull(1)
                }
                .mapNotNull(::normalizePath)
                .filter(Files::isDirectory)
                .toCollection(linkedSetOf())
        }.getOrElse {
            logger.warn(it) { "Failed to parse Steam library folders: ${vdfPath.toAbsolutePath()}" }
            emptySet()
        }
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
        const val GAME_INSTALL_DIR = "common/Assetto Corsa EVO"
        const val STEAM_APPS_DIR = "steamapps"
        const val LIBRARY_FOLDERS_FILE_NAME = "libraryfolders.vdf"
    }
}
