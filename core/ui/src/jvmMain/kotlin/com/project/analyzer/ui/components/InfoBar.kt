package com.project.analyzer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.analyzer.core.ui.Res.Res
import com.project.analyzer.core.ui.Res.info_bar_action_details
import com.project.analyzer.core.ui.Res.info_bar_action_retry
import com.project.analyzer.core.ui.Res.info_bar_dismiss
import com.project.analyzer.core.ui.Res.info_bar_preview_error_message
import com.project.analyzer.core.ui.Res.info_bar_preview_error_title
import com.project.analyzer.core.ui.Res.info_bar_preview_info_message
import com.project.analyzer.core.ui.Res.info_bar_preview_info_title
import com.project.analyzer.core.ui.Res.info_bar_preview_success_message
import com.project.analyzer.core.ui.Res.info_bar_preview_success_title
import com.project.analyzer.core.ui.Res.info_bar_preview_warning_message
import com.project.analyzer.core.ui.Res.info_bar_preview_warning_title
import com.project.analyzer.theme.SimAnalyzerTheme
import org.jetbrains.compose.resources.stringResource

public enum class InfoBarSeverity {
    Info,
    Success,
    Warning,
    Error,
}

@Composable
public fun InfoBar(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    severity: InfoBarSeverity = InfoBarSeverity.Info,
    compact: Boolean = false,
    action: (@Composable RowScope.() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    val palette = infoBarPalette(severity)
    val containerShape = if (compact) SimAnalyzerTheme.corners.item else SimAnalyzerTheme.corners.field
    val iconShape = if (compact) SimAnalyzerTheme.corners.control else SimAnalyzerTheme.corners.item
    val horizontalPadding = if (compact) 11.dp else 12.dp
    val verticalPadding = if (compact) 9.dp else 10.dp
    val rowSpacing = if (compact) 10.dp else 11.dp
    val accentHeight = if (compact) 36.dp else 40.dp
    val iconSize = if (compact) 28.dp else 30.dp
    val iconGlyphSize = if (compact) 15.dp else 16.dp
    val contentSpacing = if (compact) 6.dp else 8.dp
    val titleMessageSpacing = 2.dp
    val actionIndent = if (compact) 38.dp else 41.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(containerShape)
            .background(palette.containerColor)
            .border(
                width = 1.dp,
                color = palette.borderColor,
                shape = containerShape,
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        horizontalArrangement = Arrangement.spacedBy(rowSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(accentHeight)
                .clip(SimAnalyzerTheme.corners.pill)
                .background(palette.accentColor),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(rowSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .clip(iconShape)
                        .background(palette.iconContainerColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = palette.icon,
                        contentDescription = null,
                        tint = palette.accentColor,
                        modifier = Modifier.size(iconGlyphSize),
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(titleMessageSpacing),
                ) {
                    Text(
                        text = title,
                        color = SimAnalyzerTheme.material.onSurface,
                        style = SimAnalyzerTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = message,
                        color = SimAnalyzerTheme.material.onSurfaceVariant,
                        style = SimAnalyzerTheme.typography.bodySmall,
                        maxLines = if (compact) 2 else Int.MAX_VALUE,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (onDismiss != null) {
                    InfoBarDismissButton(
                        accentColor = palette.accentColor,
                        compact = compact,
                        onClick = onDismiss,
                    )
                }
            }

            if (action != null) {
                Row(
                    modifier = Modifier.padding(start = actionIndent),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = action,
                )
            }
        }
    }
}

private data class InfoBarPalette(
    val containerColor: Color,
    val borderColor: Color,
    val accentColor: Color,
    val iconContainerColor: Color,
    val icon: ImageVector,
)

@Composable
private fun InfoBarDismissButton(
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonSize = if (compact) 26.dp else 28.dp
    val iconSize = if (compact) 12.dp else 14.dp
    val shape = SimAnalyzerTheme.corners.control
    val backgroundColor = when {
        isPressed -> accentColor.copy(alpha = 0.18f)
        isHovered -> accentColor.copy(alpha = 0.10f)
        else -> SimAnalyzerTheme.chrome.fillMuted
    }
    val borderColor = when {
        isPressed -> accentColor.copy(alpha = 0.42f)
        isHovered -> accentColor.copy(alpha = 0.28f)
        else -> SimAnalyzerTheme.chrome.borderSubtle
    }
    val iconColor = when {
        isPressed || isHovered -> accentColor
        else -> SimAnalyzerTheme.material.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .size(buttonSize)
            .clip(shape)
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape,
            )
            .hoverable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(Res.string.info_bar_dismiss),
            tint = iconColor,
            modifier = Modifier.size(iconSize),
        )
    }
}

public data class InfoBarSnackbarVisuals(
    val title: String,
    override val message: String,
    val severity: InfoBarSeverity = InfoBarSeverity.Info,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = true,
    override val duration: SnackbarDuration = SnackbarDuration.Long,
) : SnackbarVisuals

@Composable
public fun InfoBarSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
    ) { data ->
        val visuals = data.visuals as? InfoBarSnackbarVisuals
        val severity = visuals?.severity ?: InfoBarSeverity.Info
        val palette = infoBarPalette(severity)
        InfoBar(
            title = visuals?.title.orEmpty(),
            message = data.visuals.message,
            severity = severity,
            compact = true,
            action = data.visuals.actionLabel?.let { actionLabel ->
                {
                    Text(
                        text = actionLabel,
                        color = palette.accentColor,
                        style = SimAnalyzerTheme.typography.labelMedium,
                        modifier = Modifier.clickable(onClick = data::performAction),
                    )
                }
            },
            onDismiss = if (data.visuals.withDismissAction) {
                { data.dismiss() }
            } else {
                null
            },
        )
    }
}

