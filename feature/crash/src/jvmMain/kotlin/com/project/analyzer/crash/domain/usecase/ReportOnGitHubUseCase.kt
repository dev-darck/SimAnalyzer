package com.project.analyzer.crash.domain.usecase

import com.project.analyzer.crash.domain.CrashReport

internal interface ReportOnGitHubUseCase {

    fun reportOnGitHub(report: CrashReport, githubRepo: String): Result<Unit>
}
