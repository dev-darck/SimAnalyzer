package com.project.analyzer.crash.presentation

import androidx.lifecycle.viewModelScope
import com.project.analyzer.crash.domain.CrashReport
import com.project.analyzer.crash.domain.usecase.CopyReportUseCase
import com.project.analyzer.crash.domain.usecase.OpenFileUseCase
import com.project.analyzer.crash.domain.usecase.OpenLogsFolderUseCase
import com.project.analyzer.crash.domain.usecase.ReportOnGitHubUseCase
import com.project.analyzer.leak.api.LeakAwareViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class CrashViewModel(
    private val copyReportUseCase: CopyReportUseCase,
    private val openFileUseCase: OpenFileUseCase,
    private val openLogsFolderUseCase: OpenLogsFolderUseCase,
    private val reportOnGitHubUseCase: ReportOnGitHubUseCase,
) : LeakAwareViewModel() {

    private val _state = MutableStateFlow(CrashScreenState(CrashReport()))
    val state = _state.asStateFlow()

    private val _actions = MutableSharedFlow<CrashScreenAction>()
    val actions = _actions.asSharedFlow()

    fun dispatch(event: CrashScreenUiEvent) {
        when (event) {
            is CrashScreenUiEvent.Init -> _state.update { it.copy(report = event.report) }
            is CrashScreenUiEvent.CopyReport -> copyReport()
            is CrashScreenUiEvent.CopyStacktrace -> copyStacktrace()
            is CrashScreenUiEvent.ReportOnGitHub -> reportOnGitHub(event.githubRepo)
            is CrashScreenUiEvent.OpenLogsFolder -> openLogsFolder()
            is CrashScreenUiEvent.OpenCrashFile -> openCrashFile()
            is CrashScreenUiEvent.OnTabChange -> onTabChange(event.tab)
        }
    }

    private fun copyReport() {
        copyReportUseCase(state.value.report.fullText)
            .onSuccess { showSnackbar(CrashSnackbarMessage.ReportCopied) }
            .onFailure { showSnackbar(CrashSnackbarMessage.CopyFailed(it.message ?: it.toString())) }
    }

    private fun copyStacktrace() {
        copyReportUseCase(state.value.report.stacktrace)
            .onSuccess { showSnackbar(CrashSnackbarMessage.StacktraceCopied) }
            .onFailure { showSnackbar(CrashSnackbarMessage.CopyFailed(it.message ?: it.toString())) }
    }

    private fun reportOnGitHub(githubRepo: String) {
        reportOnGitHubUseCase(state.value.report, githubRepo)
            .onSuccess { showSnackbar(CrashSnackbarMessage.ReportOpeningGitHub) }
            .onFailure { showSnackbar(CrashSnackbarMessage.OpenBrowserFailed(it.message ?: it.toString())) }
    }

    private fun openLogsFolder() {
        openLogsFolderUseCase(state.value.report.logDir)
            .onFailure { showSnackbar(CrashSnackbarMessage.OpenLogsFailed(it.message ?: it.toString())) }
    }

    private fun openCrashFile() {
        state.value.report.savedReportPath?.let {
            openFileUseCase(it)
                .onFailure { showSnackbar(CrashSnackbarMessage.OpenCrashFileFailed(it.message ?: it.toString())) }
        }
    }

    private fun onTabChange(tab: Int) {
        _state.update { it.copy(selectedTab = tab) }
    }

    private fun showSnackbar(message: CrashSnackbarMessage) {
        viewModelScope.launch {
            _actions.emit(CrashScreenAction.ShowSnackbar(message))
        }
    }
}
