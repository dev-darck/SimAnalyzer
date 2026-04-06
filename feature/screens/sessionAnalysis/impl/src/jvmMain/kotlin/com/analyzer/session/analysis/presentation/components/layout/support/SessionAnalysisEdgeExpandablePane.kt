@file:OptIn(ExperimentalComposeUiApi::class)

package com.analyzer.session.analysis.presentation.components.layout.support

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex

private val sessionAnalysisPaneHoverZoneWidth = 16.dp
private val sessionAnalysisPaneHoverExpansionWidth = 52.dp
private const val sessionAnalysisPaneExpandAnimationDurationMs = 140

internal enum class SessionAnalysisPaneHoverEdge {
    Leading,
    Trailing,
}

@Composable
internal fun SessionAnalysisEdgeExpandablePane(
    baseWidth: Dp,
    edge: SessionAnalysisPaneHoverEdge,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    val density = LocalDensity.current
    val hoverZoneWidthPx = with(density) { sessionAnalysisPaneHoverZoneWidth.toPx() }
    var expanded by remember { mutableStateOf(false) }
    var interactiveWidthPx by remember { mutableFloatStateOf(0f) }
    val animatedWidth by animateDpAsState(
        targetValue = if (expanded) {
            baseWidth + sessionAnalysisPaneHoverExpansionWidth
        } else {
            baseWidth
        },
        animationSpec = tween(durationMillis = sessionAnalysisPaneExpandAnimationDurationMs),
        label = "sessionAnalysisPaneWidth",
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(animatedWidth)
            .onSizeChanged { interactiveWidthPx = it.toSize().width }
            .onPointerEvent(PointerEventType.Move) { event ->
                val pointerX = event.changes.firstOrNull()?.position?.x ?: return@onPointerEvent
                if (interactiveWidthPx <= 0f) {
                    return@onPointerEvent
                }
                val shouldExpand = when (edge) {
                    SessionAnalysisPaneHoverEdge.Leading -> pointerX <= hoverZoneWidthPx
                    SessionAnalysisPaneHoverEdge.Trailing -> pointerX >= interactiveWidthPx - hoverZoneWidthPx
                }
                if (shouldExpand) {
                    expanded = true
                }
            }
            .onPointerEvent(PointerEventType.Exit) {
                expanded = false
            }
            .zIndex(if (expanded) 2f else 0f),
    ) {
        content(Modifier.fillMaxSize())
    }
}
