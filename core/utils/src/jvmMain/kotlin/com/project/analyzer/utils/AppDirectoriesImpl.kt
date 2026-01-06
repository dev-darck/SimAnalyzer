package com.project.analyzer.utils

import com.project.analyzer.api.di.AppEnvironment
import java.io.File

internal class AppDirectoriesImpl(
    appEnvironment: AppEnvironment
) : AppDirectories {

    private val userHome = appEnvironment.userHome

    override val dataDir: File by lazy {
        val path = when {
            appEnvironment.isWindows -> {
                val appData = System.getenv(APP_DATA) ?: "$userHome\\AppData\\Roaming"
                "$appData\\$APP_NAME"
            }

            else -> {
                val xdgData = System.getenv("XDG_DATA_HOME") ?: "$userHome/.local/share"
                "$xdgData/$APP_NAME"
            }
        }
        File(path).ensureExists()
    }

    override val preferencesDir: File by lazy {
        File(dataDir, "preferences").ensureExists()
    }

    override val cacheDir: File by lazy {
        val path = when {
            appEnvironment.isWindows -> {
                val localAppData = System.getenv(LOCAL_APP_DATA) ?: "$userHome$LOCAL"
                "$localAppData\\$APP_NAME\\cache"
            }

            else -> {
                val xdgCache = System.getenv(XDG_CACHE_HOME) ?: "$userHome/.cache"
                "$xdgCache/$APP_NAME"
            }
        }
        File(path).ensureExists()
    }

    override val logsDir: File by lazy {
        File(dataDir, "logs").ensureExists()
    }

    override val userDataDir: File by lazy {
        File(dataDir, "data").ensureExists()
    }

    private fun File.ensureExists(): File = apply {
        if (!exists()) mkdirs()
    }

    private companion object {

        const val LOCAL_APP_DATA = "LOCALAPPDATA"
        const val APP_DATA = "APPDATA"
        const val XDG_CACHE_HOME = "XDG_CACHE_HOME"
        const val LOCAL = "\\AppData\\Local"
        const val APP_NAME = "SimAnalyzer"
    }
}
