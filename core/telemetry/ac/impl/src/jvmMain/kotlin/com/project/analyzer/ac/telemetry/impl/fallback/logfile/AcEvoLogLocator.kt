package com.project.analyzer.ac.telemetry.impl.fallback.logfile

import dev.zacsweers.metro.Inject
import java.io.File

@Inject
class AcEvoLogLocator {

    private var file: File? = null

    fun locateLogFile(): File? {
        if (file != null) return file

        // -Dacevo.logPath="C:\...\log.txt"
        val override = System.getProperty("acevo.logPath")?.trim().orEmpty()
        if (override.isNotEmpty()) {
            val f = File(override)
            if (f.isFile) return f
        }

        val home = System.getProperty("user.home") ?: return null
        val userProfile = System.getenv("USERPROFILE") ?: home

        val candidates = listOf(
            File(home, "Documents/ACE/log.txt"),
            File(userProfile, "Documents/ACE/log.txt"),
        )

        file = candidates.firstOrNull { it.isFile }
        return file
    }

    fun clear() {
        file = null
    }
}
