@file:Suppress("WildcardImport", "NoWildcardImports")

package com.analyzer.settings.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.analyzer.feature.screens.settings.impl.Res.Res
import com.project.analyzer.feature.screens.settings.impl.Res.developer_description
import com.project.analyzer.feature.screens.settings.impl.Res.developer_open
import com.project.analyzer.feature.screens.settings.impl.Res.developer_title
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.components.Button
import com.project.analyzer.ui.components.SimAnalyzerButtonSize
import com.project.analyzer.ui.components.SimAnalyzerButtonVariant
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DevSettingsBlock(modifier: Modifier = Modifier, onOpen: () -> Unit = {}) {
    Column(
        modifier = modifier
            .clip(SimAnalyzerTheme.shapes.large)
            .background(SimAnalyzerTheme.material.surface)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.developer_title),
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.developer_description),
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    style = SimAnalyzerTheme.typography.labelMedium,
                )
            }

            Button(
                text = stringResource(Res.string.developer_open),
                onClick = onOpen,
                variant = SimAnalyzerButtonVariant.Secondary,
                size = SimAnalyzerButtonSize.Compact,
            )
        }
    }
}

@Preview
@Composable
private fun DevSettingsBlockPreview() {
    SimAnalyzerTheme {
        DevSettingsBlock()
    }
}
