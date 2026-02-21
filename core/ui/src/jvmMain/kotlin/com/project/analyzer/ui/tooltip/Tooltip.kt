@file:OptIn(ExperimentalFoundationApi::class)

package com.project.analyzer.ui.tooltip

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement.CursorPoint
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.analyzer.theme.SimAnalyzerTheme
import kotlin.time.Duration.Companion.milliseconds

private val TOOLTIP_DELAY_MS = 300.milliseconds.inWholeMilliseconds.toInt()

@Composable
public fun Tooltip(
    tooltip: String,
    isShowTooltip: Boolean = true,
    cursor: CursorPoint = CursorPoint(offset = DpOffset(0.dp, 16.dp)),
    content: @Composable () -> Unit,
) {
    if (isShowTooltip) {
        TooltipArea(
            tooltip = { TooltipContent(tooltip) },
            delayMillis = TOOLTIP_DELAY_MS,
            tooltipPlacement = cursor,
        ) {
            content()
        }
    } else {
        content()
    }
}

@Composable
private fun TooltipContent(text: String) {
    Surface(
        modifier = Modifier.shadow(4.dp, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = SimAnalyzerTheme.material.inverseSurface,
        tonalElevation = 4.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = SimAnalyzerTheme.material.inverseOnSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 16.sp,
        )
    }
}
