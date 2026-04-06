package com.analyzer.session.analysis.presentation.components.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.analyzer.theme.SimAnalyzerTheme

/**
 * Shared panel shell keeps the studio surfaces visually consistent without repeating chrome setup.
 */
@Composable
internal fun SessionAnalysisStudioPanel(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    opaqueBackground: Boolean = false,
    containerColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val panelShape = SimAnalyzerTheme.corners.panel
    Surface(
        modifier = modifier.clip(panelShape),
        shape = panelShape,
        color = containerColor ?: if (opaqueBackground) {
            SimAnalyzerTheme.material.surface
        } else {
            SimAnalyzerTheme.material.surfaceVariant
        },
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = SimAnalyzerTheme.chrome.borderSubtle,
        ),
    )
    {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

@Preview
@Composable
internal fun SessionAnalysisStudioPanelPreview() {
    SimAnalyzerTheme {
        SessionAnalysisStudioPanel(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Session analysis block",
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.titleSmall,
            )
        }
    }
}
