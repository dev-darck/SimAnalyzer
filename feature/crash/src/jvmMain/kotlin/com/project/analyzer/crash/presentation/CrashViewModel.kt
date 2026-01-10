package com.project.analyzer.crash.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.analyzer.crash.domain.CrashReport
import com.project.analyzer.crash.domain.usecase.CopyReportUseCase
import com.project.analyzer.crash.domain.usecase.OpenFileUseCase
import com.project.analyzer.crash.domain.usecase.OpenLogsFolderUseCase
import com.project.analyzer.crash.domain.usecase.ReportOnGitHubUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CrashViewModel(
    private val copyReportUseCase: CopyReportUseCase,
    private val openFileUseCase: OpenFileUseCase,
    private val openLogsFolderUseCase: OpenLogsFolderUseCase,
    private val reportOnGitHubUseCase: ReportOnGitHubUseCase
) : ViewModel() {

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
            .onSuccess { showSnackbar("Copied report to clipboard") }
            .onFailure { showSnackbar("Copy failed: ${it.message ?: it}") }
    }

    private fun copyStacktrace() {
        copyReportUseCase(state.value.report.stacktrace)
            .onSuccess { showSnackbar("Copied stacktrace to clipboard") }
            .onFailure { showSnackbar("Copy failed: ${it.message ?: it}") }
    }

    private fun reportOnGitHub(githubRepo: String) {
        reportOnGitHubUseCase(state.value.report, githubRepo)
            .onSuccess { showSnackbar("Full report copied. Opening GitHub…") }
            .onFailure { showSnackbar("Failed to open browser: ${it.message ?: it}") }
    }

    private fun openLogsFolder() {
        openLogsFolderUseCase(state.value.report.logDir)
            .onFailure { showSnackbar("Failed to open logs folder: ${it.message ?: it}") }
    }

    private fun openCrashFile() {
        state.value.report.savedReportPath?.let {
            openFileUseCase(it)
                .onFailure { showSnackbar("Failed to open crash file: ${it.message ?: it}") }
        }
    }

    private fun onTabChange(tab: Int) {
        _state.update { it.copy(selectedTab = tab) }
    }

    private fun showSnackbar(message: String) {
        viewModelScope.launch {
            _actions.emit(CrashScreenAction.ShowSnackbar(message))
        }
    }
}
