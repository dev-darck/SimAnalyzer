package com.project.analyzer.calibration.presentation.verify.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.analyzer.math.Vec2

@Composable
fun DirectionInfoCard(forward: Vec2?, headingDegrees: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Car Direction", style = MaterialTheme.typography.titleSmall)
                forward?.let {
                    Text(
                        "Forward: (${"%.2f".format(it.x)}, ${"%.2f".format(it.y)})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    "Heading: ${"%.1f".format(headingDegrees)}°",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = getDirectionArrow(headingDegrees),
                style = MaterialTheme.typography.headlineLarge
            )
        }
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
