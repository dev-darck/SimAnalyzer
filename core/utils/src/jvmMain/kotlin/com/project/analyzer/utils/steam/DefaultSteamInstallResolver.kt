package com.project.analyzer.utils.steam

import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<SteamInstallResolver>())
public class DefaultSteamInstallResolver : SteamInstallResolver {

    private val logger = logger()

    override fun findInstallDir(gameInstallDirName: String, steamRootProperty: String?, steamRootEnv: String?): Path? {
        val normalizedGameDir = gameInstallDirName.trim().replace('\\', '/').trim('/')
        if (normalizedGameDir.isBlank()) return null

        return findLibraries(
            steamRootProperty = steamRootProperty,
            steamRootEnv = steamRootEnv,
        ).asSequence()
            .map { libraryRoot ->
                libraryRoot.resolve(STEAM_APPS_DIR).resolve(COMMON_DIR).resolve(normalizedGameDir)
            }
            .firstOrNull(Files::isDirectory)
            ?.toAbsolutePath()
            ?.normalize()
    }

    override fun findLibraries(steamRootProperty: String?, steamRootEnv: String?): Set<Path> {
        val steamRoots = buildSteamRoots(steamRootProperty = steamRootProperty, steamRootEnv = steamRootEnv)
        val libraries = linkedSetOf<Path>()

        steamRoots.forEach { steamRoot ->
            libraries.add(steamRoot)
            libraries.addAll(parseLibraryFolders(steamRoot.resolve(STEAM_APPS_DIR).resolve(LIBRARY_FOLDERS_FILE_NAME)))
        }

        return libraries.filter(Files::isDirectory).toCollection(linkedSetOf())
    }

    private fun buildSteamRoots(steamRootProperty: String?, steamRootEnv: String?): Set<Path> {
        val roots = linkedSetOf<Path>()

        steamRootProperty?.let(System::getProperty)?.let(::normalizePath)?.let(roots::add)
        steamRootEnv?.let(System::getenv)?.let(::normalizePath)?.let(roots::add)
        readSteamRegistryPath()?.let(roots::add)

        val programFilesX86 = System.getenv("ProgramFiles(x86)")
        val programFiles = System.getenv("ProgramFiles")
        val userHome = System.getProperty("user.home")

        listOfNotNull(
            "D:/steam",
            "D:/Steam",
            "C:/steam",
            "C:/Steam",
            programFilesX86?.let { "$it/Steam" },
            programFiles?.let { "$it/Steam" },
            userHome?.let { "$it/AppData/Local/Steam" },
        ).mapNotNull(::normalizePath)
            .filterTo(roots, Files::isDirectory)

        return roots
    }

    private fun parseLibraryFolders(vdfPath: Path): Set<Path> {
        if (!Files.isRegularFile(vdfPath)) return emptySet()
        return runCatching {
            Files.readAllLines(vdfPath)
                .asSequence()
                .mapNotNull { line -> LIBRARY_PATH_REGEX.find(line)?.groupValues?.getOrNull(1) }
                .mapNotNull(::normalizePath)
                .filter(Files::isDirectory)
                .toCollection(linkedSetOf())
        }.getOrElse { error ->
            logger.warn(error) { "Failed to parse Steam library folders: ${vdfPath.toAbsolutePath()}" }
            emptySet()
        }
    }

    private fun readSteamRegistryPath(): Path? {
        val registryKeys = listOf(
            "HKCU\\Software\\Valve\\Steam" to "SteamPath",
            "HKCU\\Software\\Valve\\Steam" to "InstallPath",
            "HKLM\\Software\\Valve\\Steam" to "InstallPath",
        )
        return registryKeys.asSequence()
            .mapNotNull { (keyPath, valueName) -> readRegistryValue(keyPath, valueName) }
            .mapNotNull(::normalizePath)
            .firstOrNull(Files::isDirectory)
    }

    private fun readRegistryValue(keyPath: String, valueName: String): String? {
        if (!System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) return null

        return runCatching {
            val process = ProcessBuilder("reg", "query", keyPath, "/v", valueName)
                .redirectErrorStream(true)
                .start()

            if (!process.waitFor(REGISTRY_QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS) || process.exitValue() != 0) {
                process.destroyForcibly()
                return null
            }

            process.inputStream.bufferedReader(StandardCharsets.UTF_8).useLines { lines ->
                lines.map(String::trim)
                    .firstOrNull { line -> line.startsWith("$valueName ") }
                    ?.split(Regex("\\s+"), limit = 3)
                    ?.getOrNull(2)
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
            }
        }.getOrNull()
    }

    private fun normalizePath(raw: String?): Path? {
        val value = raw?.trim()?.trim('"')?.takeIf(String::isNotBlank) ?: return null
        return runCatching {
            Path.of(value.replace("\\\\", "\\")).normalize()
        }.getOrNull()
    }

    private companion object {
        const val STEAM_APPS_DIR = "steamapps"
        const val COMMON_DIR = "common"
        const val LIBRARY_FOLDERS_FILE_NAME = "libraryfolders.vdf"
        const val REGISTRY_QUERY_TIMEOUT_SECONDS = 2L

        val LIBRARY_PATH_REGEX: Regex = Regex("\"path\"\\s+\"([^\"]+)\"")
    }
}
