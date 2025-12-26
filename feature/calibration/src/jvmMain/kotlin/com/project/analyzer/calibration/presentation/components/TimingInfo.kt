package com.project.analyzer.calibration.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.concurrent.TimeUnit

@Composable
fun TimingInfo(name: String, current: Long?, last: Long?, best: Long?) {
    Row(modifier = Modifier.wrapContentSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$name:", style = MaterialTheme.typography.bodyLarge, color = Color.White)
        Text("Cur: ${formatMs(current)}", style = MaterialTheme.typography.bodyLarge, color = Color.White)
        Text("Last: ${formatMs(last)}", style = MaterialTheme.typography.bodyLarge, color = Color.White)
        Text("Best: ${formatMs(best)}", style = MaterialTheme.typography.bodyLarge, color = Color.White)
    }
}

fun formatMs(ms: Long?): String {
    if (ms == null) return "--:--.---"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    val millis = ms % 1000
    return String.format("%02d:%02d.%03d", minutes, seconds, millis)
}
