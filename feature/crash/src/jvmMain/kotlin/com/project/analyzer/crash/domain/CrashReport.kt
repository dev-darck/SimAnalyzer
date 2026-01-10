package com.project.analyzer.crash.domain

data class CrashReport(
    val title: String = "",
    val time: String = "",
    val threadName: String = "",
    val os: String = "",
    val java: String = "",
    val appVersion: String? = null,
    val stacktrace: String = "",
    val fullText: String = "",

    val logDir: String = "",
    val savedReportPath: String? = null,
)
