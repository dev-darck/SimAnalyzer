@file:Suppress("WildcardImport", "NoWildcardImports")

package com.project.analyzer.calibration.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.project.analyzer.feature.dev.calibration.Res.*
import com.project.analyzer.theme.SimAnalyzerTheme
import org.jetbrains.compose.resources.stringResource
import java.util.concurrent.TimeUnit

@Composable
fun TimingInfo(name: String, current: Long?, last: Long?, best: Long?) {
    Row(modifier = Modifier.wrapContentSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(Res.string.calibration_verify_label_suffix, name),
            style = SimAnalyzerTheme.typography.bodyLarge,
            color = Color.White,
        )
        Text(
            stringResource(
                Res.string.calibration_verify_label_suffix,
                stringResource(Res.string.calibration_verify_cur),
            ) + " ${formatMs(current)}",
            style = SimAnalyzerTheme.typography.bodyLarge,
            color = Color.White,
        )
        Text(
            stringResource(
                Res.string.calibration_verify_label_suffix,
                stringResource(Res.string.calibration_verify_last),
            ) + " ${formatMs(last)}",
            style = SimAnalyzerTheme.typography.bodyLarge,
            color = Color.White,
        )
        Text(
            stringResource(
                Res.string.calibration_verify_label_suffix,
                stringResource(Res.string.calibration_verify_best),
            ) + " ${formatMs(best)}",
            style = SimAnalyzerTheme.typography.bodyLarge,
            color = Color.White,
        )
    }
}

fun formatMs(ms: Long?): String {
    if (ms == null) return "--:--.---"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    val millis = ms % 1000
    return String.format("%02d:%02d.%03d", minutes, seconds, millis)
}
