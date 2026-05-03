package com.analyzer.session.details.domain.usecase

import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionLocation
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionStorage
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.nio.charset.StandardCharsets

@Inject
@SingleIn(ScreenScope::class)
internal class SessionDetailShareResultsUseCaseImpl(private val storage: RecordedTelemetrySessionStorage,) :
    SessionDetailShareResultsUseCase {

    override suspend fun copySummary(summaryText: String): SessionDetailShareResults {
        val copied = runCatching {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(
                StringSelection(summaryText),
                null,
            )
        }.isSuccess
        return if (copied) {
            SessionDetailShareResults.CopiedSummary
        } else {
            SessionDetailShareResults.Failure("Couldn't copy the session summary to the clipboard.")
        }
    }

    override suspend fun exportReport(
        directoryPath: String,
        reportFileName: String,
        summaryText: String,
    ): SessionDetailShareResults {
        val directory = File(directoryPath)
        if (!directory.exists() || !directory.isDirectory) {
            return SessionDetailShareResults.Failure("The selected export folder is no longer available.")
        }

        return runCatching {
            val target = directory.resolveUniqueReportFile(reportFileName)
            target.writeText(summaryText, StandardCharsets.UTF_8)
            SessionDetailShareResults.ExportedReport(target.absolutePath)
        }.getOrElse { error ->
            SessionDetailShareResults.Failure(
                error.message ?: "Couldn't export the session report to the selected folder.",
            )
        }
    }

    override suspend fun openSessionFiles(sessionId: Long): SessionDetailShareResults {
        val bundle = storage.findBundle(sessionId = sessionId)
            ?: return SessionDetailShareResults.Failure("No recorded files were found for this session.")
        val directories = bundle.locations.map(RecordedTelemetrySessionLocation::dir).distinct()
        val target = directories.resolveShareTarget()
            ?: return SessionDetailShareResults.Failure("The recorded session folder is no longer available.")

        val openedFolder = runCatching {
            check(Desktop.isDesktopSupported()) { "Desktop integration is not available on this system." }
            Desktop.getDesktop().open(target)
        }.isSuccess
        val copiedPath = runCatching {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(
                StringSelection(target.absolutePath),
                null,
            )
        }.isSuccess

        return when {
            openedFolder && copiedPath -> SessionDetailShareResults.OpenedSessionFilesAndCopiedPath
            openedFolder -> SessionDetailShareResults.OpenedSessionFiles
            copiedPath -> SessionDetailShareResults.CopiedSessionFilesPath
            else -> SessionDetailShareResults.Failure("Couldn't open the session folder or copy its path.")
        }
    }
}

private fun List<File>.resolveShareTarget(): File? = when {
    isEmpty() -> null

    size == 1 -> first().takeIf { it.exists() }

    else -> {
        val parents = mapNotNull(File::getParentFile)
            .filter { it.exists() }
            .distinctBy { it.absolutePath }
        when {
            parents.size == 1 -> parents.single()
            else -> maxByOrNull { it.lastModified() }?.takeIf { it.exists() }
        }
    }
}

private fun File.resolveUniqueReportFile(reportFileName: String): File {
    val extension = reportFileName.substringAfterLast('.', "")
    val baseName = if (extension.isBlank()) {
        reportFileName
    } else {
        reportFileName.removeSuffix(".$extension")
    }
    var candidate = File(this, reportFileName)
    var index = 2
    while (candidate.exists()) {
        val suffix = if (extension.isBlank()) {
            "$baseName-$index"
        } else {
            "$baseName-$index.$extension"
        }
        candidate = File(this, suffix)
        index += 1
    }
    return candidate
}
