package com.analyzer.session.analysis.domain.usecase

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
internal class SessionAnalysisShareResultsUseCaseImpl(private val storage: RecordedTelemetrySessionStorage,) :
    SessionAnalysisShareResultsUseCase {

    override suspend fun copySummary(summaryText: String): SessionAnalysisShareResults {
        val copied = runCatching {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(
                StringSelection(summaryText),
                null,
            )
        }.isSuccess
        return if (copied) {
            SessionAnalysisShareResults.CopiedSummary
        } else {
            SessionAnalysisShareResults.Failure("Couldn't copy the analysis summary to the clipboard.")
        }
    }

    override suspend fun exportReport(
        directoryPath: String,
        reportFileName: String,
        summaryText: String,
    ): SessionAnalysisShareResults {
        val directory = File(directoryPath)
        if (!directory.exists() || !directory.isDirectory) {
            return SessionAnalysisShareResults.Failure("The selected export folder is no longer available.")
        }

        return runCatching {
            val target = directory.resolveUniqueReportFile(reportFileName)
            target.writeText(summaryText, StandardCharsets.UTF_8)
            SessionAnalysisShareResults.ExportedReport(target.absolutePath)
        }.getOrElse { error ->
            SessionAnalysisShareResults.Failure(
                error.message ?: "Couldn't export the analysis report to the selected folder.",
            )
        }
    }

    override suspend fun openSessionFiles(sessionId: Long): SessionAnalysisShareResults {
        val bundle = storage.findBundle(sessionId = sessionId)
            ?: return SessionAnalysisShareResults.Failure("No recorded files were found for this session.")
        val directories = bundle.locations.map(RecordedTelemetrySessionLocation::dir).distinct()
        val target = directories.resolveShareTarget()
            ?: return SessionAnalysisShareResults.Failure("The recorded session folder is no longer available.")

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
            openedFolder && copiedPath -> SessionAnalysisShareResults.OpenedSessionFilesAndCopiedPath
            openedFolder -> SessionAnalysisShareResults.OpenedSessionFiles
            copiedPath -> SessionAnalysisShareResults.CopiedSessionFilesPath
            else -> SessionAnalysisShareResults.Failure("Couldn't open the session folder or copy its path.")
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
            else -> maxByOrNull(File::lastModified)?.takeIf { it.exists() }
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
