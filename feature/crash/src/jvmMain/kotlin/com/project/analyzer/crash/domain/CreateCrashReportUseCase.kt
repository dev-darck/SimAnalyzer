package com.project.analyzer.crash.domain

public interface CreateCrashReportUseCase {

    public fun createReport(
        throwable: Throwable,
        thread: Thread,
        title: String = "Application crash",
        appVersion: String? = null,
    ): CrashReport
}
