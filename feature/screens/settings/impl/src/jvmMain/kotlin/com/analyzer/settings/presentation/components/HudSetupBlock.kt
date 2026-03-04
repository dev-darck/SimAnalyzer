@file:Suppress("WildcardImport", "NoWildcardImports")

package com.analyzer.settings.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.analyzer.feature.screens.settings.impl.Res.*
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.onClick
import org.jetbrains.compose.resources.stringResource
import kotlin.math.sin

@Composable
internal fun HudSetupBlock(
    modifier: Modifier = Modifier,
    isHudEnabled: Boolean = true,
    onHudEnabledChange: (Boolean) -> Unit = {},
    onEditClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .clip(shape = SimAnalyzerTheme.shapes.large)
            .background(color = SimAnalyzerTheme.material.surface)
            .padding(all = 16.dp),
    ) {
        Text(
            text = stringResource(Res.string.hud_setup_title),
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.hud_setup_label),
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    style = SimAnalyzerTheme.typography.labelMedium,
                )
            }

            Box(
                modifier = Modifier
                    .widthIn(min = 72.dp)
                    .heightIn(min = 32.dp)
                    .background(
                        color = SimAnalyzerTheme.material.primary.copy(alpha = 0.22f),
                        shape = SimAnalyzerTheme.corners.control,
                    )
                    .onClick(
                        enabled = isHudEnabled,
                        onClick = onEditClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.hud_setup_edit),
                    color = SimAnalyzerTheme.material.onSurface,
                    style = SimAnalyzerTheme.typography.labelMedium,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        HudPreviewBox()

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.hud_setup_display),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.labelMedium,
            )

            Switch(
                checked = isHudEnabled,
                onCheckedChange = onHudEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = SimAnalyzerTheme.material.primary,
                    uncheckedThumbColor = Color.LightGray,
                    uncheckedTrackColor = SimAnalyzerTheme.material.onSurfaceVariant.copy(alpha = 0.25f),
                ),
            )
        }
    }
}

@Composable
private fun HudPreviewBox() {
    val shape = SimAnalyzerTheme.corners.card
    val borderColor = SimAnalyzerTheme.material.primary.copy(alpha = 0.25f)
    val bg = SimAnalyzerTheme.material.background

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = 300.dp)
            .clip(shape = shape)
            .background(color = bg)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .padding(14.dp),
    ) {
        ExampleHud(modifier = Modifier.align(Alignment.TopEnd))
        ExampleHud(modifier = Modifier.align(Alignment.TopStart))
        ExampleHud(modifier = Modifier.align(Alignment.BottomEnd))

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 6.dp, bottom = 6.dp)
                .width(width = 210.dp)
                .height(height = 60.dp)
                .clip(shape = SimAnalyzerTheme.corners.item)
                .background(color = SimAnalyzerTheme.material.surface.copy(alpha = 0.5f))
                .border(
                    width = 1.dp,
                    color = SimAnalyzerTheme.material.onSurface.copy(alpha = 0.10f),
                    shape = SimAnalyzerTheme.corners.item,
                ),
        ) {
            val red = SimAnalyzerTheme.extended.red
            val teal = SimAnalyzerTheme.extended.teal
            Canvas(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                val w = size.width
                val h = size.height

                fun drawWave(color: Color, phase: Float, amp: Float, base: Float) {
                    val path = Path()
                    for (i in 0..50) {
                        val x = w * i / 50f
                        val t = i / 50f * 6.28f
                        val y = (h * base) + sin(t + phase) * (h * amp)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, color = color, style = Stroke(width = 2f))
                }

                drawWave(red, phase = 0.5f, amp = 0.22f, base = 0.65f)
                drawWave(teal, phase = 1.4f, amp = 0.18f, base = 0.60f)
            }
        }
    }
}

@Composable
private fun ExampleHud(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(width = 120.dp)
            .fillMaxWidth(fraction = 0.45f)
            .height(height = 110.dp)
            .clip(shape = SimAnalyzerTheme.corners.field)
            .background(color = SimAnalyzerTheme.material.surface.copy(alpha = 0.55f))
            .border(
                width = 1.dp,
                color = SimAnalyzerTheme.material.onSurface.copy(alpha = 0.10f),
                shape = SimAnalyzerTheme.corners.field,
            )
            .padding(10.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .height(14.dp)
                    .fillMaxWidth(0.75f)
                    .clip(SimAnalyzerTheme.corners.compact)
                    .background(SimAnalyzerTheme.material.onSurface.copy(alpha = 0.12f)),
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .height(10.dp)
                    .fillMaxWidth(0.65f)
                    .clip(SimAnalyzerTheme.corners.compact)
                    .background(SimAnalyzerTheme.material.onSurface.copy(alpha = 0.10f)),
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .height(10.dp)
                    .fillMaxWidth(0.55f)
                    .clip(SimAnalyzerTheme.corners.compact)
                    .background(SimAnalyzerTheme.material.onSurface.copy(alpha = 0.10f)),
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .fillMaxWidth()
                    .clip(SimAnalyzerTheme.corners.control)
                    .background(SimAnalyzerTheme.material.onSurface.copy(alpha = 0.08f)),
            )
        }
    }
}

@Preview
@Composable
private fun HudSetupBlockPreview() {
    SimAnalyzerTheme {
        HudSetupBlock()
    }
}
