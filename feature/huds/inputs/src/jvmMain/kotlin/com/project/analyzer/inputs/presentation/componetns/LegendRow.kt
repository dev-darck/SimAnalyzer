package com.project.analyzer.inputs.presentation.componetns

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.analyzer.theme.SimAnalyzerTheme

@Composable
internal fun LegendRow() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LegendItem(label = "T", color = SimAnalyzerTheme.extended.lightGreen)
        Spacer(modifier = Modifier.width(10.dp))
        LegendItem(label = "B", color = SimAnalyzerTheme.extended.red)
        Spacer(modifier = Modifier.width(10.dp))
        LegendItem(label = "C", color = SimAnalyzerTheme.extended.amber)
        Spacer(modifier = Modifier.width(10.dp))
        LegendItem(label = "S", color = SimAnalyzerTheme.extended.cyan)
    }
}

@Composable
private fun LegendItem(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(size = 8.dp)
                .background(color = color, shape = CircleShape),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, color = SimAnalyzerTheme.material.onSurfaceVariant)
    }
}
