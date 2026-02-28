@file:Suppress("WildcardImport", "NoWildcardImports")

@file:OptIn(ExperimentalFoundationApi::class)

package com.project.analyzer.inputs.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.analyzer.feature.huds.inputs.Res.Res
import com.project.analyzer.feature.huds.inputs.Res.inputs_hud_active_tooltip
import com.project.analyzer.feature.huds.inputs.Res.inputs_hud_graph_tooltip
import com.project.analyzer.feature.huds.inputs.Res.inputs_hud_header_tooltip
import com.project.analyzer.feature.huds.inputs.Res.inputs_hud_idle_tooltip
import com.project.analyzer.feature.huds.inputs.Res.inputs_hud_percent
import com.project.analyzer.feature.huds.inputs.Res.inputs_hud_title
import com.project.analyzer.feature.huds.inputs.Res.inputs_hud_values
import com.project.analyzer.feature.huds.inputs.Res.inputs_hud_values_tooltip
import com.project.analyzer.hud.api.hudPanelSurfaceColor
import com.project.analyzer.inputs.presentation.componetns.InputsGraphBlock
import com.project.analyzer.inputs.presentation.componetns.LegendRow
import com.project.analyzer.inputs.presentation.model.InputsSeries
import com.project.analyzer.inputs.settings.InputHudSettings
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.format.formatPercent
import com.project.analyzer.ui.tooltip.Tooltip
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.sin

@Composable
internal fun InputsHudContent(
    state: InputsHudUiState = InputsHudUiState(),
    modifier: Modifier = Modifier,
    isToolTipEnabled: Boolean = false,
) {
    AnimatedVisibility(
        visible = state.isShow,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        val shape = SimAnalyzerTheme.corners.overlay

        Box(
            modifier = modifier
                .width(state.settings.widthDp.dp)
                .clip(shape)
                .background(color = hudPanelSurfaceColor(SimAnalyzerTheme.material.surface))
                .padding(all = 12.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (state.settings.showHeader) {
                    Header(state = state, isToolTipEnabled = isToolTipEnabled)
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Hinted(
                    tooltip = stringResource(Res.string.inputs_hud_graph_tooltip),
                    isEnabled = isToolTipEnabled,
                ) {
                    InputsGraphBlock(
                        state = state,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(state.settings.graphHeightDp.dp),
                    )
                }

                if (state.settings.showLegend) {
                    Spacer(Modifier.height(8.dp))
                    LegendRow(isToolTipEnabled = isToolTipEnabled)
                }
            }
        }
    }
}

@Composable
private fun Header(state: InputsHudUiState, isToolTipEnabled: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Hinted(
            tooltip = stringResource(Res.string.inputs_hud_header_tooltip),
            isEnabled = isToolTipEnabled,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResource(Res.string.inputs_hud_title),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
        }

        val dotColor = if (state.isSessionActive) {
            SimAnalyzerTheme.material.tertiary
        } else {
            SimAnalyzerTheme.extended.middlePriorityOutline
        }

        Hinted(
            tooltip = if (state.isSessionActive) {
                stringResource(Res.string.inputs_hud_active_tooltip)
            } else {
                stringResource(Res.string.inputs_hud_idle_tooltip)
            },
            isEnabled = isToolTipEnabled,
        ) {
            Box(
                modifier = Modifier
                    .size(size = 8.dp)
                    .background(dotColor, androidx.compose.foundation.shape.CircleShape),
            )
        }

        Spacer(Modifier.width(10.dp))

        Hinted(
            tooltip = stringResource(Res.string.inputs_hud_values_tooltip),
            isEnabled = isToolTipEnabled,
        ) {
            Text(
                text = stringResource(
                    Res.string.inputs_hud_values,
                    pct(state.throttle),
                    pct(state.brake),
                    pct(state.clutch),
                ),
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.bodyLarge.copy(fontFamily = SimAnalyzerTheme.fonts.mono),
            )
        }
    }
}

