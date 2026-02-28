package com.project.analyzer.crash.presentation

import com.project.analyzer.crash.domain.CrashReport

data class CrashScreenState(val report: CrashReport, val selectedTab: Int = 0)

sealed interface CrashScreenUiEvent {
    data class Init(val report: CrashReport) : CrashScreenUiEvent
    data object CopyReport : CrashScreenUiEvent
    data object CopyStacktrace : CrashScreenUiEvent
    data class ReportOnGitHub(val githubRepo: String) : CrashScreenUiEvent
    data object OpenLogsFolder : CrashScreenUiEvent
    data object OpenCrashFile : CrashScreenUiEvent
    data class OnTabChange(val tab: Int) : CrashScreenUiEvent
}

sealed interface CrashScreenAction {
    data class ShowSnackbar(val message: CrashSnackbarMessage) : CrashScreenAction
}

sealed interface CrashSnackbarMessage {
    data object ReportCopied : CrashSnackbarMessage
    data object StacktraceCopied : CrashSnackbarMessage
    data object ReportOpeningGitHub : CrashSnackbarMessage
    data class CopyFailed(val reason: String) : CrashSnackbarMessage
    data class OpenBrowserFailed(val reason: String) : CrashSnackbarMessage
    data class OpenLogsFailed(val reason: String) : CrashSnackbarMessage
    data class OpenCrashFileFailed(val reason: String) : CrashSnackbarMessage
}
