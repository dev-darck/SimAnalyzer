package com.analyzer.session.analysis.presentation.components.layout.support

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.analyzer.session.analysis.presentation.components.layout.model.SessionAnalysisStudioLayoutMetrics

/**
 * Resolves pane sizes from breakpoints so the studio layout adapts without scattering width math.
 */
internal fun resolveSessionAnalysisStudioLayoutMetrics(
    maxWidth: Dp,
    maxHeight: Dp,
    scrollCollapseProgress: Float,
    heroManuallyCollapsed: Boolean,
): SessionAnalysisStudioLayoutMetrics {
    val useThreePaneLayout = maxWidth >= SessionAnalysisWideLayoutBreakpoint
    val collapseProgress = if (heroManuallyCollapsed) {
        1f
    } else {
        scrollCollapseProgress
    }
    val heroExpandedHeight = resolveHeroExpandedHeight(maxWidth = maxWidth, maxHeight = maxHeight)
    val heroCollapsedHeight = resolveHeroCollapsedHeight(maxWidth = maxWidth, maxHeight = maxHeight)
    val heroManualCollapsedHeight = resolveHeroManualCollapsedHeight(maxWidth = maxWidth, maxHeight = maxHeight)
    return SessionAnalysisStudioLayoutMetrics(
        useThreePaneLayout = useThreePaneLayout,
        collapseProgress = collapseProgress,
        heroHeight = if (heroManuallyCollapsed) {
            heroManualCollapsedHeight
        } else {
            lerp(heroExpandedHeight, heroCollapsedHeight, collapseProgress)
        },
        leftPaneWidth = if (maxWidth >= SessionAnalysisDesktopLayoutBreakpoint) 336.dp else 292.dp,
        rightPaneWidth = if (maxWidth >= SessionAnalysisDesktopLayoutBreakpoint) 404.dp else 356.dp,
    )
}

internal fun resolveHeroExpandedHeight(maxWidth: Dp, maxHeight: Dp): Dp = when {
    maxWidth >= SessionAnalysisDesktopLayoutBreakpoint -> (maxHeight * 0.42f).coerceIn(560.dp, 720.dp)
    maxWidth >= SessionAnalysisMediumLayoutBreakpoint -> (maxHeight * 0.38f).coerceIn(500.dp, 620.dp)
    else -> (maxHeight * 0.34f).coerceIn(460.dp, 560.dp)
}

internal fun resolveHeroCollapsedHeight(maxWidth: Dp, maxHeight: Dp): Dp = when {
    maxWidth >= SessionAnalysisDesktopLayoutBreakpoint -> (maxHeight * 0.23f).coerceIn(250.dp, 340.dp)
    maxWidth >= SessionAnalysisMediumLayoutBreakpoint -> (maxHeight * 0.22f).coerceIn(240.dp, 310.dp)
    else -> (maxHeight * 0.19f).coerceIn(220.dp, 280.dp)
}

internal fun resolveHeroManualCollapsedHeight(maxWidth: Dp, maxHeight: Dp): Dp = when {
    maxWidth >= SessionAnalysisDesktopLayoutBreakpoint -> (maxHeight * 0.16f).coerceIn(168.dp, 220.dp)
    maxWidth >= SessionAnalysisMediumLayoutBreakpoint -> (maxHeight * 0.15f).coerceIn(156.dp, 204.dp)
    else -> (maxHeight * 0.14f).coerceIn(148.dp, 188.dp)
}
