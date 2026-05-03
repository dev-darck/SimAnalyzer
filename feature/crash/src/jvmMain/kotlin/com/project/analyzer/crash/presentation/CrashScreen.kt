@file:Suppress("WildcardImport", "NoWildcardImports")

package com.project.analyzer.crash.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.analyzer.crash.domain.CrashReport
import com.project.analyzer.crash.presentation.components.CrashCodePanel
import com.project.analyzer.crash.presentation.components.CrashOverviewPanel
import com.project.analyzer.feature.crash.Res.Res
import com.project.analyzer.feature.crash.Res.crash_exit
import com.project.analyzer.feature.crash.Res.crash_screen_subtitle
import com.project.analyzer.feature.crash.Res.crash_screen_title
import com.project.analyzer.feature.crash.Res.crash_snackbar_copy_failed
import com.project.analyzer.feature.crash.Res.crash_snackbar_open_browser_failed
import com.project.analyzer.feature.crash.Res.crash_snackbar_open_file_failed
import com.project.analyzer.feature.crash.Res.crash_snackbar_open_logs_failed
import com.project.analyzer.feature.crash.Res.crash_snackbar_report_copied
import com.project.analyzer.feature.crash.Res.crash_snackbar_report_opening_github
import com.project.analyzer.feature.crash.Res.crash_snackbar_stacktrace_copied
import com.project.analyzer.feature.crash.Res.crash_tab_full_report
import com.project.analyzer.feature.crash.Res.crash_tab_overview
import com.project.analyzer.feature.crash.Res.crash_tab_stacktrace
import com.project.analyzer.theme.SimAnalyzerTheme
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

private data class CrashTabSpec(val title: StringResource, val icon: ImageVector)

private val crashTabs = listOf(
    CrashTabSpec(Res.string.crash_tab_overview, Icons.Outlined.Warning),
    CrashTabSpec(Res.string.crash_tab_full_report, Icons.Default.Info),
    CrashTabSpec(Res.string.crash_tab_stacktrace, Icons.Default.Edit),
)

@Composable
internal fun CrashScreen(crashReport: CrashReport, onExit: () -> Unit) {
    val viewModel: CrashViewModel = metroViewModel()
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.actions) {
        viewModel.dispatch(CrashScreenUiEvent.Init(crashReport))
        viewModel.actions.onEach { action ->
            when (action) {
                is CrashScreenAction.ShowSnackbar -> snackbar.showSnackbar(action.message.resolveText())
            }
        }.launchIn(this)
    }

    SimAnalyzerTheme {
        CrashScreenContent(
            report = state.report,
            snackbarHostState = snackbar,
            onEvent = viewModel::dispatch,
            onExit = onExit,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CrashScreenContent(
    report: CrashReport,
    snackbarHostState: SnackbarHostState,
    onEvent: (CrashScreenUiEvent) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(Res.string.crash_screen_title),
                            style = SimAnalyzerTheme.typography.titleSmall,
                        )
                        Text(
                            text = stringResource(Res.string.crash_screen_subtitle),
                            style = SimAnalyzerTheme.typography.labelMedium,
                            color = SimAnalyzerTheme.material.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 12.dp, end = 8.dp)
                            .size(28.dp),
                        tint = SimAnalyzerTheme.material.error,
                    )
                },
                actions = {
                    Button(
                        onClick = onExit,
                        modifier = Modifier.padding(end = 12.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.crash_exit),
                            style = SimAnalyzerTheme.typography.labelMedium,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                crashTabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = stringResource(tab.title),
                                    style = SimAnalyzerTheme.typography.labelMedium,
                                )
                            }
                        },
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = SimAnalyzerTheme.corners.overlay,
                color = SimAnalyzerTheme.material.surface,
                border = BorderStroke(1.dp, SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.5f)),
                tonalElevation = 1.dp,
            ) {
                when (selectedTab) {
                    0 -> CrashOverviewPanel(report, onEvent)

                    1 -> CrashCodePanel(
                        text = report.fullText,
                        title = stringResource(Res.string.crash_tab_full_report),
                    )

                    2 -> CrashCodePanel(
                        text = report.stacktrace,
                        title = stringResource(Res.string.crash_tab_stacktrace),
                    )
                }
            }
        }
    }
}

private suspend fun CrashSnackbarMessage.resolveText(): String = when (this) {
    CrashSnackbarMessage.ReportCopied -> getString(Res.string.crash_snackbar_report_copied)
    CrashSnackbarMessage.StacktraceCopied -> getString(Res.string.crash_snackbar_stacktrace_copied)
    CrashSnackbarMessage.ReportOpeningGitHub -> getString(Res.string.crash_snackbar_report_opening_github)
    is CrashSnackbarMessage.CopyFailed -> getString(Res.string.crash_snackbar_copy_failed, reason)
    is CrashSnackbarMessage.OpenBrowserFailed -> getString(Res.string.crash_snackbar_open_browser_failed, reason)
    is CrashSnackbarMessage.OpenLogsFailed -> getString(Res.string.crash_snackbar_open_logs_failed, reason)
    is CrashSnackbarMessage.OpenCrashFileFailed -> getString(Res.string.crash_snackbar_open_file_failed, reason)
}

@Preview
@Composable
private fun CrashScreenPreview() {
    val report = CrashReport(
        time = "2023-10-27 10:00:00",
        threadName = "main",
        stacktrace = "java.lang.RuntimeException: Test exception\n\tat " +
            "com.project.analyzer.crash.presentation.CrashScreenPreview(CrashScreen.kt:100)",
        fullText = "Full crash report text...",
        appVersion = "1.0.0",
        os = "Windows 11",
        java = "17.0.1",
    )
    SimAnalyzerTheme {
        CrashScreenContent(
            report = report,
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
            onExit = {},
        )
    }
}
