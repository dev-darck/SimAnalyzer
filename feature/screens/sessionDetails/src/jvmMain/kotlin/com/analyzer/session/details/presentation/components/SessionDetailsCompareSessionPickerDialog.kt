package com.analyzer.session.details.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.analyzer.session.details.presentation.model.SessionDetailCompareSessionCandidateUi
import com.analyzer.session.details.presentation.model.SessionDetailCompareSessionPickerUi
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.components.Button
import com.project.analyzer.ui.components.InfoDialog
import com.project.analyzer.ui.components.SimAnalyzerButtonSize
import com.project.analyzer.ui.components.SimAnalyzerButtonVariant
import java.io.File
import java.net.URI

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SessionDetailsCompareSessionPickerDialog(
    picker: SessionDetailCompareSessionPickerUi,
    onDismiss: () -> Unit,
    onSelectSession: (Long) -> Unit,
    onBrowseImportSession: () -> Unit,
    onImportSessionPath: (String) -> Unit,
) {
    if (!picker.isVisible) return

    var dropActive by remember { mutableStateOf(false) }
    val dropTarget = remember(onImportSessionPath) {
        sessionImportDropTarget(
            onDropStarted = { dropActive = true },
            onDropFinished = { dropActive = false },
            onImportSessionPath = onImportSessionPath,
        )
    }

    InfoDialog(
        title = picker.title,
        message = "",
        supportingText = picker.supportingText,
        dismissButtonText = "Close",
        onDismissRequest = onDismiss,
        onDismissClick = onDismiss,
        modifier = Modifier.widthIn(min = 460.dp, max = 620.dp),
    ) {
        SessionDetailsCompareImportZone(
            isImporting = picker.isImporting,
            isDropActive = dropActive,
            onBrowseImportSession = onBrowseImportSession,
            modifier = Modifier
                .fillMaxWidth()
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { event ->
                        event.dragData() is DragData.FilesList
                    },
                    target = dropTarget,
                ),
        )

        picker.statusMessage?.takeIf(String::isNotBlank)?.let { message ->
            Text(
                text = message,
                style = SimAnalyzerTheme.typography.bodySmall,
                color = SimAnalyzerTheme.material.onPrimaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SimAnalyzerTheme.corners.item)
                    .background(SimAnalyzerTheme.material.primaryContainer.copy(alpha = 0.92f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }

        Text(
            text = "Compatible sessions",
            style = SimAnalyzerTheme.typography.labelLarge,
            color = SimAnalyzerTheme.material.onSurface,
        )

        when {
            picker.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = SimAnalyzerTheme.material.primary)
                }
            }

            picker.candidates.isEmpty() -> {
                Text(
                    text = picker.emptyMessage ?: "No comparable sessions were found yet.",
                    style = SimAnalyzerTheme.typography.bodyLarge,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        items = picker.candidates,
                        key = SessionDetailCompareSessionCandidateUi::sessionId,
                    ) { candidate ->
                        CompareSessionCandidateRow(
                            candidate = candidate,
                            onCompare = { onSelectSession(candidate.sessionId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionDetailsCompareImportZone(
    isImporting: Boolean,
    isDropActive: Boolean,
    onBrowseImportSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(SimAnalyzerTheme.corners.panel)
            .border(
                width = if (isDropActive) 2.dp else 1.dp,
                color = if (isDropActive) {
                    SimAnalyzerTheme.material.primary
                } else {
                    SimAnalyzerTheme.chrome.borderSubtle
                },
                shape = SimAnalyzerTheme.corners.panel,
            ),
        color = if (isDropActive) {
            SimAnalyzerTheme.material.primaryContainer.copy(alpha = 0.42f)
        } else {
            SimAnalyzerTheme.chrome.fillMuted
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (isDropActive) {
                    "Drop the session folder to add it"
                } else {
                    "Drop a Sim Analyzer session folder here"
                },
                style = SimAnalyzerTheme.typography.titleSmall,
                color = SimAnalyzerTheme.material.onSurface,
            )
            Text(
                text = "Then press Compare on a compatible session below. Only the same game and track layout are accepted.",
                style = SimAnalyzerTheme.typography.bodySmall,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
            )
            Button(
                text = if (isImporting) "Adding session..." else "Choose Folder",
                onClick = onBrowseImportSession,
                enabled = !isImporting,
                variant = SimAnalyzerButtonVariant.Secondary,
                size = SimAnalyzerButtonSize.Compact,
                leadingIcon = Icons.Filled.FolderOpen,
            )
        }
    }
}

@Composable
private fun CompareSessionCandidateRow(candidate: SessionDetailCompareSessionCandidateUi, onCompare: () -> Unit,) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.corners.panel),
        color = SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.34f),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = SimAnalyzerTheme.chrome.borderSubtle,
                    shape = SimAnalyzerTheme.corners.panel,
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = candidate.carLabel,
                        style = SimAnalyzerTheme.typography.titleSmall,
                        color = SimAnalyzerTheme.material.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${candidate.sessionTypeLabel} • ${candidate.dateLabel} • ${candidate.timeLabel}",
                        style = SimAnalyzerTheme.typography.bodySmall,
                        color = SimAnalyzerTheme.material.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                candidate.recommendationLabel?.let { label ->
                    Text(
                        text = label,
                        style = SimAnalyzerTheme.typography.labelSmall,
                        color = SimAnalyzerTheme.material.onPrimaryContainer,
                        modifier = Modifier
                            .clip(SimAnalyzerTheme.corners.pill)
                            .background(SimAnalyzerTheme.material.primaryContainer)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Best ${candidate.bestLapLabel} • ${candidate.lapsLabel} laps",
                    style = SimAnalyzerTheme.typography.labelMedium,
                    color = SimAnalyzerTheme.material.onSurface,
                )
                Button(
                    text = "Compare",
                    onClick = onCompare,
                    variant = SimAnalyzerButtonVariant.Outline,
                    size = SimAnalyzerButtonSize.Compact,
                    leadingIcon = Icons.Filled.Timeline,
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun sessionImportDropTarget(
    onDropStarted: () -> Unit,
    onDropFinished: () -> Unit,
    onImportSessionPath: (String) -> Unit,
): DragAndDropTarget = object : DragAndDropTarget {

    override fun onStarted(event: DragAndDropEvent) {
        onDropStarted()
    }

    override fun onEntered(event: DragAndDropEvent) {
        onDropStarted()
    }

    override fun onExited(event: DragAndDropEvent) {
        onDropFinished()
    }

    override fun onEnded(event: DragAndDropEvent) {
        onDropFinished()
    }

    override fun onDrop(event: DragAndDropEvent): Boolean {
        val files = event.dragData() as? DragData.FilesList ?: return false
        val selectedPath = files.readFiles()
            .mapNotNull(::toDroppedSessionPath)
            .firstOrNull()
            ?: return false
        onImportSessionPath(selectedPath)
        onDropFinished()
        return true
    }
}

private fun toDroppedSessionPath(rawPath: String): String? = runCatching {
    if (rawPath.startsWith("file:", ignoreCase = true)) {
        File(URI(rawPath)).absolutePath
    } else {
        rawPath
    }
}.getOrNull()
