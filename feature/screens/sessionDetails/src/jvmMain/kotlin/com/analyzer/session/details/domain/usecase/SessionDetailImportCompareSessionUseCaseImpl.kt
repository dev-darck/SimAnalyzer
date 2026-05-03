package com.analyzer.session.details.domain.usecase

import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionSettings
import com.project.analyzer.telemetry.recording.api.file.TELEMETRY_SESSION_META_FILE_NAME
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionBundle
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionMetadata
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionStorage
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URI
import java.util.Locale

@Inject
@SingleIn(ScreenScope::class)
internal class SessionDetailImportCompareSessionUseCaseImpl(
    private val storage: RecordedTelemetrySessionStorage,
    private val settings: TelemetryAcquisitionSettings,
    private val json: Json,
) : SessionDetailImportCompareSessionUseCase {

    override suspend fun importSession(
        criteria: SessionDetailCompareCriteria,
        path: String,
    ): SessionDetailImportCompareSessionResult {
        val sourceDir = resolveImportSourceDir(path)
            ?: return SessionDetailImportCompareSessionResult.Rejected(
                reason = "Choose a Sim Analyzer session folder or drop a session.json file.",
            )
        val metadataFile = File(sourceDir, TELEMETRY_SESSION_META_FILE_NAME)
        if (!metadataFile.isFile) {
            return SessionDetailImportCompareSessionResult.Rejected(
                reason = "This folder does not contain a recorded Sim Analyzer session.",
            )
        }
        val metadata = runCatching {
            json.decodeFromString<RecordedTelemetrySessionMetadata>(metadataFile.readText())
        }.getOrElse { error ->
            return SessionDetailImportCompareSessionResult.Rejected(
                reason = error.message ?: "The dropped session metadata could not be read.",
            )
        }
        validateCompatibility(criteria, metadata)?.let { reason ->
            return SessionDetailImportCompareSessionResult.Rejected(reason)
        }

        val storageRoot = File(settings.currentConfig().storageLocation)
        if (!storageRoot.exists() && !storageRoot.mkdirs()) {
            return SessionDetailImportCompareSessionResult.Failure(
                reason = "The recordings folder could not be prepared for import.",
            )
        }

        val sourceCanonical = sourceDir.canonicalFile
        val storageCanonical = storageRoot.canonicalFile
        val existingBundles = storage.loadBundles(forceRefresh = true)
        if (sourceCanonical.isInside(
                storageCanonical,
            ) || existingBundles.containsSession(metadata.sessionId, sourceCanonical)
        ) {
            return SessionDetailImportCompareSessionResult.AlreadyAvailable(metadata.sessionId)
        }

        val targetDir = storageCanonical.resolveUniqueSessionDirectory(
            seedName = sourceCanonical.name.takeIf(String::isNotBlank) ?: "session-${metadata.sessionId}",
        )
        val copied = runCatching {
            sourceCanonical.copyRecursively(target = targetDir, overwrite = false) { _, _ ->
                OnErrorAction.TERMINATE
            }
        }
        if (copied.isFailure) {
            targetDir.deleteRecursively()
            return SessionDetailImportCompareSessionResult.Failure(
                reason = copied.exceptionOrNull()?.message
                    ?: "The compare session could not be imported into the library.",
            )
        }

        storage.loadBundles(forceRefresh = true)
        return SessionDetailImportCompareSessionResult.Imported(
            sessionId = metadata.sessionId,
            destinationPath = targetDir.absolutePath,
        )
    }

    private fun validateCompatibility(
        criteria: SessionDetailCompareCriteria,
        metadata: RecordedTelemetrySessionMetadata,
    ): String? {
        val gameId = metadata.gameId.normalizeIdentity()
        if (gameId == null || gameId != criteria.gameId.normalizeIdentity()) {
            return "Only ${criteria.gameLabel} recordings can be compared on this screen."
        }
        val trackId = metadata.trackId.normalizeIdentity()
        if (trackId == null || trackId != criteria.trackId.normalizeIdentity()) {
            return "Only recordings from ${criteria.trackLabel} can be compared here."
        }
        if (!matchesLayout(criteria.layoutId, metadata.layoutId)) {
            return "This recording uses a different track layout, so it was not added."
        }
        return null
    }

    private fun matchesLayout(expectedLayoutId: String?, actualLayoutId: String?): Boolean {
        val normalizedExpected = expectedLayoutId.normalizeIdentity()
        val normalizedActual = actualLayoutId.normalizeIdentity()
        return when {
            normalizedExpected == null && normalizedActual == null -> true
            normalizedExpected == null || normalizedActual == null -> false
            else -> normalizedExpected == normalizedActual
        }
    }

    private fun resolveImportSourceDir(path: String): File? {
        val candidate = path.toImportFile() ?: return null
        if (candidate.isDirectory) {
            if (File(candidate, TELEMETRY_SESSION_META_FILE_NAME).isFile) return candidate
            val nestedMatches = candidate.listFiles()
                .orEmpty()
                .filter(File::isDirectory)
                .filter { directory -> File(directory, TELEMETRY_SESSION_META_FILE_NAME).isFile }
            return nestedMatches.singleOrNull()
        }
        val parent = candidate.parentFile ?: return null
        return when {
            candidate.name.equals(TELEMETRY_SESSION_META_FILE_NAME, ignoreCase = true) -> parent
            File(parent, TELEMETRY_SESSION_META_FILE_NAME).isFile -> parent
            else -> null
        }
    }

    private fun String.toImportFile(): File? = runCatching {
        if (startsWith("file:", ignoreCase = true)) {
            File(URI(this))
        } else {
            File(this)
        }
    }.getOrNull()
}

private fun List<RecordedTelemetrySessionBundle>.containsSession(
    sessionId: Long,
    sourceDir: File,
): Boolean = any { bundle ->
    bundle.sessionId == sessionId ||
        bundle.locations.any { location -> location.dir.canonicalFile == sourceDir }
}

private fun File.isInside(root: File): Boolean = runCatching {
    canonicalFile.toPath().startsWith(root.canonicalFile.toPath())
}.getOrDefault(false)

private fun File.resolveUniqueSessionDirectory(seedName: String): File {
    val normalizedSeed = seedName
        .replace(Regex("[^A-Za-z0-9._-]+"), "-")
        .trim('-')
        .ifBlank { "session" }
    var index = 1
    var candidate = resolve(normalizedSeed)
    while (candidate.exists()) {
        index += 1
        candidate = resolve("$normalizedSeed-$index")
    }
    return candidate
}

private fun String?.normalizeIdentity(): String? = this
    ?.trim()
    ?.takeIf(String::isNotBlank)
    ?.lowercase(Locale.US)
