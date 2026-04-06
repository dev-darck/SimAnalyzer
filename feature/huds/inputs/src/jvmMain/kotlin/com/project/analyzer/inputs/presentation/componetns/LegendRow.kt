@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

@file:Suppress("WildcardImport", "NoWildcardImports")

package com.project.analyzer.inputs.presentation.componetns

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.project.analyzer.feature.huds.inputs.Res.Res
import com.project.analyzer.feature.huds.inputs.Res.inputs_legend_b
import com.project.analyzer.feature.huds.inputs.Res.inputs_legend_c
import com.project.analyzer.feature.huds.inputs.Res.inputs_legend_s
import com.project.analyzer.feature.huds.inputs.Res.inputs_legend_t
import com.project.analyzer.feature.huds.inputs.Res.inputs_legend_tooltip_brake
import com.project.analyzer.feature.huds.inputs.Res.inputs_legend_tooltip_clutch
import com.project.analyzer.feature.huds.inputs.Res.inputs_legend_tooltip_steering
import com.project.analyzer.feature.huds.inputs.Res.inputs_legend_tooltip_throttle
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.tooltip.Tooltip
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LegendRow(isToolTipEnabled: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LegendItem(
            label = stringResource(Res.string.inputs_legend_t),
            color = SimAnalyzerTheme.extended.lightGreen,
            tooltip = stringResource(Res.string.inputs_legend_tooltip_throttle),
            isToolTipEnabled = isToolTipEnabled,
        )
        Spacer(modifier = Modifier.width(10.dp))
        LegendItem(
            label = stringResource(Res.string.inputs_legend_b),
            color = SimAnalyzerTheme.extended.red,
            tooltip = stringResource(Res.string.inputs_legend_tooltip_brake),
            isToolTipEnabled = isToolTipEnabled,
        )
        Spacer(modifier = Modifier.width(10.dp))
        LegendItem(
            label = stringResource(Res.string.inputs_legend_c),
            color = SimAnalyzerTheme.extended.amber,
            tooltip = stringResource(Res.string.inputs_legend_tooltip_clutch),
            isToolTipEnabled = isToolTipEnabled,
        )
        Spacer(modifier = Modifier.width(10.dp))
        LegendItem(
            label = stringResource(Res.string.inputs_legend_s),
            color = SimAnalyzerTheme.extended.cyan,
            tooltip = stringResource(Res.string.inputs_legend_tooltip_steering),
            isToolTipEnabled = isToolTipEnabled,
        )
    }
}

@Composable
private fun LegendItem(label: String, color: Color, tooltip: String, isToolTipEnabled: Boolean) {
    val content: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(size = 8.dp)
                    .background(color = color, shape = SimAnalyzerTheme.corners.pill),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.labelSmall,
            )
        }
    }

    if (isToolTipEnabled) {
        Tooltip(tooltip = tooltip) {
            content()
        }
    } else {
        content()
    }
}
