package com.analyzer.session.analysis.presentation.components.map.resolver

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import com.analyzer.session.analysis.presentation.components.map.model.TrackDiagnosticPalette
import com.analyzer.session.analysis.presentation.components.map.model.TrackIssueMarker
import com.analyzer.session.analysis.presentation.components.map.model.TrackIssueMarkerPlacement
import com.analyzer.session.analysis.presentation.components.map.support.circularFractionDistance
import com.analyzer.session.analysis.presentation.components.map.support.sampleDirectionAtFraction
import com.analyzer.session.analysis.presentation.components.map.support.samplePointAtFraction
import com.analyzer.session.analysis.presentation.model.CornerScoreUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Resolves diagnostic markers and overlays so map annotations stay aligned with the active track projection.
 */
private const val focusIssueWindowFraction: Float = 0.035f
private const val trackCornerOverlayMinimumSpacingFraction: Float = 0.038f

internal fun resolveFocusedCorner(corners: List<CornerScoreUi>, activeTrackPosition: Float?): CornerScoreUi? {
    if (corners.isEmpty()) return null
    return if (activeTrackPosition != null) {
        corners.minByOrNull { corner ->
            circularFractionDistance(corner.trackPosition, activeTrackPosition)
        }
    } else {
        corners.minWithOrNull(
            compareBy(CornerScoreUi::score)
                .thenByDescending(CornerScoreUi::timeVsReferenceMs),
        )
    }
}

internal fun resolveFocusedIssues(
    issues: List<SessionAnalysisHighlightUi>,
    focusCorner: CornerScoreUi?,
    activeTrackPosition: Float?,
): List<SessionAnalysisHighlightUi> {
    val anchorTrackPosition = activeTrackPosition ?: focusCorner?.trackPosition
    return issues
        .filter { issue ->
            when {
                issue.isAttachedToFocusCorner(focusCorner) -> true

                anchorTrackPosition != null -> issue.trackPosition?.let { trackPosition ->
                    circularFractionDistance(trackPosition, anchorTrackPosition) <= focusIssueWindowFraction
                } == true

                else -> false
            }
        }
        .sortedWith(
            compareByDescending(SessionAnalysisHighlightUi::priority)
                .thenByDescending { issue -> issue.deltaMs ?: 0 },
        )
        .distinctBy(SessionAnalysisHighlightUi::stableOverlayKey)
        .take(3)
}

@Suppress("LongParameterList")
internal fun resolveIssueMarkers(
    anchorLine: List<SessionAnalysisFractionPointUi>,
    issues: List<SessionAnalysisHighlightUi>,
    focusCorner: CornerScoreUi?,
    canvasSize: IntSize,
    palette: TrackDiagnosticPalette,
    occupiedPositions: List<Offset>,
    placement: TrackIssueMarkerPlacement,
): List<TrackIssueMarker> {
    return issues
        .distinctBy(SessionAnalysisHighlightUi::stableOverlayKey)
        .take(1)
        .mapIndexedNotNull { index, issue ->
            val trackPosition = issue.trackPosition ?: return@mapIndexedNotNull null
            val anchorPoint = anchorLine.samplePointAtFraction(trackPosition) ?: return@mapIndexedNotNull null
            val direction = anchorLine.sampleDirectionAtFraction(trackPosition)
            val baseDistance = placement.baseDistancePx +
                if (issue.isAttachedToFocusCorner(focusCorner)) {
                    placement.focusDistanceBonusPx
                } else {
                    0f
                } +
                index * placement.stackStepPx
            val offset = overlayOffset(
                direction = direction,
                distance = baseDistance,
                tangentShiftPx = placement.tangentShiftPx,
                markerIndex = index + 4,
            )
            var position = Offset(
                x = anchorPoint.x + offset.x,
                y = anchorPoint.y + offset.y,
            )
            if (occupiedPositions.any { occupied -> occupied.distanceTo(position) < placement.spacingPx }) {
                val nudgedOffset = overlayOffset(
                    direction = direction,
                    distance = baseDistance + placement.outwardRetryPx,
                    tangentShiftPx = placement.tangentShiftPx * 1.2f,
                    markerIndex = index + 7,
                )
                position = Offset(
                    x = anchorPoint.x + nudgedOffset.x,
                    y = anchorPoint.y + nudgedOffset.y,
                )
            }
            position = position.clampToViewport(
                canvasSize = canvasSize,
                margin = placement.viewportMarginPx,
            )
            TrackIssueMarker(
                issue = issue,
                position = position,
                accent = issue.category.accent(palette),
            )
        }
}

internal fun resolveIssueMarkerPlacement(surfaceStrokePx: Float): TrackIssueMarkerPlacement {
    val baseDistance = (surfaceStrokePx * 0.5f + 20f).coerceIn(28f, 48f)
    return TrackIssueMarkerPlacement(
        baseDistancePx = baseDistance,
        focusDistanceBonusPx = (surfaceStrokePx * 0.32f).coerceIn(6f, 10f),
        stackStepPx = 8f,
        tangentShiftPx = (surfaceStrokePx * 0.35f).coerceIn(5f, 9f),
        outwardRetryPx = (surfaceStrokePx * 0.75f).coerceIn(10f, 16f),
        spacingPx = (surfaceStrokePx * 1.15f).coerceIn(24f, 34f),
        viewportMarginPx = (baseDistance + 8f).coerceIn(22f, 28f),
    )
}

