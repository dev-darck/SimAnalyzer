package com.analyzer.session.details.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.analyzer.session.details.presentation.model.SessionDetailCompareLapUi
import com.project.analyzer.feature.screens.sessionDetails.Res.Res
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_action_add_session
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_action_more
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_action_more_close
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_action_share
import com.project.analyzer.theme.SimAnalyzerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SessionDetailsWorkspaceSpeedDial(
    selectedCompareLaps: ImmutableList<SessionDetailCompareLapUi>,
    compareStartEnabled: Boolean,
    actionsEnabled: Boolean,
    compareFabDescription: String,
    onStartCompare: () -> Unit,
    onOpenCompareSessionPicker: () -> Unit,
    onShareResults: () -> Unit,
) {
    var actionsExpanded by rememberSaveable { mutableStateOf(false) }
    val openActionsLabel = stringResource(Res.string.session_details_action_more)
    val closeActionsLabel = stringResource(Res.string.session_details_action_more_close)
    val mainFabRotation by animateFloatAsState(
        targetValue = if (actionsExpanded) 45f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "session-details-speed-dial-fab-rotation",
    )
    val actions = rememberWorkspaceActions(
        selectedCompareLaps = selectedCompareLaps,
        compareStartEnabled = compareStartEnabled,
        actionsEnabled = actionsEnabled,
        compareFabDescription = compareFabDescription,
        onStartCompare = onStartCompare,
        onOpenCompareSessionPicker = onOpenCompareSessionPicker,
        onShareResults = onShareResults,
    )

    LaunchedEffect(actionsEnabled) {
        if (!actionsEnabled) actionsExpanded = false
    }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SessionDetailsWorkspaceActionItems(
            actions = actions,
            expanded = actionsExpanded,
            onActionClick = { action ->
                actionsExpanded = false
                action.onClick()
            },
        )
        SessionDetailsWorkspaceMainFab(
            expanded = actionsExpanded,
            enabled = actionsEnabled,
            rotation = mainFabRotation,
            openActionsLabel = openActionsLabel,
            closeActionsLabel = closeActionsLabel,
            onClick = { actionsExpanded = !actionsExpanded },
        )
    }
}

@Composable
private fun rememberWorkspaceActions(
    selectedCompareLaps: ImmutableList<SessionDetailCompareLapUi>,
    compareStartEnabled: Boolean,
    actionsEnabled: Boolean,
    compareFabDescription: String,
    onStartCompare: () -> Unit,
    onOpenCompareSessionPicker: () -> Unit,
    onShareResults: () -> Unit,
): ImmutableList<SessionDetailsWorkspaceActionItem> = persistentListOf(
    SessionDetailsWorkspaceActionItem(
        label = stringResource(Res.string.session_details_action_share),
        contentDescription = stringResource(Res.string.session_details_action_share),
        icon = Icons.Filled.Share,
        enabled = actionsEnabled,
        containerColor = SimAnalyzerTheme.material.tertiaryContainer,
        contentColor = SimAnalyzerTheme.material.onTertiaryContainer,
        onClick = onShareResults,
    ),
    SessionDetailsWorkspaceActionItem(
        label = stringResource(Res.string.session_details_action_add_session),
        contentDescription = stringResource(Res.string.session_details_action_add_session),
        icon = Icons.Filled.Add,
        enabled = actionsEnabled,
        containerColor = SimAnalyzerTheme.material.secondaryContainer,
        contentColor = SimAnalyzerTheme.material.onSecondaryContainer,
        onClick = onOpenCompareSessionPicker,
    ),
    SessionDetailsWorkspaceActionItem(
        label = compareFabLabel(
            isCompareSelectionMode = false,
            selectedCompareLaps = selectedCompareLaps,
        ),
        contentDescription = compareFabDescription,
        icon = Icons.Filled.Timeline,
        enabled = compareStartEnabled,
        containerColor = SimAnalyzerTheme.material.primaryContainer,
        contentColor = SimAnalyzerTheme.material.onPrimaryContainer,
        onClick = onStartCompare,
    ),
)

@Composable
private fun SessionDetailsWorkspaceActionItems(
    actions: ImmutableList<SessionDetailsWorkspaceActionItem>,
    expanded: Boolean,
    onActionClick: (SessionDetailsWorkspaceActionItem) -> Unit,
) {
    actions.asReversed().forEachIndexed { index, action ->
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = 160,
                    delayMillis = index * 36,
                    easing = LinearOutSlowInEasing,
                ),
            ) + slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = tween(
                    durationMillis = 220,
                    delayMillis = index * 36,
                    easing = FastOutSlowInEasing,
                ),
            ) + scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(
                    durationMillis = 220,
                    delayMillis = index * 36,
                    easing = FastOutSlowInEasing,
                ),
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = 120),
            ) + slideOutVertically(
                targetOffsetY = { it / 3 },
                animationSpec = tween(durationMillis = 140),
            ) + scaleOut(
                targetScale = 0.92f,
                animationSpec = tween(durationMillis = 140),
            ),
            label = "session-details-speed-dial-item-$index",
        ) {
            SessionDetailsWorkspaceActionRow(
                action = action,
                onClick = { onActionClick(action) },
            )
        }
    }
}

@Composable
private fun SessionDetailsWorkspaceMainFab(
    expanded: Boolean,
    enabled: Boolean,
    rotation: Float,
    openActionsLabel: String,
    closeActionsLabel: String,
    onClick: () -> Unit,
) {
    FloatingActionButton(
        onClick = {
            if (enabled) onClick()
        },
        modifier = Modifier.semantics {
            contentDescription = if (expanded) closeActionsLabel else openActionsLabel
        },
        containerColor = if (expanded) {
            SimAnalyzerTheme.material.secondaryContainer
        } else {
            SimAnalyzerTheme.material.primaryContainer
        },
        contentColor = if (expanded) {
            SimAnalyzerTheme.material.onSecondaryContainer
        } else {
            SimAnalyzerTheme.material.onPrimaryContainer
        },
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.rotate(rotation),
        )
    }
}

@Composable
private fun SessionDetailsWorkspaceActionRow(
    action: SessionDetailsWorkspaceActionItem,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 40.dp)
            .clickable(
                enabled = action.enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {
                contentDescription = action.contentDescription
            }
            .alpha(if (action.enabled) 1f else 0.54f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = action.label,
            style = SimAnalyzerTheme.typography.labelLarge,
            color = SimAnalyzerTheme.material.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 220.dp),
        )

        Surface(
            color = action.containerColor,
            contentColor = action.contentColor,
            shape = CircleShape,
            shadowElevation = 8.dp,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                )
            }
        }
    }
}
