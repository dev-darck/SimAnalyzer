package com.project.analyzer.crash.presentation.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.analyzer.crash.domain.CrashReport
import com.project.analyzer.crash.presentation.CrashScreenUiEvent
import com.project.analyzer.feature.crash.Res.Res
import com.project.analyzer.feature.crash.Res.crash_action_copy_report
import com.project.analyzer.feature.crash.Res.crash_action_copy_stacktrace
import com.project.analyzer.feature.crash.Res.crash_action_open_crash_file
import com.project.analyzer.feature.crash.Res.crash_action_open_logs
import com.project.analyzer.feature.crash.Res.crash_action_report_github
import com.project.analyzer.feature.crash.Res.crash_actions_title
import com.project.analyzer.feature.crash.Res.crash_metadata_app_version
import com.project.analyzer.feature.crash.Res.crash_metadata_java
import com.project.analyzer.feature.crash.Res.crash_metadata_os
import com.project.analyzer.feature.crash.Res.crash_metadata_thread
import com.project.analyzer.feature.crash.Res.crash_metadata_time
import com.project.analyzer.feature.crash.Res.crash_metadata_title
import com.project.analyzer.feature.crash.Res.crash_overview_message
import com.project.analyzer.feature.crash.Res.crash_overview_title
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.scrollbar.AppScrollbarAdapter
import com.project.analyzer.ui.scrollbar.AppVerticalScrollbar
import org.jetbrains.compose.resources.stringResource

private const val DEFAULT_GITHUB_REPO = "dev-darck/SimAnalyzer"

@Composable
internal fun CrashOverviewPanel(report: CrashReport, onEvent: (CrashScreenUiEvent) -> Unit) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier.fillMaxSize(),
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