@Composable
private fun infoBarPalette(severity: InfoBarSeverity): InfoBarPalette = when (severity) {
    InfoBarSeverity.Info -> InfoBarPalette(
        containerColor = lerp(
            start = SimAnalyzerTheme.material.surface,
            stop = SimAnalyzerTheme.material.primaryContainer,
            fraction = 0.72f,
        ),
        borderColor = SimAnalyzerTheme.material.primary.copy(alpha = 0.32f),
        accentColor = SimAnalyzerTheme.extended.cyan,
        iconContainerColor = lerp(
            start = SimAnalyzerTheme.material.surfaceVariant,
            stop = SimAnalyzerTheme.material.primary,
            fraction = 0.18f,
        ),
        icon = Icons.Filled.Info,
    )

    InfoBarSeverity.Success -> InfoBarPalette(
        containerColor = lerp(
            start = SimAnalyzerTheme.material.surface,
            stop = SimAnalyzerTheme.extended.teal,
            fraction = 0.16f,
        ),
        borderColor = SimAnalyzerTheme.extended.teal.copy(alpha = 0.42f),
        accentColor = SimAnalyzerTheme.extended.teal,
        iconContainerColor = lerp(
            start = SimAnalyzerTheme.material.surfaceVariant,
            stop = SimAnalyzerTheme.extended.teal,
            fraction = 0.20f,
        ),
        icon = Icons.Filled.CheckCircle,
    )

    InfoBarSeverity.Warning -> InfoBarPalette(
        containerColor = lerp(
            start = SimAnalyzerTheme.material.surface,
            stop = SimAnalyzerTheme.extended.amber,
            fraction = 0.13f,
        ),
        borderColor = SimAnalyzerTheme.extended.amber.copy(alpha = 0.40f),
        accentColor = SimAnalyzerTheme.extended.amber,
        iconContainerColor = lerp(
            start = SimAnalyzerTheme.material.surfaceVariant,
            stop = SimAnalyzerTheme.extended.amber,
            fraction = 0.18f,
        ),
        icon = Icons.Filled.WarningAmber,
    )

    InfoBarSeverity.Error -> InfoBarPalette(
        containerColor = lerp(
            start = SimAnalyzerTheme.material.surface,
            stop = SimAnalyzerTheme.extended.red,
            fraction = 0.14f,
        ),
        borderColor = SimAnalyzerTheme.extended.red.copy(alpha = 0.40f),
        accentColor = SimAnalyzerTheme.extended.red,
        iconContainerColor = lerp(
            start = SimAnalyzerTheme.material.surfaceVariant,
            stop = SimAnalyzerTheme.extended.red,
            fraction = 0.18f,
        ),
        icon = Icons.Filled.ErrorOutline,
    )
}

@Preview
@Composable
private fun InfoBarPreview() {
    SimAnalyzerTheme {
        Column(
            modifier = Modifier
                .background(SimAnalyzerTheme.material.background)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InfoBar(
                title = stringResource(Res.string.info_bar_preview_info_title),
                message = stringResource(Res.string.info_bar_preview_info_message),
                severity = InfoBarSeverity.Info,
                onDismiss = {},
            )
            InfoBar(
                title = stringResource(Res.string.info_bar_preview_success_title),
                message = stringResource(Res.string.info_bar_preview_success_message),
                severity = InfoBarSeverity.Success,
            )
            InfoBar(
                title = stringResource(Res.string.info_bar_preview_warning_title),
                message = stringResource(Res.string.info_bar_preview_warning_message),
                severity = InfoBarSeverity.Warning,
                action = {
                    Text(
                        text = stringResource(Res.string.info_bar_action_details),
                        color = SimAnalyzerTheme.extended.amber,
                        style = SimAnalyzerTheme.typography.labelLarge,
                    )
                },
            )
            InfoBar(
                title = stringResource(Res.string.info_bar_preview_error_title),
                message = stringResource(Res.string.info_bar_preview_error_message),
                severity = InfoBarSeverity.Error,
                action = {
                    Text(
                        text = stringResource(Res.string.info_bar_action_retry),
                        color = SimAnalyzerTheme.extended.red,
                        style = SimAnalyzerTheme.typography.labelLarge,
                    )
                },
            )
        }
    }
}
