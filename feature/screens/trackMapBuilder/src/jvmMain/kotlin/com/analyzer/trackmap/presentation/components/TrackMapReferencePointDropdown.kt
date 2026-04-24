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
import com.analyzer.trackmap.presentation.model.TrackMapReferencePointUi
import com.project.analyzer.theme.SimAnalyzerTheme

@Composable
internal fun TrackMapReferencePointDropdown(
    selected: TrackMapReferencePointUi,
    onSelected: (TrackMapReferencePointUi) -> Unit,
) {
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
            TrackMapReferencePointUi.entries.forEach { point ->
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
private fun TrackMapReferencePointUi.displayName(): String = when (this) {
    TrackMapReferencePointUi.CarCenter -> "Car center"
    TrackMapReferencePointUi.FrontAxle -> "Front axle"
    TrackMapReferencePointUi.RearAxle -> "Rear axle"
}
