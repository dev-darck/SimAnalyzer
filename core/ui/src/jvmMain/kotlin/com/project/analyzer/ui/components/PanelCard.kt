package com.project.analyzer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.analyzer.core.ui.Res.Res
import com.project.analyzer.core.ui.Res.responsive_panel_card_preview_title
import com.project.analyzer.core.ui.Res.responsive_panel_card_preview_value
import com.project.analyzer.theme.SimAnalyzerTheme
import org.jetbrains.compose.resources.stringResource

@Composable
public fun ResponsivePanelCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    backgroundColor: Color = SimAnalyzerTheme.material.surface,
    borderColor: Color = SimAnalyzerTheme.chrome.borderSubtle,
    content: @Composable BoxWithConstraintsScope.() -> Unit,
) {
    val shape = SimAnalyzerTheme.shapes.large

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape,
            )
            .padding(contentPadding),
        content = content,
    )
}

@Preview
@Composable
private fun ResponsivePanelCardPreview() {
    SimAnalyzerTheme {
        ResponsivePanelCard {
            SessionStatCard(
                title = stringResource(Res.string.responsive_panel_card_preview_title),
                value = stringResource(Res.string.responsive_panel_card_preview_value),
            )
        }
    }
}
