package com.analyzer.session.analysis.presentation.components.map.support

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

internal const val focusOverlaySettleDelayMs: Long = 320L
private const val trackMapOverlayMarkerReferenceSidePx: Float = 220f
private const val trackMapOverlayMarkerMinimumScale: Float = 0.62f
private const val trackMapSectorLabelBorderAlpha: Float = 0.44f
private const val trackMapFocusedCornerFillAlpha: Float = 0.26f
private const val trackMapCornerFillAlpha: Float = 0.14f
private const val trackMapFocusedCornerBorderAlpha: Float = 0.9f
private const val trackMapCornerBorderAlpha: Float = 0.52f
private const val trackMapFocusedIssueFillAlpha: Float = 0.22f
private const val trackMapIssueFillAlpha: Float = 0.14f
private const val trackMapFocusedIssueBorderAlpha: Float = 0.84f
private const val trackMapIssueBorderAlpha: Float = 0.46f

internal val trackCornerMarkerSizeDp = 30.dp
internal val trackCornerMarkerEdgeGapDp = 16.dp
internal val trackCornerMarkerSpacingDp = 36.dp
internal val trackCornerMarkerTangentRetryDp = 18.dp
internal val trackCornerMarkerOutwardRetryDp = 10.dp
internal val trackCornerMarkerViewportMarginDp = 24.dp

internal val sectorLabelChipSizeDp = 26.dp
internal val sectorLabelGapDp = 8.dp
internal val sectorLabelSpacingDp = 34.dp
internal val sectorLabelNudgeDp = 10.dp

internal fun resolveTrackMapOverlayMarkerScale(canvasSize: IntSize): Float {
    val minSide = minOf(canvasSize.width, canvasSize.height).toFloat()
    if (!minSide.isFinite() || minSide <= 0f) return 1f
    return (minSide / trackMapOverlayMarkerReferenceSidePx).coerceIn(
        trackMapOverlayMarkerMinimumScale,
        1f,
    )
}

internal fun resolveTrackMapSectorLabelBorderColor(accent: Color): Color =
    accent.copy(alpha = trackMapSectorLabelBorderAlpha)

internal fun resolveTrackMapCornerMarkerFillColor(accent: Color, focused: Boolean): Color =
    accent.copy(alpha = if (focused) trackMapFocusedCornerFillAlpha else trackMapCornerFillAlpha)

internal fun resolveTrackMapCornerMarkerBorderColor(accent: Color, focused: Boolean): Color =
    accent.copy(alpha = if (focused) trackMapFocusedCornerBorderAlpha else trackMapCornerBorderAlpha)

internal fun resolveTrackMapIssueMarkerFillColor(accent: Color, emphasized: Boolean): Color =
    accent.copy(alpha = if (emphasized) trackMapFocusedIssueFillAlpha else trackMapIssueFillAlpha)

internal fun resolveTrackMapIssueMarkerBorderColor(accent: Color, emphasized: Boolean): Color =
    accent.copy(alpha = if (emphasized) trackMapFocusedIssueBorderAlpha else trackMapIssueBorderAlpha)
