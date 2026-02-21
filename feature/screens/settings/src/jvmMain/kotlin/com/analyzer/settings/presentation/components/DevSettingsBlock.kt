package com.analyzer.settings.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.onClick

@Composable
internal fun DevSettingsBlock(modifier: Modifier = Modifier, onOpen: () -> Unit = {}) {
    Column(
        modifier = modifier
            .clip(SimAnalyzerTheme.shapes.large)
            .background(SimAnalyzerTheme.material.surface)
            .padding(16.dp),
    ) {
        Text(
            text = "Developer",
            color = SimAnalyzerTheme.material.onSurface,
            fontSize = 20.sp,
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Calibration, telemetry inspector, diagnostics",
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    fontSize = 13.sp,
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            Row(
                modifier = Modifier
                    .widthIn(min = 86.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SimAnalyzerTheme.material.primary.copy(alpha = 0.15f))
                    .border(
                        width = 1.dp,
                        color = SimAnalyzerTheme.material.primary.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(10.dp),
                    )
                    .onClick(onClick = onOpen)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Open",
                    color = SimAnalyzerTheme.material.onSurface,
                    fontSize = 12.sp,
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = ">",
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
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
