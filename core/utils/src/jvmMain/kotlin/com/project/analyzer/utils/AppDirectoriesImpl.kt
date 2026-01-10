package com.project.analyzer.utils

import java.io.File

internal class AppDirectoriesImpl : AppDirectories {

    override val dataDir: File get() = AppPaths.dataDir
    override val preferencesDir: File get() = AppPaths.preferencesDir
    override val cacheDir: File get() = AppPaths.cacheDir
    override val logsDir: File get() = AppPaths.logsDir
    override val userDataDir: File get() = AppPaths.userDataDir
}
