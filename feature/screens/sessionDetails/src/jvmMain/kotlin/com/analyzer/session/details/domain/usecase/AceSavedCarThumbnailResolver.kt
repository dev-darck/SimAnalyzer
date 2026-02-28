package com.analyzer.session.details.domain.usecase

import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.File
import kotlin.math.abs

data class SavedCarThumbnailMatch(val savedCarId: String, val texturePath: String)

@Inject
@SingleIn(ScreenScope::class)
class AceSavedCarThumbnailResolver {

    fun resolve(gameId: String, carModel: String?, sessionStartedAtMs: Long): SavedCarThumbnailMatch? {
        if (!gameId.equals(ACE_GAME_ID, ignoreCase = true)) return null
        val normalizedCarModel = carModel?.trim()?.takeIf { it.isNotBlank() } ?: return null

        val roots = locateAceRoots()
        if (roots.isEmpty()) return null

        roots.forEach { root ->
            val thumbnailDir = File(root, THUMBNAILS_RELATIVE_PATH)
            if (!thumbnailDir.isDirectory) return@forEach

            val candidates = buildCandidates(
                carModel = normalizedCarModel,
                sessionStartedAtMs = sessionStartedAtMs,
                thumbnailDir = thumbnailDir,
                savedCarsDirs = locateSavedCarsDirs(root),
            )
            if (candidates.isNotEmpty()) {
                // TODO: Replace model-based disambiguation with an exact saved-car GUID from ACE once
                // the game exposes a stable mapping between the active session car and SavedCars IDs.
                return candidates.minWithOrNull(
                    compareBy<ThumbnailCandidate> { abs(it.savedCarFile.lastModified() - sessionStartedAtMs) }
                        .thenByDescending { it.savedCarFile.lastModified() }
                        .thenBy { it.savedCarId },
                )?.toMatch()
            }
        }

        return null
    }

    private fun buildCandidates(
        carModel: String,
        sessionStartedAtMs: Long,
        thumbnailDir: File,
        savedCarsDirs: List<File>,
    ): List<ThumbnailCandidate> = savedCarsDirs
        .asSequence()
        .flatMap { dir ->
            dir.listFiles()
                .orEmpty()
                .asSequence()
                .filter { it.isFile && it.extension.equals(SAVED_CAR_EXTENSION, ignoreCase = true) }
        }
        .mapNotNull { savedCarFile ->
            if (!savedCarFile.name.startsWith("${carModel}_", ignoreCase = true)) return@mapNotNull null

            val savedCarId = SAVED_CAR_ID_REGEX.find(savedCarFile.nameWithoutExtension)?.groupValues?.get(1)
                ?: return@mapNotNull null
            val textureFile = File(thumbnailDir, "$savedCarId.texture")
            if (!textureFile.isFile) return@mapNotNull null

            ThumbnailCandidate(
                savedCarId = savedCarId,
                savedCarFile = savedCarFile,
                textureFile = textureFile,
            )
        }
        .sortedBy { abs(it.savedCarFile.lastModified() - sessionStartedAtMs) }
        .toList()

    private fun locateAceRoots(): List<File> {
        val home = System.getProperty("user.home")
        val userProfile = System.getenv("USERPROFILE") ?: home
        val oneDrive = System.getenv("OneDrive")
            ?: System.getenv("OneDriveConsumer")
            ?: System.getenv("OneDriveCommercial")

        val roots = linkedSetOf<File>()

        fun addRoot(base: String?, child: String) {
            if (base.isNullOrBlank()) return
            roots += File(base, child)
        }

        addRoot(home, "Saved Games/ACE")
        addRoot(userProfile, "Saved Games/ACE")
        addRoot(oneDrive, "Saved Games/ACE")

        addRoot(home, "Documents/ACE")
        addRoot(userProfile, "Documents/ACE")
        addRoot(oneDrive, "Documents/ACE")

        val homeDrive = System.getenv("HOMEDRIVE")
        val homePath = System.getenv("HOMEPATH")
        if (!homeDrive.isNullOrBlank() && !homePath.isNullOrBlank()) {
            roots += File(homeDrive + homePath, "Saved Games/ACE")
            roots += File(homeDrive + homePath, "Documents/ACE")
        }

        return roots.filter { it.isDirectory }
    }

    private fun locateSavedCarsDirs(root: File): List<File> {
        val profileDataDir = File(root, PROFILE_DATA_RELATIVE_PATH)
        if (!profileDataDir.isDirectory) return emptyList()

        return profileDataDir
            .listFiles()
            .orEmpty()
            .asSequence()
            .filter { it.isDirectory }
            .map { File(it, OPEN_DATA_SAVED_CARS_RELATIVE_PATH) }
            .filter { it.isDirectory }
            .sortedByDescending { it.lastModified() }
            .toList()
    }

    private data class ThumbnailCandidate(val savedCarId: String, val savedCarFile: File, val textureFile: File) {
        fun toMatch(): SavedCarThumbnailMatch = SavedCarThumbnailMatch(
            savedCarId = savedCarId,
            texturePath = textureFile.absolutePath,
        )
    }

    private companion object {

        const val ACE_GAME_ID = "ACE"
        const val THUMBNAILS_RELATIVE_PATH = "SavedCars/Thumbnails"
        const val PROFILE_DATA_RELATIVE_PATH = "ProfileData"
        const val OPEN_DATA_SAVED_CARS_RELATIVE_PATH = "OpenData/SavedCars"
        const val SAVED_CAR_EXTENSION = "carfinalstatewithconsumable"
        val SAVED_CAR_ID_REGEX = Regex("([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})$")
    }
}
