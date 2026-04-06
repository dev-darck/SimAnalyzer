package com.analyzer.session.analysis.presentation.components.map.support

/**
 * Resolves stroke widths from zoom so the track stays legible without overpowering overlays.
 */
internal fun resolveTrackMapStrokeWidths(surfaceStrokePx: Float, zoom: Float): SessionAnalysisTrackMapStrokeWidths {
    val visibleSurfaceWidth = (surfaceStrokePx * zoom.coerceAtLeast(1f))
        .takeIf { width -> width.isFinite() && width > 0f }
        ?: 14f

    return SessionAnalysisTrackMapStrokeWidths(
        sectorBoundaryCasing = (visibleSurfaceWidth * 0.29f).coerceIn(3.6f, 5.8f),
        sectorBoundaryCore = (visibleSurfaceWidth * 0.14f).coerceIn(1.8f, 2.8f),
        referenceCasing = (visibleSurfaceWidth * 0.20f).coerceIn(2.4f, 4.0f),
        referenceCore = (visibleSurfaceWidth * 0.10f).coerceIn(1.2f, 2.2f),
        idealCasing = (visibleSurfaceWidth * 0.32f).coerceIn(3.6f, 6.2f),
        idealCore = (visibleSurfaceWidth * 0.20f).coerceIn(2.2f, 4.2f),
        selectedBase = (visibleSurfaceWidth * 0.10f).coerceIn(1.2f, 2.2f),
        selectedTrailCasing = (visibleSurfaceWidth * 0.26f).coerceIn(2.8f, 5.2f),
        selectedTrailCore = (visibleSurfaceWidth * 0.18f).coerceIn(2.0f, 3.6f),
        markerRingRadius = (visibleSurfaceWidth * 0.33f).coerceIn(4.8f, 7.6f),
        markerRingStroke = (visibleSurfaceWidth * 0.09f).coerceIn(1.2f, 2.2f),
        markerInnerRadius = (visibleSurfaceWidth * 0.17f).coerceIn(2.6f, 4.0f),
        markerDirectionLength = (visibleSurfaceWidth * 0.33f).coerceIn(4.8f, 7.6f),
    )
}
