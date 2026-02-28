package com.project.analyzer.utils

import java.io.File

public interface AppDirectories {

    public val dataDir: File
    public val preferencesDir: File
    public val cacheDir: File
    public val logsDir: File
    public val userDataDir: File
    public val runtimeDir: File
    public val lockFile: File
}