@Composable
private fun pct(v: Float): String = stringResource(
    Res.string.inputs_hud_percent,
    formatPercent((v * 100f).toInt()),
)

@Composable
private fun Hinted(
    tooltip: String,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        if (isEnabled) {
            Tooltip(tooltip = tooltip) {
                content()
            }
        } else {
            content()
        }
    }
}

@Preview(name = "Inputs HUD (Session)")
@Composable
internal fun InputsHudContentSessionPreview() {
    SimAnalyzerTheme {
        InputsHudContent(
            state = demoState(sessionActive = true),
            modifier = Modifier,
        )
    }
}

@Preview(name = "Inputs HUD (No session)")
@Composable
internal fun InputsHudContentNoSessionPreview() {
    SimAnalyzerTheme {
        InputsHudContent(
            state = demoState(sessionActive = false),
            modifier = Modifier,
        )
    }
}

internal fun demoState(
    sessionActive: Boolean,
    inputHudSettings: InputHudSettings = InputHudSettings(),
): InputsHudUiState {
    val capacity = inputHudSettings.historySeconds * 60
    val series = InputsSeries(capacity = capacity)

    if (sessionActive) {
        for (i in 0 until capacity) {
            val t = i / (capacity - 1f)

            val thr = clamp01(
                smoothPulse(t, start = 0.20f, rise = 0.10f, end = 0.55f, fall = 0.10f) * 0.95f +
                    smoothRamp(t, start = 0.70f, rise = 0.15f) * 0.55f,
            )

            val brk = clamp01(
                smoothPulse(t, start = 0.48f, rise = 0.08f, end = 0.72f, fall = 0.10f) * 0.95f,
            )

            val clt = clamp01(
                smoothPulse(t, start = 0.30f, rise = 0.05f, end = 0.38f, fall = 0.05f) * 0.25f +
                    smoothPulse(t, start = 0.62f, rise = 0.03f, end = 0.66f, fall = 0.03f) * 0.12f,
            )

            val steer = clampSigned(
                0.10f * sin(2f * PI.toFloat() * t * 3.0f) +
                    0.05f * sin(2f * PI.toFloat() * t * 11.0f) +
                    smoothPulseSigned(t, start = 0.55f, rise = 0.12f, end = 0.80f, fall = 0.12f) * 0.65f,
            )

            series.push(thr, brk, clt, steer)
        }
    }

    val last = sampleAtRightEdge(series)

    return InputsHudUiState(
        isShow = true,
        isSessionActive = sessionActive,
        throttle = last.thr,
        brake = last.brk,
        clutch = last.clt,
        steerNorm = last.steer,
        series = series,
        settings = inputHudSettings,
    )
}

private data class LastSample(val thr: Float, val brk: Float, val clt: Float, val steer: Float)

private fun sampleAtRightEdge(series: InputsSeries): LastSample {
    var lastT = 0f
    var lastB = 0f
    var lastC = 0f
    var lastS = 0f
    series.forEachOldestToNewest { _, t, b, c, s ->
        lastT = t
        lastB = b
        lastC = c
        lastS = s
    }
    return LastSample(lastT, lastB, lastC, lastS)
}

private fun clamp01(v: Float): Float = v.coerceIn(0f, 1f)
private fun clampSigned(v: Float): Float = v.coerceIn(-1f, 1f)

private fun smoothstep(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun smoothRamp(t: Float, start: Float, rise: Float): Float = smoothstep((t - start) / rise)

private fun smoothPulse(t: Float, start: Float, rise: Float, end: Float, fall: Float): Float {
    val up = smoothstep((t - start) / rise)
    val down = 1f - smoothstep((t - end) / fall)
    return (up * down).coerceIn(0f, 1f)
}

private fun smoothPulseSigned(t: Float, start: Float, rise: Float, end: Float, fall: Float): Float =
    smoothPulse(t, start, rise, end, fall)
