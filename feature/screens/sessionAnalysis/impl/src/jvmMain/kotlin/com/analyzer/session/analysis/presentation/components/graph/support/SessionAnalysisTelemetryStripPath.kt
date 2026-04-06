package com.analyzer.session.analysis.presentation.components.graph.support

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

internal fun List<Offset>.toPolylinePath(): Path {
    if (isEmpty()) return Path()
    val path = Path()
    path.moveTo(first().x, first().y)
    for (index in 1 until size) {
        val point = this[index]
        path.lineTo(point.x, point.y)
    }
    return path
}
