package com.analyzer.session.analysis.presentation.components.map.path

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

/**
 * Builds deterministic linear paths from sampled points for map overlays and hit testing.
 */
internal class SessionAnalysisLinearPathFactory {

    fun create(points: List<Offset>, closed: Boolean = false, breakIndices: Set<Int> = emptySet()): Path {
        val segmentedPath = segmentTrackPath(
            points = points,
            closed = closed,
            breakIndices = breakIndices,
        )
        if (segmentedPath.segments.isEmpty()) return Path()
        val path = Path()
        segmentedPath.segments.forEach { segment ->
            if (segment.isEmpty()) return@forEach
            path.moveTo(segment.first().x, segment.first().y)
            for (index in 1 until segment.size) {
                path.lineTo(segment[index].x, segment[index].y)
            }
        }
        if (segmentedPath.closeLoop && segmentedPath.segments.firstOrNull()?.size ?: 0 > 2) {
            path.close()
        }
        return path
    }
}
