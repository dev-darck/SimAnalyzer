package com.project.analyzer.ac.telemetry.impl.fallback.logfile

import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.Inject
import java.io.File
import kotlin.math.min

@Inject
class AcEvoLogLocator {

    @Volatile
    private var cached: File? = null

    fun locateLogFile(): File? {
        cached?.let {
            if (it.isFile) return it else cached = null
        }

        readOverridePath()?.let { f ->
            cached = f
            return f
        }

        val candidates = buildCandidates()
        candidates.firstOrNull { it.isFile }?.let {
            cached = it
            return it
        }

        val found = walkForLogTxt(candidates.mapNotNull { it.parentFile }.distinct())
        if (found != null) cached = found

        logger.info { "AcEvoLogLocator Found log file: ${found?.absolutePath}" }
        return found
    }

    fun clear() {
        cached = null
    }

    private fun readOverridePath(): File? {
        val prop = System.getProperty("acevo.logPath")?.trim().orEmpty()
        if (prop.isNotEmpty()) {
            val f = File(prop)
            if (f.isFile) return f
        }

        val env = System.getenv("ACEVO_LOG_PATH")?.trim().orEmpty()
        if (env.isNotEmpty()) {
            val f = File(env)
            if (f.isFile) return f
        }

        return null
    }

    private fun buildCandidates(): List<File> {
        val home = System.getProperty("user.home")
        val userProfile = System.getenv("USERPROFILE") ?: home
        val oneDrive = System.getenv("OneDrive")
            ?: System.getenv("OneDriveConsumer")
            ?: System.getenv("OneDriveCommercial")

        val docsDirs = linkedSetOf<File>()

        fun addDocs(base: String?) {
            if (base.isNullOrBlank()) return
            docsDirs += File(base, "Documents")
        }

        fun addMyDocs(base: String?) {
            if (base.isNullOrBlank()) return
            docsDirs += File(base, "My Documents")
        }

        addDocs(home)
        addDocs(userProfile)
        addDocs(oneDrive)

        addMyDocs(userProfile)
        addMyDocs(oneDrive)

        val homeDrive = System.getenv("HOMEDRIVE")
        val homePath = System.getenv("HOMEPATH")
        if (!homeDrive.isNullOrBlank() && !homePath.isNullOrBlank()) {
            docsDirs += File(homeDrive + homePath, "Documents")
        }

        val result = ArrayList<File>(docsDirs.size * 6)

        for (docs in docsDirs) {
            result += File(docs, "ACE/log.txt")

            result += File(docs, "Assetto Corsa Evo/log.txt")
            result += File(docs, "Assetto Corsa EVO/log.txt")
            result += File(docs, "Assetto Corsa Evo/logs/log.txt")
            result += File(docs, "Assetto Corsa EVO/logs/log.txt")
        }

        val linuxHome = home?.takeIf { it.isNotBlank() }
        if (linuxHome != null) {
            result += File(linuxHome, ".local/share/ACE/log.txt")
            result += File(linuxHome, ".config/ACE/log.txt")

            val compat = File(linuxHome, ".steam/steam/steamapps/compatdata")
            if (compat.isDirectory) {
                val dirs = compat.listFiles { f -> f.isDirectory }?.sortedByDescending { it.lastModified() }.orEmpty()
                val maxDirs = min(30, dirs.size)
                for (i in 0 until maxDirs) {
                    val d = dirs[i]
                    result += File(d, "pfx/drive_c/users/steamuser/Documents/ACE/log.txt")
                }
            }
        }

        return result.distinct()
    }

    private fun walkForLogTxt(startDirs: List<File>): File? {
        val allowedParents = setOf(
            "ACE",
            "Assetto Corsa Evo",
            "Assetto Corsa EVO",
            "logs",
        )

        for (dir in startDirs) {
            if (!dir.isDirectory) continue
            dir.walkTopDown()
                .maxDepth(4)
                .onEnter { it.isDirectory && !it.name.startsWith(".") }
                .firstOrNull {
                    it.isFile &&
                        it.name.equals("log.txt", ignoreCase = true) &&
                        (
                            it.parentFile?.name?.let { p ->
                                allowedParents.any { a ->
                                    a.equals(
                                        p,
                                        ignoreCase = true,
                                    )
                                }
                            } == true
                            )
                }
                ?.let { return it }
        }
        return null
    }
}
