package com.analyzer.session.analysis.presentation.components.map.support

import androidx.compose.ui.geometry.Offset
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi

/**
 * Viewport helpers keep pan and zoom math separate from the composable map scene code.
 */
internal fun List<SessionAnalysisFractionPointUi>.averageOffset(): Offset {
    if (isEmpty()) return Offset.Zero
    return Offset(
        x = sumOf { point -> point.x.toDouble() }.toFloat() / size,
        y = sumOf { point -> point.y.toDouble() }.toFloat() / size,
    )
}
