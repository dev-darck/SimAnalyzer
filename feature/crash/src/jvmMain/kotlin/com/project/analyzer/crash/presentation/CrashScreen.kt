@file:Suppress("WildcardImport", "NoWildcardImports")

package com.project.analyzer.crash.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.analyzer.crash.di.CrashGraph
import com.project.analyzer.crash.domain.CrashReport
import com.project.analyzer.feature.crash.Res.Res
import com.project.analyzer.feature.crash.Res.crash_action_copy_report
import com.project.analyzer.feature.crash.Res.crash_action_copy_stacktrace
import com.project.analyzer.feature.crash.Res.crash_action_open_crash_file
import com.project.analyzer.feature.crash.Res.crash_action_open_logs
import com.project.analyzer.feature.crash.Res.crash_action_report_github
import com.project.analyzer.feature.crash.Res.crash_actions_title
import com.project.analyzer.feature.crash.Res.crash_code_panel_lines
import com.project.analyzer.feature.crash.Res.crash_exit
import com.project.analyzer.feature.crash.Res.crash_metadata_app_version
import com.project.analyzer.feature.crash.Res.crash_metadata_java
import com.project.analyzer.feature.crash.Res.crash_metadata_os
import com.project.analyzer.feature.crash.Res.crash_metadata_thread
import com.project.analyzer.feature.crash.Res.crash_metadata_time
import com.project.analyzer.feature.crash.Res.crash_metadata_title
import com.project.analyzer.feature.crash.Res.crash_overview_message
import com.project.analyzer.feature.crash.Res.crash_overview_title
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
import com.project.analyzer.ui.scrollbar.AppHorizontalScrollbar
import com.project.analyzer.ui.scrollbar.AppScrollbarAdapter
import com.project.analyzer.ui.scrollbar.AppVerticalScrollbar
import dev.zacsweers.metro.createGraph
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

private const val DEFAULT_GITHUB_REPO = "dev-darck/SimAnalyzer"

private data class CrashTabSpec(val title: StringResource, val icon: ImageVector)

private val crashTabs = listOf(
    CrashTabSpec(Res.string.crash_tab_overview, Icons.Outlined.Warning),
    CrashTabSpec(Res.string.crash_tab_full_report, Icons.Default.Info),
    CrashTabSpec(Res.string.crash_tab_stacktrace, Icons.Default.Edit),
)

@Composable
internal fun CrashScreen(crashReport: CrashReport, onExit: () -> Unit) {
    val graph = remember { createGraph<CrashGraph>() }
    val viewModel = graph.crashViewModel
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
                    0 -> OverviewPanel(report, onEvent)
                    1 -> CodePanel(text = report.fullText, title = stringResource(Res.string.crash_tab_full_report))
                    2 -> CodePanel(text = report.stacktrace, title = stringResource(Res.string.crash_tab_stacktrace))
                }
            }
        }
    }
}

@Composable
private fun OverviewPanel(report: CrashReport, onEvent: (CrashScreenUiEvent) -> Unit) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = SimAnalyzerTheme.material.errorContainer.copy(alpha = 0.32f),
                ),
                border = BorderStroke(1.dp, SimAnalyzerTheme.material.error.copy(alpha = 0.4f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = SimAnalyzerTheme.material.error,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(Res.string.crash_overview_title),
                            style = SimAnalyzerTheme.typography.titleSmall,
                        )
                        Text(
                            text = stringResource(Res.string.crash_overview_message),
                            style = SimAnalyzerTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            CrashMetadata(report = report)
            CrashActions(onEvent = onEvent)
        }
        AppVerticalScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(vertical = 8.dp),
            adapter = AppScrollbarAdapter(rememberScrollbarAdapter(scrollState)),
        )
    }
}

@Composable
private fun CrashMetadata(report: CrashReport) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.35f),
        ),
        border = BorderStroke(1.dp, SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(Res.string.crash_metadata_title), style = SimAnalyzerTheme.typography.titleMedium)
            MetadataEntry(stringResource(Res.string.crash_metadata_time), report.time)
            MetadataEntry(stringResource(Res.string.crash_metadata_thread), report.threadName)
            report.appVersion?.takeIf { it.isNotBlank() }?.let {
                MetadataEntry(stringResource(Res.string.crash_metadata_app_version), it)
            }
            MetadataEntry(stringResource(Res.string.crash_metadata_os), report.os)
            MetadataEntry(stringResource(Res.string.crash_metadata_java), report.java)
        }
    }
}

@Composable
private fun MetadataEntry(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = SimAnalyzerTheme.typography.labelMedium,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
        )
        Text(
            text = value,
            style = SimAnalyzerTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CrashActions(onEvent: (CrashScreenUiEvent) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.22f),
        ),
        border = BorderStroke(1.dp, SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(Res.string.crash_actions_title), style = SimAnalyzerTheme.typography.titleMedium)
            CrashActionButton(
                text = stringResource(Res.string.crash_action_report_github),
                icon = Icons.Default.Share,
                emphasized = true,
                onClick = { onEvent(CrashScreenUiEvent.ReportOnGitHub(DEFAULT_GITHUB_REPO)) },
            )
            CrashActionButton(
                text = stringResource(Res.string.crash_action_copy_report),
                icon = Icons.Default.Edit,
                onClick = { onEvent(CrashScreenUiEvent.CopyReport) },
            )
            CrashActionButton(
                text = stringResource(Res.string.crash_action_copy_stacktrace),
                icon = Icons.Default.Edit,
                onClick = { onEvent(CrashScreenUiEvent.CopyStacktrace) },
            )
            CrashActionButton(
                text = stringResource(Res.string.crash_action_open_logs),
                icon = Icons.Default.Info,
                onClick = { onEvent(CrashScreenUiEvent.OpenLogsFolder) },
            )
            CrashActionButton(
                text = stringResource(Res.string.crash_action_open_crash_file),
                icon = Icons.Default.Info,
                onClick = { onEvent(CrashScreenUiEvent.OpenCrashFile) },
            )
        }
    }
}

@Composable
private fun CrashActionButton(text: String, icon: ImageVector, emphasized: Boolean = false, onClick: () -> Unit) {
    if (emphasized) {
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = text, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                style = SimAnalyzerTheme.typography.labelMedium,
            )
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = text, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                style = SimAnalyzerTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun CodePanel(text: String, title: String) {
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()
    val lineCount = remember(text) {
        if (text.isBlank()) {
            0
        } else {
            text.count { it == '\n' } + 1
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.42f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = SimAnalyzerTheme.material.onSurfaceVariant,
            )
            Text(
                text = stringResource(Res.string.crash_code_panel_lines, title, lineCount),
                style = SimAnalyzerTheme.typography.labelMedium,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
            )
        }

        HorizontalDivider(color = SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.5f))

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.24f),
        ) {
            SelectionContainer {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = text,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(vScroll)
                            .horizontalScroll(hScroll)
                            .padding(16.dp)
                            .padding(end = 10.dp, bottom = 10.dp),
                        color = SimAnalyzerTheme.material.onSurface,
                        style = SimAnalyzerTheme.typography.bodySmall.copy(fontFamily = SimAnalyzerTheme.fonts.mono),
                        softWrap = false,
                    )
                    AppVerticalScrollbar(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(vertical = 6.dp),
                        adapter = AppScrollbarAdapter(
                            rememberScrollbarAdapter(vScroll),
                        ),
                    )
                    AppHorizontalScrollbar(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(end = 12.dp, start = 6.dp),
                        adapter = AppScrollbarAdapter(
                            rememberScrollbarAdapter(hScroll),
                        ),
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
