package com.project.analyzer.ui.scrollbar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.analyzer.theme.SimAnalyzerTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val SCROLLBAR_SCROLL_REVEAL_DURATION_MILLIS = 900L
private const val SCROLLBAR_OFFSET_DELTA_EPSILON = 0.5
private val ScrollbarThickness: Dp = 6.dp
private val ScrollbarRevealZoneSize: Dp = 16.dp
private val ScrollbarHoverPadding: Dp = (ScrollbarRevealZoneSize - ScrollbarThickness) / 2f

@Composable
public fun rememberAppScrollbarStyle(): ScrollbarStyle {
    val shape = SimAnalyzerTheme.corners.indicator
    val unhoverColor = SimAnalyzerTheme.chrome.borderStrong.copy(alpha = 0.65f)
    val hoverColor = SimAnalyzerTheme.chrome.borderInteractive.copy(alpha = 0.9f)

    return remember(shape, unhoverColor, hoverColor) {
        ScrollbarStyle(
            minimalHeight = 16.dp,
            thickness = ScrollbarThickness,
            shape = shape,
            hoverDurationMillis = 120,
            unhoverColor = unhoverColor,
            hoverColor = hoverColor,
        )
    }
}

@Composable
public fun AppVerticalScrollbar(adapter: ScrollbarAdapter, modifier: Modifier = Modifier) {
    val uiState = rememberAppScrollbarUiState(
        adapter = adapter,
        animationLabel = "appVerticalScrollbarAlpha",
    )

    VerticalScrollbar(
        adapter = adapter,
        modifier = modifier
            .requiredWidth(ScrollbarRevealZoneSize)
            .padding(horizontal = ScrollbarHoverPadding)
            .appScrollbarChrome(
                interactionSource = uiState.interactionSource,
                alpha = uiState.alpha,
            ),
        style = rememberAppScrollbarStyle(),
    )
}

@Composable
public fun AppHorizontalScrollbar(adapter: ScrollbarAdapter, modifier: Modifier = Modifier) {
    val uiState = rememberAppScrollbarUiState(
        adapter = adapter,
        animationLabel = "appHorizontalScrollbarAlpha",
    )

    HorizontalScrollbar(
        adapter = adapter,
        modifier = modifier
            .requiredHeight(ScrollbarRevealZoneSize)
            .padding(vertical = ScrollbarHoverPadding)
            .appScrollbarChrome(
                interactionSource = uiState.interactionSource,
                alpha = uiState.alpha,
            ),
        style = rememberAppScrollbarStyle(),
    )
}

@Composable
private fun rememberAppScrollbarUiState(adapter: ScrollbarAdapter, animationLabel: String): AppScrollbarUiState {
    val interactionSource = remember { MutableInteractionSource() }
    val isEdgeHovered by interactionSource.collectIsHoveredAsState()
    val isScrollRevealActive = rememberScrollRevealState(adapter)
    val canScroll by remember(adapter) {
        derivedStateOf {
            adapter.contentSize - adapter.viewportSize > SCROLLBAR_OFFSET_DELTA_EPSILON
        }
    }
    val isVisible = canScroll && (isEdgeHovered || isScrollRevealActive)
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = if (isVisible) 120 else 220),
        label = animationLabel,
    )

    return AppScrollbarUiState(
        interactionSource = interactionSource,
        alpha = alpha,
    )
}

private fun Modifier.appScrollbarChrome(interactionSource: MutableInteractionSource, alpha: Float): Modifier =
    hoverable(interactionSource)
        .clip(SimAnalyzerTheme.corners.indicator)
        .alpha(alpha)

@Composable
private fun rememberScrollRevealState(adapter: ScrollbarAdapter): Boolean {
    var isVisible by remember(adapter) { mutableStateOf(false) }

    LaunchedEffect(adapter) {
        var hideJob: Job? = null
        var hasPreviousOffset = false
        var previousOffset = 0.0

        snapshotFlow { adapter.scrollOffset }
            .collect { currentOffset ->
                if (!hasPreviousOffset) {
                    hasPreviousOffset = true
                    previousOffset = currentOffset
                    return@collect
                }

                if (abs(currentOffset - previousOffset) <= SCROLLBAR_OFFSET_DELTA_EPSILON) {
                    return@collect
                }

                previousOffset = currentOffset
                isVisible = true

                hideJob?.cancel()
                hideJob = launch {
                    delay(SCROLLBAR_SCROLL_REVEAL_DURATION_MILLIS)
                    isVisible = false
                }
            }
    }

    return isVisible
}

private data class AppScrollbarUiState(val interactionSource: MutableInteractionSource, val alpha: Float)
