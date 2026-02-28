package com.project.analyzer.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

public class AppDirectoriesImpl internal constructor(
    override val dataDir: File,
) : AppDirectories {

    override val preferencesDir: File = File(dataDir, "preferences")
    override val logsDir: File = File(dataDir, "logs")
    override val userDataDir: File = File(dataDir, "user")
    override val cacheDir: File = File(dataDir, "cache")
    override val runtimeDir: File = File(dataDir, "runtime")
    override val lockFile: File = File(runtimeDir, "app.lock")

    internal fun ensureStructure(): AppDirectoriesImpl = apply {
        dataDir.ensureExists()
        preferencesDir.ensureExists()
        logsDir.ensureExists()
        userDataDir.ensureExists()
        cacheDir.ensureExists()
        runtimeDir.ensureExists()
    }
}

public suspend fun resolveAppDirectories(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
): AppDirectoriesImpl = withContext(dispatcher) {
    val codeSourceRootDir = detectCodeSourceRootDirWindows()
    val installationRootDir = detectInstallationRootDirWindows(codeSourceRootDir)
    val portableBaseDir = if (BuildConfig.IS_PORTABLE) {
        (installationRootDir ?: codeSourceRootDir).takeIf { it.isWritableDirectory() }
    } else {
        null
    }

    val dataDir = portableBaseDir
        ?.let { base -> File(base, "data") }
        ?: resolveInstalledDataDir(installationRootDir)

    AppDirectoriesImpl(dataDir).ensureStructure()
}

private const val APP_DATA = "APPDATA"
private const val INSTALL_SETTINGS_FILE_NAME = "simanalyzer-installation.ini"
private const val INSTALL_SETTINGS_SECTION = "Paths"
private const val INSTALL_SETTINGS_KEY = "DataRoot"
private const val MAX_INSTALL_ROOT_DEPTH = 6

private fun detectCodeSourceRootDirWindows(): File {
    val codeSourceFile = runCatching {
        val uri: URI = AppDirectoriesImpl::class.java.protectionDomain.codeSource.location.toURI()
        File(uri)
    }.getOrNull()

    val dir = when {
        codeSourceFile == null -> File(System.getProperty("user.dir"))
        codeSourceFile.isFile -> codeSourceFile.parentFile ?: File(System.getProperty("user.dir"))
        else -> codeSourceFile
    }

    if (dir.name.equals("app", ignoreCase = true) && dir.parentFile != null) {
        return dir.parentFile
    }

    return dir
}

private fun detectInstallationRootDirWindows(codeSourceRootDir: File): File? {
    val candidates = linkedSetOf<File>()
    collectInstallRootCandidates(System.getProperty("jpackage.app-path"), candidates)
    collectInstallRootCandidates(codeSourceRootDir.absolutePath, candidates)

    return candidates.firstOrNull { candidate ->
        File(candidate, INSTALL_SETTINGS_FILE_NAME).isFile
    } ?: candidates.firstOrNull(::looksLikePackagedAppRoot)
}

private fun resolveInstalledDataDir(installationRootDir: File?): File {
    val configuredPath = readConfiguredDataRoot(installationRootDir)
    return File(configuredPath ?: defaultInstalledDataRootPath(installationRootDir)).ensureExists()
}

private fun defaultInstalledDataRootPath(installationRootDir: File?): String {
    installationRootDir?.let { installRoot ->
        return File(installRoot, "data").absolutePath
    }

    val userHome = System.getProperty("user.home")
    val appData = System.getenv(APP_DATA) ?: "$userHome\\AppData\\Roaming"
    return File(appData, BuildConfig.APP_NAME).absolutePath
}

private fun readConfiguredDataRoot(installationRootDir: File?): String? {
    return readConfiguredDataRootFromInstallSettings(installationRootDir)
        ?: readConfiguredDataRootFromRegistry()
}

private fun readConfiguredDataRootFromInstallSettings(installationRootDir: File?): String? {
    val installSettingsFile = installationSettingsFile(installationRootDir) ?: return null

    val lines = installSettingsFile.readLines(StandardCharsets.UTF_8)
    val hasSections = lines.any { line ->
        val trimmed = line.trim()
        trimmed.startsWith("[") && trimmed.endsWith("]")
    }

    var insidePathsSection = !hasSections
    lines.forEach { rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) return@forEach

        if (line.startsWith("[") && line.endsWith("]")) {
            insidePathsSection = line.removePrefix("[").removeSuffix("]") == INSTALL_SETTINGS_SECTION
            return@forEach
        }

        if (!insidePathsSection) return@forEach

        val keyValueSeparatorIndex = line.indexOf('=')
        if (keyValueSeparatorIndex <= 0) return@forEach

        val key = line.substring(0, keyValueSeparatorIndex).trim()
        if (key != INSTALL_SETTINGS_KEY) return@forEach

        val value = line.substring(keyValueSeparatorIndex + 1).trim().trim('"')
        if (value.isNotBlank()) {
            return value
        }
    }

    return null
}

private fun readConfiguredDataRootFromRegistry(): String? {
    if (!System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) return null

    return readConfiguredRegistryValue("HKCU\\Software\\SimAnalyzer")
        ?: readConfiguredRegistryValue("HKLM\\Software\\SimAnalyzer")
}

private fun readConfiguredRegistryValue(keyPath: String): String? {
    return runCatching {
        val process = ProcessBuilder("reg", "query", keyPath, "/v", INSTALL_SETTINGS_KEY)
            .redirectErrorStream(true)
            .start()

        if (!process.waitFor(2, TimeUnit.SECONDS) || process.exitValue() != 0) {
            process.destroyForcibly()
            return null
        }

        process.inputStream.bufferedReader(StandardCharsets.UTF_8).useLines { lines ->
            lines.map(String::trim)
                .firstOrNull { line -> line.startsWith("$INSTALL_SETTINGS_KEY ") }
                ?.split(Regex("\\s+"), limit = 3)
                ?.getOrNull(2)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }
    }.getOrNull()
}

private fun installationSettingsFile(installationRootDir: File?): File? {
    installationRootDir?.let { installRoot ->
        val rootFile = File(installRoot, INSTALL_SETTINGS_FILE_NAME)
        if (rootFile.isFile) return rootFile
    }

    return null
}

private fun collectInstallRootCandidates(rawPath: String?, out: MutableSet<File>) {
    if (rawPath.isNullOrBlank()) return

    var current = File(rawPath)
    if (current.isFile) {
        current = current.parentFile ?: return
    }

    repeat(MAX_INSTALL_ROOT_DEPTH) {
        out += current
        current = current.parentFile ?: return
    }
}

private fun looksLikePackagedAppRoot(dir: File): Boolean {
    if (!dir.isDirectory) return false

    val childNames = dir.list()?.map { it.lowercase() }?.toSet() ?: emptySet()
    return "app" in childNames || "runtime" in childNames || "lib" in childNames
}

private fun File.isWritableDirectory(): Boolean {
    if (exists()) return isDirectory && canWrite()
    return mkdirs() && canWrite()
}

private fun File.ensureExists(): File = apply {
    if (!exists()) mkdirs()
}
