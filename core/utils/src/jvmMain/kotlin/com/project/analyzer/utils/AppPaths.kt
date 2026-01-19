package com.project.analyzer.utils

import java.io.File

internal object AppPaths {

    private const val LOCAL_APP_DATA = "LOCALAPPDATA"
    private const val APP_DATA = "APPDATA"
    private const val XDG_DATA_HOME = "XDG_DATA_HOME"
    private const val XDG_CACHE_HOME = "XDG_CACHE_HOME"
    private const val LOCAL = "\\AppData\\Local"

    private val userHome: String get() = System.getProperty("user.home")
    private val isWindows: Boolean get() = System.getProperty("os.name").lowercase().contains("win")

    val dataDir: File by lazy {
        val path = if (isWindows) {
            val appData = System.getenv(APP_DATA) ?: "$userHome\\AppData\\Roaming"
            "$appData\\${BuildConfig.APP_NAME}"
        } else {
            val xdgData = System.getenv(XDG_DATA_HOME) ?: "$userHome/.local/share"
            "$xdgData/${BuildConfig.APP_NAME}"
        }
        File(path).ensureExists()
    }

    val logsDir: File by lazy {
        File(dataDir, "logs").ensureExists()
    }

    val preferencesDir: File by lazy {
        File(dataDir, "preferences").ensureExists()
    }

    val cacheDir: File by lazy {
        val path = if (isWindows) {
            val localAppData = System.getenv(LOCAL_APP_DATA) ?: "$userHome$LOCAL"
            "$localAppData\\${BuildConfig.APP_NAME}\\cache"
        } else {
            val xdgCache = System.getenv(XDG_CACHE_HOME) ?: "$userHome/.cache"
            "$xdgCache/${BuildConfig.APP_NAME}"
        }
        File(path).ensureExists()
    }

    val userDataDir: File by lazy {
        File(dataDir, "data").ensureExists()
    }

    private fun File.ensureExists(): File = apply {
        if (!exists()) mkdirs()
    }
}
