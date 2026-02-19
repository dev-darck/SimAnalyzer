package com.project.analyzer.crash.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.analyzer.crash.di.CrashGraph
import com.project.analyzer.crash.domain.CrashReport
import com.project.analyzer.theme.SimAnalyzerTheme
import dev.zacsweers.metro.createGraph
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private const val DEFAULT_GITHUB_REPO = "dev-darck/SimAnalyzer"

private data class CrashTabSpec(
    val title: String,
    val icon: ImageVector,
)

private val crashTabs = listOf(
    CrashTabSpec("Overview", Icons.Outlined.Warning),
    CrashTabSpec("Full Report", Icons.Default.Info),
    CrashTabSpec("Stacktrace", Icons.Default.Edit),
)

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
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("SimAnalyzer has crashed")
                        Text(
                            text = "A crash report is ready. You can review and share it.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                actions = {
                    Button(
                        onClick = onExit,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text("Exit")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                crashTabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(tab.title)
                            }
                        }
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                tonalElevation = 1.dp
            ) {
                when (selectedTab) {
                    0 -> OverviewPanel(report, onEvent)
                    1 -> CodePanel(text = report.fullText, title = "Full Report")
                    2 -> CodePanel(text = report.stacktrace, title = "Stacktrace")
                }
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
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.32f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "The app encountered an unexpected error.",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Crash report was saved locally. Please share it to help fix the issue faster.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        CrashMetadata(report = report)
        CrashActions(onEvent = onEvent)
    }
}

@Composable
private fun CrashMetadata(report: CrashReport) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Crash Metadata", style = MaterialTheme.typography.titleMedium)
            MetadataEntry("Time", report.time)
            MetadataEntry("Thread", report.threadName)
            report.appVersion?.takeIf { it.isNotBlank() }?.let {
                MetadataEntry("App Version", it)
            }
            MetadataEntry("OS", report.os)
            MetadataEntry("Java", report.java)
        }
    }
}

@Composable
private fun MetadataEntry(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CrashActions(onEvent: (CrashScreenUiEvent) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Actions", style = MaterialTheme.typography.titleMedium)
            CrashActionButton(
                text = "Report on GitHub",
                icon = Icons.Default.Share,
                emphasized = true,
                onClick = { onEvent(CrashScreenUiEvent.ReportOnGitHub(DEFAULT_GITHUB_REPO)) }
            )
            CrashActionButton(
                text = "Copy report",
                icon = Icons.Default.Edit,
                onClick = { onEvent(CrashScreenUiEvent.CopyReport) }
            )
            CrashActionButton(
                text = "Copy stacktrace",
                icon = Icons.Default.Edit,
                onClick = { onEvent(CrashScreenUiEvent.CopyStacktrace) }
            )
            CrashActionButton(
                text = "Open logs folder",
                icon = Icons.Default.Info,
                onClick = { onEvent(CrashScreenUiEvent.OpenLogsFolder) }
            )
            CrashActionButton(
                text = "Open crash file",
                icon = Icons.Default.Info,
                onClick = { onEvent(CrashScreenUiEvent.OpenCrashFile) }
            )
        }
    }
}

@Composable
private fun CrashActionButton(
    text: String,
    icon: ImageVector,
    emphasized: Boolean = false,
    onClick: () -> Unit
) {
    if (emphasized) {
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = text, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = text, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text)
        }
    }
}

@Composable
private fun CodePanel(
    text: String,
    title: String,
) {
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
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$title • $lineCount lines",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
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
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    softWrap = false
                )
            }
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
