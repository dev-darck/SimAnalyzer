package com.project.analyzer.crash.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.analyzer.crash.di.CrashGraph
import com.project.analyzer.crash.domain.CrashReport
import com.project.analyzer.theme.SimAnalyzerTheme
import dev.zacsweers.metro.createGraph
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private const val DEFAULT_GITHUB_REPO = "dev-darck/SimAnalyzer"

@Composable
internal fun CrashScreen(
    crashReport: CrashReport,
    onExit: () -> Unit,
) {
    val graph = remember { createGraph<CrashGraph>() }
    val viewModel = graph.crashViewModel
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.actions) {
        viewModel.dispatch(CrashScreenUiEvent.Init(crashReport))
        viewModel.actions.onEach { action ->
            when (action) {
                is CrashScreenAction.ShowSnackbar -> snackbar.showSnackbar(action.message)
            }
        }.launchIn(this)
    }

    SimAnalyzerTheme {
        CrashScreenContent(
            report = state.report,
            snackbarHostState = snackbar,
            onEvent = viewModel::dispatch,
            onExit = onExit
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
) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("SimAnalyzer has crashed") },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp).size(28.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                actions = {
                    Button(onClick = onExit, modifier = Modifier.padding(end = 12.dp)) {
                        Text("Exit")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Overview") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Full Report") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Stacktrace") }
                )
            }
            when (selectedTab) {
                0 -> OverviewPanel(report, onEvent)
                1 -> CodePanel(text = report.fullText, modifier = Modifier.fillMaxSize())
                2 -> CodePanel(text = report.stacktrace, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun OverviewPanel(report: CrashReport, onEvent: (CrashScreenUiEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "A crash report was saved. Please report this issue to help us fix it.",
            style = MaterialTheme.typography.bodyLarge
        )
        CrashMetadata(report = report)
        CrashActions(onEvent = onEvent)
    }
}

@Composable
private fun CrashMetadata(report: CrashReport) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Crash Information", style = MaterialTheme.typography.titleMedium)
        MetadataEntry("Time:", report.time)
        MetadataEntry("Thread:", report.threadName)
        report.appVersion?.takeIf { it.isNotBlank() }?.let {
            MetadataEntry("App Version:", it)
        }
        MetadataEntry("OS:", report.os)
        MetadataEntry("Java:", report.java)
    }
}

@Composable
private fun MetadataEntry(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(100.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CrashActions(onEvent: (CrashScreenUiEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Actions", style = MaterialTheme.typography.titleMedium)
        ActionButton(
            text = "Report on GitHub",
            icon = Icons.Default.Share,
            onClick = { onEvent(CrashScreenUiEvent.ReportOnGitHub(DEFAULT_GITHUB_REPO)) }
        )
        ActionButton(
            text = "Copy report",
            icon = Icons.Default.Edit,
            onClick = { onEvent(CrashScreenUiEvent.CopyReport) }
        )
        ActionButton(
            text = "Copy stacktrace",
            icon = Icons.Default.Edit,
            onClick = { onEvent(CrashScreenUiEvent.CopyStacktrace) }
        )
        ActionButton(
            text = "Open logs folder",
            icon = Icons.Default.Info,
            onClick = { onEvent(CrashScreenUiEvent.OpenLogsFolder) }
        )
        ActionButton(
            text = "Open crash file",
            icon = Icons.Default.Info,
            onClick = { onEvent(CrashScreenUiEvent.OpenCrashFile) }
        )
    }
}

@Composable
private fun ActionButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(icon, contentDescription = text, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
private fun CodePanel(text: String, modifier: Modifier = Modifier) {
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        SelectionContainer {
            Text(
                text = text,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(vScroll)
                    .horizontalScroll(hScroll)
                    .padding(16.dp),
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                softWrap = false
            )
        }
    }
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
        java = "17.0.1"
    )
    SimAnalyzerTheme {
        CrashScreenContent(
            report = report,
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
            onExit = {}
        )
    }
}
