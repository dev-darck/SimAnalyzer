package com.project.analyzer.ac.telemetry.impl.fallback.logfile

import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.Inject
import java.io.File
import kotlin.math.min
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

@Inject
class AcEvoLogLocator {

    @Volatile
    private var cached: File? = null

    @Volatile
    private var lastExplicitScanMs: Long = 0L

    @Volatile
    private var lastWalkScanMs: Long = 0L

    private val candidates: List<File> by lazy(::buildCandidates)
    private val walkRoots: List<File> by lazy { candidates.mapNotNull { it.parentFile }.distinct() }

    fun locateLogFile(): File? {
        readOverridePath()?.let { f ->
            cacheResolved(f, source = "override")
            return f
        }

        val nowMs = System.currentTimeMillis()

        resolveExplicitCandidate(nowMs)?.let { return it }

        cached?.takeIf { it.isFile }?.let {
            if (nowMs - lastWalkScanMs < WALK_RESCAN_INTERVAL_MS) return it
        }

        lastWalkScanMs = nowMs
        val found = walkForLogTxt(startDirs = walkRoots, nowMs = nowMs)
        if (found != null) {
            cacheResolved(found, source = "walk")
            return found
        }

        cached = cached?.takeIf { it.isFile && isFreshCandidate(it, nowMs) }
        return cached
    }

    fun clear() {
        cached = null
        lastExplicitScanMs = 0L
        lastWalkScanMs = 0L
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

        val savedGamesDirs = linkedSetOf<File>()
        val docsDirs = linkedSetOf<File>()

        fun addSavedGames(base: String?) {
            if (base.isNullOrBlank()) return
            savedGamesDirs += File(base, "Saved Games")
        }

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

        addSavedGames(home)
        addSavedGames(userProfile)
        addSavedGames(oneDrive)

        if (!homeDrive.isNullOrBlank() && !homePath.isNullOrBlank()) {
            savedGamesDirs += File(homeDrive + homePath, "Saved Games")
        }

        val result = ArrayList<File>(docsDirs.size * 6)

        for (savedGames in savedGamesDirs) {
            result += File(savedGames, "ACE/log.txt")

            result += File(savedGames, "Assetto Corsa Evo/log.txt")
            result += File(savedGames, "Assetto Corsa EVO/log.txt")
            result += File(savedGames, "Assetto Corsa Evo/logs/log.txt")
            result += File(savedGames, "Assetto Corsa EVO/logs/log.txt")
        }

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

    private fun resolveExplicitCandidate(nowMs: Long): File? {
        val cachedFile = cached?.takeIf { it.isFile && isFreshCandidate(it, nowMs) }
        if (cachedFile != null && nowMs - lastExplicitScanMs < EXPLICIT_RESCAN_INTERVAL_MS) {
            return cachedFile
        }

        lastExplicitScanMs = nowMs

        val explicit = candidates
            .asSequence()
            .filter { isFreshCandidate(it, nowMs) }
            .maxByOrNull { it.lastModified() }

        if (explicit != null) {
            cacheResolved(explicit, source = "explicit")
            return explicit
        }

        cached = cachedFile
        return cachedFile
    }

    private fun cacheResolved(file: File, source: String) {
        val old = cached
        cached = file
        if (old?.absolutePath != file.absolutePath) {
            logger.debug {
                "AcEvoLogLocator selected log file: ${file.absolutePath} " +
                    "(source=$source, ageMs=${System.currentTimeMillis() - file.lastModified()})"
            }
        }
    }

    private fun isFreshCandidate(file: File, nowMs: Long): Boolean {
        if (!file.isFile) return false
        val lastModified = file.lastModified()
        if (lastModified <= 0L) return false
        val ageMs = nowMs - lastModified
        return ageMs in 0..AUTO_PICK_MAX_AGE_MS
    }

    private fun walkForLogTxt(startDirs: List<File>, nowMs: Long): File? {
        val allowedParents = setOf(
            "ACE",
            "Assetto Corsa Evo",
            "Assetto Corsa EVO",
            "logs",
        )

        var freshest: File? = null
        for (dir in startDirs) {
            if (!dir.isDirectory) continue
            dir.walkTopDown()
                .maxDepth(4)
                .onEnter { it.isDirectory && !it.name.startsWith(".") }
                .filter {
                    it.isFile &&
                        isFreshCandidate(it, nowMs) &&
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
                .forEach { candidate ->
                    if (freshest == null || candidate.lastModified() > freshest.lastModified()) {
                        freshest = candidate
                    }
                }
        }
        return freshest
    }

    private companion object {

        val EXPLICIT_RESCAN_INTERVAL_MS = 1.seconds.inWholeMilliseconds
        val WALK_RESCAN_INTERVAL_MS = 10.seconds.inWholeMilliseconds
        val AUTO_PICK_MAX_AGE_MS = 12.hours.inWholeMilliseconds
    }
}
