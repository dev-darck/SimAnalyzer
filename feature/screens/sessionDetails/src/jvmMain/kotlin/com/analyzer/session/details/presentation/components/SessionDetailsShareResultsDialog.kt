package com.analyzer.session.details.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.analyzer.session.details.presentation.model.SessionDetailShareDialogUi
import com.project.analyzer.feature.screens.sessionDetails.Res.Res
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_share_close
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_share_copy
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_share_copy_description
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_share_export
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_share_export_description
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_share_export_name
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_share_formats
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_share_preview
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_share_raw_files
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_share_raw_files_description
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.components.Button
import com.project.analyzer.ui.components.InfoDialog
import com.project.analyzer.ui.components.SimAnalyzerButtonSize
import com.project.analyzer.ui.components.SimAnalyzerButtonVariant
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SessionDetailsShareResultsDialog(
    shareDialog: SessionDetailShareDialogUi,
    onDismiss: () -> Unit,
    onCopySummary: () -> Unit,
    onExportReport: () -> Unit,
    onOpenRawFiles: () -> Unit,
) {
    if (!shareDialog.isVisible) return

    InfoDialog(
        title = shareDialog.title,
        message = shareDialog.supportingText,
        onDismissRequest = onDismiss,
        modifier = Modifier.width(640.dp),
    ) {
        SessionDetailsShareMetaRow(
            label = stringResource(Res.string.session_details_share_export_name),
            value = shareDialog.reportFileName,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(Res.string.session_details_share_formats),
            style = SimAnalyzerTheme.typography.labelLarge,
            color = SimAnalyzerTheme.material.onSurface,
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SessionDetailsShareActionCard(
                title = stringResource(Res.string.session_details_share_copy),
                description = stringResource(Res.string.session_details_share_copy_description),
                buttonText = "Copy",
                icon = Icons.Filled.ContentCopy,
                variant = SimAnalyzerButtonVariant.Primary,
                onClick = onCopySummary,
            )
            SessionDetailsShareActionCard(
                title = stringResource(Res.string.session_details_share_export),
                description = stringResource(Res.string.session_details_share_export_description),
                buttonText = "Save",
                icon = Icons.Filled.Description,
                variant = SimAnalyzerButtonVariant.Secondary,
                onClick = onExportReport,
            )
            SessionDetailsShareActionCard(
                title = stringResource(Res.string.session_details_share_raw_files),
                description = stringResource(Res.string.session_details_share_raw_files_description),
                buttonText = "Open",
                icon = Icons.Filled.FolderOpen,
                variant = SimAnalyzerButtonVariant.Outline,
                onClick = onOpenRawFiles,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(Res.string.session_details_share_preview),
            style = SimAnalyzerTheme.typography.labelLarge,
            color = SimAnalyzerTheme.material.onSurface,
        )

        SelectionContainer {
            Text(
                text = shareDialog.summaryText,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .clip(SimAnalyzerTheme.corners.panel)
                    .background(SimAnalyzerTheme.chrome.fillMuted)
                    .border(
                        width = 1.dp,
                        color = SimAnalyzerTheme.chrome.borderSubtle,
                        shape = SimAnalyzerTheme.corners.panel,
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                style = SimAnalyzerTheme.typography.bodyMedium,
                color = SimAnalyzerTheme.material.onSurface,
            )
        }

        Button(
            text = stringResource(Res.string.session_details_share_close),
            onClick = onDismiss,
            variant = SimAnalyzerButtonVariant.Outline,
            size = SimAnalyzerButtonSize.Compact,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

@Composable
private fun SessionDetailsShareActionCard(
    title: String,
    description: String,
    buttonText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    variant: SimAnalyzerButtonVariant,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.corners.panel)
            .background(SimAnalyzerTheme.chrome.fillMuted)
            .border(
                width = 1.dp,
                color = SimAnalyzerTheme.chrome.borderSubtle,
                shape = SimAnalyzerTheme.corners.panel,
            )
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = SimAnalyzerTheme.typography.titleSmall,
                color = SimAnalyzerTheme.material.onSurface,
            )
            Text(
                text = description,
                style = SimAnalyzerTheme.typography.bodySmall,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
            )
        }
        Button(
            text = buttonText,
            onClick = onClick,
            variant = variant,
            size = SimAnalyzerButtonSize.Compact,
            leadingIcon = icon,
        )
    }
}

@Composable
private fun SessionDetailsShareMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.corners.item)
            .background(SimAnalyzerTheme.chrome.fillMuted)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = SimAnalyzerTheme.typography.bodySmall,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(SimAnalyzerTheme.material.secondary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = SimAnalyzerTheme.material.secondary,
                )
            }
            Text(
                text = value,
                style = SimAnalyzerTheme.typography.labelMedium,
                color = SimAnalyzerTheme.material.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
