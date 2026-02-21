package com.project.analyzer.utils

import java.io.File
import java.net.URI

internal object AppPaths {

    private const val APP_DATA = "APPDATA"
    private const val LOCAL_APP_DATA = "LOCALAPPDATA"
    private val userHome: String get() = System.getProperty("user.home")
    private val appRootDir: File by lazy { detectAppRootDirWindows() }

    private val portableBaseDir: File? by lazy {
        if (!BuildConfig.IS_PORTABLE) return@lazy null
        appRootDir.takeIf { it.isWritableDirectory() }
    }

    val dataDir: File by lazy {
        portableBaseDir?.let { base ->
            return@lazy File(base, "data").ensureExists()
        }

        val appData = System.getenv(APP_DATA) ?: "$userHome\\AppData\\Roaming"
        File(appData, BuildConfig.APP_NAME).ensureExists()
    }

    val preferencesDir: File by lazy {
        File(dataDir, "preferences").ensureExists()
    }

    val logsDir: File by lazy {
        File(dataDir, "logs").ensureExists()
    }

    val userDataDir: File by lazy {
        File(dataDir, "user").ensureExists()
    }

    val cacheDir: File by lazy {
        portableBaseDir?.let {
            return@lazy File(dataDir, "cache").ensureExists()
        }

        val local = System.getenv(LOCAL_APP_DATA) ?: "$userHome\\AppData\\Local"
        File(local, "${BuildConfig.APP_NAME}\\cache").ensureExists()
    }

    private fun detectAppRootDirWindows(): File {
        val codeSourceFile =
            runCatching {
                val uri: URI = AppPaths::class.java.protectionDomain.codeSource.location.toURI()
                File(uri)
            }.getOrNull()

        val dir =
            when {
                codeSourceFile == null -> File(System.getProperty("user.dir"))
                codeSourceFile.isFile -> codeSourceFile.parentFile ?: File(System.getProperty("user.dir"))
                else -> codeSourceFile
            }

        if (dir.name.equals("app", ignoreCase = true) && dir.parentFile != null) {
            return dir.parentFile
        }

        return dir
    }

    private fun File.isWritableDirectory(): Boolean {
        if (exists()) return isDirectory && canWrite()
        return mkdirs() && canWrite()
    }

    private fun File.ensureExists(): File = apply {
        if (!exists()) mkdirs()
    }
}
