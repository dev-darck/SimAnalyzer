package com.project.analyzer.calibration.presentation.verify.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.project.analyzer.math.Vec2
import com.project.analyzer.theme.SimAnalyzerTheme

@Composable
fun DirectionInfoCard(forward: Vec2?, headingDegrees: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.shapes.medium)
            .background(SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.2f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                "Car direction",
                style = MaterialTheme.typography.titleSmall,
                color = SimAnalyzerTheme.material.onSurface
            )
            forward?.let {
                Text(
                    "Forward: (${"%.2f".format(it.x)}, ${"%.2f".format(it.y)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = SimAnalyzerTheme.material.onSurfaceVariant
                )
            }
            Text(
                "Heading: ${"%.1f".format(headingDegrees)} deg",
                style = MaterialTheme.typography.bodySmall,
                color = SimAnalyzerTheme.material.onSurfaceVariant
            )
        }
        Text(
            text = getDirectionArrow(headingDegrees),
            style = MaterialTheme.typography.headlineLarge,
            color = SimAnalyzerTheme.material.primary
        )
    }
}

private fun getDirectionArrow(degrees: Float): String {
    // Normalize to 0-360
    val normalized = ((degrees % 360) + 360) % 360
    return when {
        normalized !in 22.5..<337.5 -> "↑"
        normalized < 67.5 -> "↗"
        normalized < 112.5 -> "→"
        normalized < 157.5 -> "↘"
        normalized < 202.5 -> "↓"
        normalized < 247.5 -> "↙"
        normalized < 292.5 -> "←"
        else -> "↖"
    }
}
