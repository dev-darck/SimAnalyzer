package com.project.analyzer.crash.domain.usecase

import com.project.analyzer.crash.domain.CrashReport
import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder

internal class ReportOnGitHubUseCase(private val copyReportUseCase: CopyReportUseCase) {

    operator fun invoke(report: CrashReport, githubRepo: String): Result<Unit> {
        copyReportUseCase(report.fullText)

        val url = buildGithubIssueUrl(
            repo = githubRepo,
            title = "Crash: ${report.title}",
            body = buildIssueBody(report),
            maxBodyChars = 3500,
        )

        return browse(url)
    }

    private fun buildIssueBody(report: CrashReport): String = buildString {
        appendLine("### What happened?")
        appendLine("The application crashed.")
        appendLine()
        appendLine("### Crash snippet")
        appendLine("```")
        appendLine(report.stacktrace.take(1800))
        appendLine("```")
    }

    private fun buildGithubIssueUrl(repo: String, title: String, body: String, maxBodyChars: Int): String {
        val base = "https://github.com/$repo/issues/new"
        val safeBody = if (body.length <= maxBodyChars) {
            body
        } else {
            body.take(maxBodyChars) + "\n\n(Body truncated by the app. Paste the full report from clipboard.)"
        }

        val t = URLEncoder.encode(title, "UTF-8")
        val b = URLEncoder.encode(safeBody, "UTF-8")
        return "$base?title=$t&body=$b"
    }

    private fun browse(url: String): Result<Unit> = runCatching {
        if (!Desktop.isDesktopSupported()) error("Desktop is not supported")
        Desktop.getDesktop().browse(URI(url))
    }
}
