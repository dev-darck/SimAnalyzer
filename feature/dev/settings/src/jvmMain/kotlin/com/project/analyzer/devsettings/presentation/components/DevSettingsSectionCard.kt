package com.project.analyzer.devsettings.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.project.analyzer.theme.SimAnalyzerTheme
import androidx.compose.material3.Text

@Composable
internal fun DevSettingsSectionCard(
    title: String,
    subtitle: String = "",
    showContent: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.shapes.large)
            .background(SimAnalyzerTheme.material.surface)
            .padding(16.dp),
    ) {
        Text(
            text = title,
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.titleMedium,
        )
        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
        }
        if (showContent) {
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
