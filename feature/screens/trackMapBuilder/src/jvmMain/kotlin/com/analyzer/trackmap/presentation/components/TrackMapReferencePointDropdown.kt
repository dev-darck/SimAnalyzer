package com.analyzer.trackmap.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.theme.SimAnalyzerTheme

@Composable
fun TrackMapReferencePointDropdown(selected: ReferencePoint, onSelected: (ReferencePoint) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(
                text = selected.displayName(),
                style = SimAnalyzerTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Select reference point",
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ReferencePoint.entries.forEach { point ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = point.displayName(),
                            style = SimAnalyzerTheme.typography.bodySmall,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(point)
                    },
                )
            }
        }
    }
}

@Composable
private fun ReferencePoint.displayName(): String = when (this) {
    ReferencePoint.CAR_CENTER -> "Car center"
    ReferencePoint.FRONT_AXLE -> "Front axle"
    ReferencePoint.REAR_AXLE -> "Rear axle"
}