internal fun List<CornerScoreUi>.selectOverlayCorners(focusCorner: CornerScoreUi?, limit: Int): List<CornerScoreUi> {
    val orderedCorners = sortedBy(CornerScoreUi::trackPosition)
    if (orderedCorners.size <= limit) return orderedCorners
    val focusCornerNumber = focusCorner?.cornerNumber
    val selected = mutableListOf<CornerScoreUi>()

    fun MutableList<CornerScoreUi>.tryAdd(corner: CornerScoreUi) {
        if (size >= limit) return
        if (any { selectedCorner -> selectedCorner.cornerNumber == corner.cornerNumber }) return
        val tooCloseToExisting = any { selectedCorner ->
            circularFractionDistance(selectedCorner.trackPosition, corner.trackPosition) <
                trackCornerOverlayMinimumSpacingFraction
        }
        if (!tooCloseToExisting) {
            add(corner)
        }
    }

    focusCorner?.let(selected::tryAdd)
    val stride = (orderedCorners.size.toFloat() / limit.toFloat()).coerceAtLeast(1f)
    var cursor = 0f
    while (selected.size < limit && cursor < orderedCorners.size.toFloat() + stride) {
        val candidate = orderedCorners[cursor.toInt().coerceIn(0, orderedCorners.lastIndex)]
        if (candidate.cornerNumber != focusCornerNumber) {
            selected.tryAdd(candidate)
        }
        cursor += stride
    }
    orderedCorners.forEach { corner ->
        if (corner.cornerNumber == focusCornerNumber) return@forEach
        selected.tryAdd(corner)
    }

    return selected
        .distinctBy(CornerScoreUi::cornerNumber)
        .sortedBy(CornerScoreUi::trackPosition)
}

internal fun SessionAnalysisHighlightUi.stableOverlayKey(): String = buildString {
    append(id.ifBlank { "${category.name}-${cornerNumber ?: -1}-$title" })
    append('|')
    append(trackPosition?.let { trackPosition -> (trackPosition * 1000f).roundToInt() } ?: -1)
}

internal fun List<SessionAnalysisHighlightUi>.overlaySignature(): String = joinToString(separator = "|") { issue ->
    issue.stableOverlayKey()
}

internal fun SessionAnalysisHighlightCategory.shortLabel(): String = when (this) {
    SessionAnalysisHighlightCategory.TimeLoss -> "Δ"
    SessionAnalysisHighlightCategory.TrailBrakingMissing -> "TR"
    SessionAnalysisHighlightCategory.EarlyApexEntry -> "EA"
    SessionAnalysisHighlightCategory.LateApexEntry -> "LA"
    SessionAnalysisHighlightCategory.CoastingZone -> "CO"
    SessionAnalysisHighlightCategory.Understeer -> "U"
    SessionAnalysisHighlightCategory.Oversteer -> "O"
    SessionAnalysisHighlightCategory.WheelLockup -> "LK"
    SessionAnalysisHighlightCategory.WheelSpin -> "SP"
    SessionAnalysisHighlightCategory.InconsistentLine -> "LN"
    else -> "!"
}

private fun SessionAnalysisHighlightUi.isAttachedToFocusCorner(focusCorner: CornerScoreUi?): Boolean {
    if (focusCorner == null) return false
    if (cornerNumber != null && cornerNumber == focusCorner.cornerNumber) return true
    val issueTrackPosition = trackPosition ?: return false
    return circularFractionDistance(issueTrackPosition, focusCorner.trackPosition) <= focusIssueWindowFraction * 0.7f
}

private fun SessionAnalysisHighlightCategory.accent(palette: TrackDiagnosticPalette): Color = when (this) {
    SessionAnalysisHighlightCategory.TimeLoss -> palette.critical

    SessionAnalysisHighlightCategory.TrailBrakingMissing,
    SessionAnalysisHighlightCategory.EarlyApexEntry,
    SessionAnalysisHighlightCategory.LateApexEntry,
    SessionAnalysisHighlightCategory.CoastingZone,
    SessionAnalysisHighlightCategory.InconsistentLine,
        -> palette.warning

    SessionAnalysisHighlightCategory.Understeer -> palette.warning

    SessionAnalysisHighlightCategory.Oversteer -> palette.oversteer

    SessionAnalysisHighlightCategory.WheelLockup -> palette.lockup

    SessionAnalysisHighlightCategory.WheelSpin -> palette.wheelSpin

    else -> palette.neutral
}

private fun overlayOffset(direction: Offset?, distance: Float, tangentShiftPx: Float, markerIndex: Int): Offset {
    val resolvedDirection = direction ?: Offset(0f, -1f)
    val normal = Offset(-resolvedDirection.y, resolvedDirection.x)
    val side = if (markerIndex % 2 == 0) 1f else -1f
    val tangentShift = ((markerIndex % 3) - 1) * tangentShiftPx
    return Offset(
        x = normal.x * distance * side + resolvedDirection.x * tangentShift,
        y = normal.y * distance * side + resolvedDirection.y * tangentShift,
    )
}

private fun Offset.distanceTo(other: Offset): Float {
    val dx = x - other.x
    val dy = y - other.y
    return sqrt((dx * dx) + (dy * dy))
}

private fun Offset.clampToViewport(canvasSize: IntSize, margin: Float): Offset {
    if (canvasSize.width <= 0 || canvasSize.height <= 0) return this
    return Offset(
        x = x.coerceIn(margin, canvasSize.width.toFloat() - margin),
        y = y.coerceIn(margin, canvasSize.height.toFloat() - margin),
    )
}
