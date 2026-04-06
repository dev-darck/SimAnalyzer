package com.project.analyzer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.project.analyzer.theme.SimAnalyzerTheme

@Composable
public fun InfoDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    message: String = "",
    checkboxText: String = "",
    checked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
    supportingText: String = "",
    supportingIcon: ImageVector? = Icons.Filled.Info,
    dismissButtonText: String = "",
    confirmButtonText: String = "",
    onDismissClick: (() -> Unit)? = null,
    onConfirmClick: (() -> Unit)? = null,
    dismissButtonVariant: SimAnalyzerButtonVariant = SimAnalyzerButtonVariant.Secondary,
    confirmButtonVariant: SimAnalyzerButtonVariant = SimAnalyzerButtonVariant.Primary,
    properties: DialogProperties = DialogProperties(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        usePlatformDefaultWidth = false,
    ),
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val containerColor = SimAnalyzerTheme.material.surface
    val borderColor = SimAnalyzerTheme.chrome.borderStrong

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .widthIn(max = 780.dp)
                .clip(SimAnalyzerTheme.corners.panel)
                .background(containerColor)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = SimAnalyzerTheme.corners.panel,
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = title,
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.headlineSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            if (message.isNotBlank()) {
                Text(
                    text = message,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    style = SimAnalyzerTheme.typography.bodyLarge,
                )
            }

            content()

            if (checkboxText.isNotBlank()) {
                InfoDialogCheckboxRow(
                    text = checkboxText,
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                )
            }

            if (supportingText.isNotBlank()) {
                InfoDialogSupportingRow(
                    text = supportingText,
                    icon = supportingIcon,
                )
            }

            val hasDismissButton = dismissButtonText.isNotBlank() && onDismissClick != null
            val hasConfirmButton = confirmButtonText.isNotBlank() && onConfirmClick != null

            if (hasDismissButton || hasConfirmButton) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (hasDismissButton) {
                        Button(
                            text = dismissButtonText,
                            onClick = { onDismissClick.invoke() },
                            modifier = Modifier
                                .weight(1f)
                                .height(62.dp),
                            variant = dismissButtonVariant,
                        )
                    }
                    if (hasConfirmButton) {
                        Button(
                            text = confirmButtonText,
                            onClick = { onConfirmClick.invoke() },
                            modifier = Modifier
                                .weight(1f)
                                .height(62.dp),
                            variant = confirmButtonVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoDialogCheckboxRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.corners.item)
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(SimAnalyzerTheme.corners.control)
                .background(
                    if (checked) SimAnalyzerTheme.material.primary.copy(alpha = 0.16f) else Color.Transparent,
                )
                .border(
                    width = 1.5.dp,
                    color = if (checked) {
                        SimAnalyzerTheme.material.primary
                    } else {
                        SimAnalyzerTheme.chrome.borderInteractive
                    },
                    shape = SimAnalyzerTheme.corners.control,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = SimAnalyzerTheme.material.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Text(
            text = text,
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun InfoDialogSupportingRow(
    text: String,
    icon: ImageVector?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(SimAnalyzerTheme.corners.badge)
                .background(SimAnalyzerTheme.chrome.fillMuted),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SimAnalyzerTheme.material.onSurfaceVariant.copy(alpha = 0.78f),
                    modifier = Modifier.size(19.dp),
                )
            }
        }

        Text(
            text = text,
            color = SimAnalyzerTheme.material.onSurfaceVariant.copy(alpha = 0.78f),
            style = SimAnalyzerTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview
@Composable
private fun InfoDialogPreview() {
    SimAnalyzerTheme {
        InfoDialog(
            title = "When pressing the close button:",
            checkboxText = "Remember my decision",
            checked = true,
            onCheckedChange = {},
            supportingText = "Use \"Minimize to tray\" so HUD overlays keep updating while" +
                " the app is in the background. You can change this later in Settings.",
            dismissButtonText = "Exit",
            confirmButtonText = "Minimize to tray",
            onDismissRequest = {},
            onDismissClick = {},
            onConfirmClick = {},
            dismissButtonVariant = SimAnalyzerButtonVariant.Secondary,
        ) {
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}
