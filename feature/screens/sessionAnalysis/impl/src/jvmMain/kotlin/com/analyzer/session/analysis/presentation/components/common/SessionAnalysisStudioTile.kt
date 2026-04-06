package com.analyzer.session.analysis.presentation.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.onClick

@Composable
internal fun SessionAnalysisStudioTile(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = modifier.then(
            if (onClick != null) {
                Modifier.onClick(onClick = onClick)
            } else {
                Modifier
            },
        ),
        shape = SimAnalyzerTheme.corners.field,
        color = if (selected) {
            SimAnalyzerTheme.chrome.fillSelection
        } else {
            SimAnalyzerTheme.chrome.fillMuted
        },
        border = BorderStroke(1.dp, sessionAnalysisStudioTileBorderColor(selected)),
    ) {
        Box(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

@Composable
internal fun sessionAnalysisStudioTileBorderColor(selected: Boolean): Color = if (selected) {
    SimAnalyzerTheme.material.primary.copy(alpha = 0.24f)
} else {
    SimAnalyzerTheme.chrome.borderSubtle
}
