package com.project.analyzer.crash.domain

internal interface CreateCrashReportUseCase {

    fun createReport(
        throwable: Throwable,
        thread: Thread,
        title: String = "Application crash",
        appVersion: String? = null,
    ): CrashReport
}
