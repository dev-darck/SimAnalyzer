package com.analyzer.session.details.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.analyzer.session.details.presentation.model.SessionDetailCompareLapUi
import com.project.analyzer.feature.screens.sessionDetails.Res.Res
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_action_compare
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_action_compare_cancel
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_action_compare_cancel_short
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_action_compare_confirm
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_action_compare_confirm_short
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_action_compare_progress
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_action_compare_ready
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_action_compare_select
import com.project.analyzer.theme.SimAnalyzerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SessionDetailsCompareFabBar(
    isCompareSelectionMode: Boolean,
    selectedCompareLaps: ImmutableList<SessionDetailCompareLapUi>,
    compareConfirmEnabled: Boolean,
    modifier: Modifier = Modifier,
    onStartCompare: () -> Unit = {},
    onCancelCompare: () -> Unit = {},
    onConfirmCompare: () -> Unit = {},
) {
    val compareFabDescription = stringResource(Res.string.session_details_action_compare)
    val cancelDescription = stringResource(Res.string.session_details_action_compare_cancel)
    val cancelLabel = stringResource(Res.string.session_details_action_compare_cancel_short)
    val confirmDescription = stringResource(Res.string.session_details_action_compare_confirm)
    val confirmLabel = stringResource(Res.string.session_details_action_compare_confirm_short)

    AnimatedContent(
        targetState = isCompareSelectionMode,
        modifier = modifier,
        transitionSpec = {
            (
                fadeIn() + scaleIn(initialScale = 0.96f) + slideInHorizontally(initialOffsetX = { it / 5 })
                ) togetherWith (
                fadeOut() + scaleOut(targetScale = 0.96f) + slideOutHorizontally(targetOffsetX = { it / 5 })
                )
        },
        label = "session-details-compare-fab",
    ) { compareSelectionMode ->
        if (compareSelectionMode) {
            SessionDetailsCompareSelectionDock(
                compareConfirmEnabled = compareConfirmEnabled,
                title = compareFabDescription,
                statusLabel = compareFabLabel(
                    isCompareSelectionMode = true,
                    selectedCompareLaps = selectedCompareLaps,
                ),
                cancelLabel = cancelLabel,
                confirmLabel = confirmLabel,
                cancelDescription = cancelDescription,
                confirmDescription = confirmDescription,
                onCancelCompare = onCancelCompare,
                onConfirmCompare = onConfirmCompare,
            )
        } else {
            ExtendedFloatingActionButton(
                text = {
                    Text(
                        text = compareFabLabel(
                            isCompareSelectionMode = false,
                            selectedCompareLaps = selectedCompareLaps,
                        ),
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Timeline,
                        contentDescription = null,
                    )
                },
                onClick = onStartCompare,
                modifier = Modifier.semantics {
                    contentDescription = compareFabDescription
                },
                expanded = true,
                containerColor = SimAnalyzerTheme.material.primaryContainer,
                contentColor = SimAnalyzerTheme.material.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun SessionDetailsCompareSelectionDock(
    compareConfirmEnabled: Boolean,
    title: String,
    statusLabel: String,
    cancelLabel: String,
    confirmLabel: String,
    cancelDescription: String,
    confirmDescription: String,
    onCancelCompare: () -> Unit,
    onConfirmCompare: () -> Unit,
) {
    Surface(
        shape = SimAnalyzerTheme.corners.pill,
        color = SimAnalyzerTheme.material.surface,
        border = BorderStroke(1.dp, SimAnalyzerTheme.chrome.borderSubtle),
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 64.dp)
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SessionDetailsCompareLead(
                title = title,
                statusLabel = statusLabel,
                compareConfirmEnabled = compareConfirmEnabled,
            )
            Spacer(
                modifier = Modifier
                    .width(1.dp)
                    .height(30.dp)
                    .background(SimAnalyzerTheme.chrome.borderSubtle),
            )
            SessionDetailsCompareActionChip(
                icon = Icons.Filled.Close,
                contentDescription = cancelDescription,
                label = cancelLabel,
                tint = SimAnalyzerTheme.extended.red,
                backgroundColor = SimAnalyzerTheme.extended.red.copy(alpha = 0.12f),
                borderColor = SimAnalyzerTheme.extended.red.copy(alpha = 0.24f),
                onClick = onCancelCompare,
            )
            SessionDetailsCompareActionChip(
                icon = Icons.Filled.Check,
                contentDescription = confirmDescription,
                label = confirmLabel,
                tint = if (compareConfirmEnabled) {
                    SimAnalyzerTheme.extended.lightGreen
                } else {
                    SimAnalyzerTheme.material.onSurfaceVariant.copy(alpha = 0.72f)
                },
                backgroundColor = if (compareConfirmEnabled) {
                    SimAnalyzerTheme.extended.lightGreen.copy(alpha = 0.14f)
                } else {
                    SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.8f)
                },
                borderColor = if (compareConfirmEnabled) {
                    SimAnalyzerTheme.extended.lightGreen.copy(alpha = 0.28f)
                } else {
                    SimAnalyzerTheme.chrome.borderSubtle
                },
                enabled = compareConfirmEnabled,
                onClick = onConfirmCompare,
            )
        }
    }
}

@Composable
private fun SessionDetailsCompareLead(
    title: String,
    statusLabel: String,
    compareConfirmEnabled: Boolean,
) {
    val iconContainerColor = when {
        compareConfirmEnabled -> SimAnalyzerTheme.material.primaryContainer
        else -> SimAnalyzerTheme.material.secondaryContainer
    }

    Row(
        modifier = Modifier.padding(start = 2.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconContainerColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Timeline,
                contentDescription = null,
                tint = if (compareConfirmEnabled) {
                    SimAnalyzerTheme.material.onPrimaryContainer
                } else {
                    SimAnalyzerTheme.material.onSecondaryContainer
                },
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = SimAnalyzerTheme.typography.labelSmall,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = statusLabel,
                style = SimAnalyzerTheme.typography.titleSmall,
                color = SimAnalyzerTheme.material.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SessionDetailsCompareActionChip(
    icon: ImageVector,
    contentDescription: String,
    label: String,
    tint: Color,
    backgroundColor: Color,
    borderColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(SimAnalyzerTheme.corners.pill)
            .background(backgroundColor)
            .then(
                Modifier.border(
                    width = 1.dp,
                    color = borderColor,
                    shape = SimAnalyzerTheme.corners.pill,
                ),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            color = tint,
            style = SimAnalyzerTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun compareFabLabel(
    isCompareSelectionMode: Boolean,
    selectedCompareLaps: ImmutableList<SessionDetailCompareLapUi>,
): String = when {
    !isCompareSelectionMode -> stringResource(Res.string.session_details_action_compare)
    selectedCompareLaps.size >= 2 -> stringResource(Res.string.session_details_action_compare_ready)
    selectedCompareLaps.isNotEmpty() -> stringResource(
        Res.string.session_details_action_compare_progress,
        selectedCompareLaps.size,
    )

    else -> stringResource(Res.string.session_details_action_compare_select)
}

@Preview
@Composable
private fun SessionDetailsCompareFabBarPreview() {
    SimAnalyzerTheme {
        SessionDetailsCompareFabBar(
            isCompareSelectionMode = true,
            selectedCompareLaps = persistentListOf(
                SessionDetailCompareLapUi(
                    segmentId = 1L,
                    lapNumber = 3,
                    lapLabel = "3",
                    sessionTypeLabel = "Race",
                    totalTimeMs = 95_212,
                ),
            ),
            compareConfirmEnabled = false,
        )
    }
}

@Preview
@Composable
private fun SessionDetailsCompareFabBarIdlePreview() {
    SimAnalyzerTheme {
        SessionDetailsCompareFabBar(
            isCompareSelectionMode = false,
            selectedCompareLaps = persistentListOf(),
            compareConfirmEnabled = false,
        )
    }
}
