package com.project.analyzer.ui.scrollbar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.TestTags.VisibilityName
import com.project.analyzer.ui.modifier.uiTestTag
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val SCROLLBAR_SCROLL_REVEAL_DURATION_MILLIS = 900L
private const val SCROLLBAR_OFFSET_DELTA_EPSILON = 0.5
private val ScrollbarThickness: Dp = 6.dp
private val ScrollbarRevealZoneSize: Dp = 16.dp
private val ScrollbarHoverPadding: Dp = (ScrollbarRevealZoneSize - ScrollbarThickness) / 2f

@Stable
public class AppScrollbarAdapter(adapter: ScrollbarAdapter) {

    internal val delegate: ScrollbarAdapter = adapter

    override fun equals(other: Any?): Boolean =
        other is AppScrollbarAdapter && delegate === other.delegate

    override fun hashCode(): Int = System.identityHashCode(delegate)
}

internal val AppScrollbarVisibilityKey: SemanticsPropertyKey<Boolean> =
    SemanticsPropertyKey(name = VisibilityName)

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
public fun AppVerticalScrollbar(
    adapter: AppScrollbarAdapter,
    modifier: Modifier = Modifier,
) {
    val uiState = rememberAppScrollbarUiState(
        adapter = adapter,
        animationLabel = "appVerticalScrollbarAlpha",
    )

    VerticalScrollbar(
        adapter = adapter.delegate,
        modifier = modifier
            .uiTestTag(TestTags.ScrollbarVertical)
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
public fun AppHorizontalScrollbar(
    adapter: AppScrollbarAdapter,
    modifier: Modifier = Modifier,
) {
    val uiState = rememberAppScrollbarUiState(
        adapter = adapter,
        animationLabel = "appHorizontalScrollbarAlpha",
    )

    HorizontalScrollbar(
        adapter = adapter.delegate,
        modifier = modifier
            .uiTestTag(TestTags.ScrollbarHorizontal)
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
private fun rememberAppScrollbarUiState(
    adapter: AppScrollbarAdapter,
    animationLabel: String,
): AppScrollbarUiState {
    val interactionSource = remember { MutableInteractionSource() }
    val isEdgeHovered by interactionSource.collectIsHoveredAsState()
    val isScrollRevealActive = rememberScrollRevealState(adapter)
    val canScroll by remember(adapter) {
        derivedStateOf {
            adapter.delegate.contentSize - adapter.delegate.viewportSize > SCROLLBAR_OFFSET_DELTA_EPSILON
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
        .semantics {
            this[AppScrollbarVisibilityKey] = alpha > 0f
        }

@Composable
private fun rememberScrollRevealState(
    adapter: AppScrollbarAdapter,
): Boolean {
    var isVisible by remember(adapter) { mutableStateOf(false) }

    LaunchedEffect(adapter) {
        var hideJob: Job? = null
        var hasPreviousOffset = false
        var previousOffset = 0.0

        snapshotFlow { adapter.delegate.scrollOffset }
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

@Preview(name = "Vertical Scrollbar")
@Composable
private fun AppVerticalScrollbarPreview() {
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        scrollState.scrollTo(160)
    }

    SimAnalyzerTheme {
        Surface(color = SimAnalyzerTheme.material.background) {
            Box(
                modifier = Modifier
                    .width(260.dp)
                    .height(220.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                        .padding(end = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    repeat(16) { index ->
                        Surface(
                            color = SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.32f),
                            shape = SimAnalyzerTheme.corners.item,
                        ) {
                            Text(
                                text = "Preview row ${index + 1}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                style = SimAnalyzerTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                AppVerticalScrollbar(
                    adapter = AppScrollbarAdapter(rememberScrollbarAdapter(scrollState)),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp),
                )
            }
        }
    }
}

@Preview(name = "Horizontal Scrollbar")
@Composable
private fun AppHorizontalScrollbarPreview() {
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        scrollState.scrollTo(180)
    }

    SimAnalyzerTheme {
        Surface(color = SimAnalyzerTheme.material.background) {
            Box(
                modifier = Modifier
                    .width(320.dp)
                    .height(140.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(16.dp)
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    repeat(10) { index ->
                        Surface(
                            modifier = Modifier.width(120.dp),
                            color = SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.32f),
                            shape = SimAnalyzerTheme.corners.item,
                        ) {
                            Text(
                                text = "Column ${index + 1}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 24.dp),
                                style = SimAnalyzerTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                AppHorizontalScrollbar(
                    adapter = AppScrollbarAdapter(rememberScrollbarAdapter(scrollState)),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = 6.dp, end = 12.dp),
                )
            }
        }
    }
}
