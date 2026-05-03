package com.analyzer.session.analysis.presentation.components

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
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_action_more
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_action_more_close
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_action_share
import com.project.analyzer.theme.SimAnalyzerTheme
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SessionAnalysisWorkspaceFabBar(actionsEnabled: Boolean, onShareResults: () -> Unit,) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val openActionsLabel = stringResource(Res.string.session_analysis_action_more)
    val closeActionsLabel = stringResource(Res.string.session_analysis_action_more_close)
    val mainFabRotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "session-analysis-speed-dial-fab-rotation",
    )

    LaunchedEffect(actionsEnabled) {
        if (!actionsEnabled) expanded = false
    }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(
                animationSpec = tween(durationMillis = 160, easing = LinearOutSlowInEasing),
            ) + slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            ) + scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            ),
            exit = fadeOut(animationSpec = tween(durationMillis = 120)) +
                slideOutVertically(
                    targetOffsetY = { it / 3 },
                    animationSpec = tween(durationMillis = 140),
                ) +
                scaleOut(
                    targetScale = 0.92f,
                    animationSpec = tween(durationMillis = 140),
                ),
            label = "session-analysis-speed-dial-item",
        ) {
            SessionAnalysisWorkspaceActionRow(
                label = stringResource(Res.string.session_analysis_action_share),
                contentDescription = stringResource(Res.string.session_analysis_action_share),
                enabled = actionsEnabled,
                onClick = {
                    expanded = false
                    onShareResults()
                },
            )
        }

        FloatingActionButton(
            onClick = {
                if (actionsEnabled) expanded = !expanded
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
                modifier = Modifier.rotate(mainFabRotation),
            )
        }
    }
}

@Composable
private fun SessionAnalysisWorkspaceActionRow(
    label: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 40.dp)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
            }
            .alpha(if (enabled) 1f else 0.54f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            style = SimAnalyzerTheme.typography.labelLarge,
            color = SimAnalyzerTheme.material.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 220.dp),
        )

        Surface(
            color = SimAnalyzerTheme.material.tertiaryContainer,
            contentColor = SimAnalyzerTheme.material.onTertiaryContainer,
            shape = CircleShape,
            shadowElevation = 8.dp,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = null,
                )
            }
        }
    }
}
