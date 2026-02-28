@file:OptIn(ExperimentalFoundationApi::class)

package com.project.analyzer.fuel.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.analyzer.feature.huds.fuel.Res.Res
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_overlay_waiting
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_peak_label
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_plan_empty
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_plan_horizon_time
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_plan_max
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_plan_now
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_plan_title
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_reset_content_description
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_subtitle_per_lap
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_subtitle_predictive
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_subtitle_waiting
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_subtitle_warmup
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_summary_basis
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_summary_fuel_left
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_summary_laps_left
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_summary_last_lap
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_title
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_tooltip_basis_approx
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_tooltip_basis_measured
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_tooltip_confidence_prefix
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_tooltip_fuel_left
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_tooltip_laps_left
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_tooltip_last_lap
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_tooltip_main_value
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_tooltip_peak_value
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_tooltip_phase_per_lap
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_tooltip_phase_predictive
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_tooltip_phase_waiting
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_tooltip_phase_warmup
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_tooltip_plan_horizon_time
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_tooltip_plan_max
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_tooltip_plan_now
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_tooltip_plan_title
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_tooltip_reset
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_tooltip_title
import com.project.analyzer.feature.huds.fuel.Res.fuel_hud_unit_laps
import com.project.analyzer.fuel.domain.model.FuelPhase
import com.project.analyzer.hud.api.hudPanelSurfaceColor
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.tooltip.Tooltip
import org.jetbrains.compose.resources.stringResource

private val FUEL_HUD_WIDTH = 304.dp

@Composable
fun FuelDemoContent(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        FuelHudContent(
            state = FuelHudUiState(
                isShow = true,
                isSessionActive = true,
                phase = FuelPhase.PREDICTIVE,
                mainValue = "1.45 L",
                peakValue = "1.52 L",
                fuelLeftText = "20.34 L",
                lapsRemainingCount = 14,
                lapsRemainingIsApprox = true,
                lapBasisText = "≈1:28.500",
                planRows = listOf(
                    PlanRowUi(5, "≈7:22", "7 L", "8 L"),
                    PlanRowUi(10, "≈14:45", "15 L", "16 L"),
                    PlanRowUi(15, "≈22:07", "22 L", "23 L"),
                ),
                confidence = 0.55,
            ),
            isToolTipEnabled = true,
        )
        FuelHudContent(
            state = FuelHudUiState(
                isShow = true,
                isSessionActive = true,
                phase = FuelPhase.PIT_WAITING,
                planRows = listOf(
                    PlanRowUi(5, "≈7:22", "7 L", "8 L"),
                    PlanRowUi(10, "≈14:45", "15 L", "16 L"),
                    PlanRowUi(15, "≈22:07", "22 L", "23 L"),
                ),
            ),
            isToolTipEnabled = true,
        )
    }
}

@Composable
internal fun FuelHudContent(
    state: FuelHudUiState,
    isToolTipEnabled: Boolean = false,
    onResetAll: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        enter = fadeIn(),
        exit = fadeOut(),
        visible = state.isShow,
    ) {
        val shape = SimAnalyzerTheme.corners.overlay

        Box(
            modifier = modifier
                .width(FUEL_HUD_WIDTH)
                .clip(shape)
                .background(hudPanelSurfaceColor(SimAnalyzerTheme.material.surface)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HeaderBlock(
                    mainValue = state.mainValue,
                    peakValue = state.peakValue,
                    confidence = state.confidence,
                    displayLapNumber = state.displayLapNumber,
                    isSessionActive = state.isSessionActive,
                    phase = state.phase,
                    onResetAll = onResetAll,
                    isToolTipEnabled = isToolTipEnabled,
                )

                SummaryBlock(
                    fuelLeftText = state.fuelLeftText,
                    lapsRemainingCount = state.lapsRemainingCount,
                    lapsRemainingIsApprox = state.lapsRemainingIsApprox,
                    lastLapTimeText = state.lastLapTimeText,
                    lapBasisText = state.lapBasisText,
                    lapBasisIsApprox = state.lapBasisIsApprox,
                    isCurrentLapValid = state.isCurrentLapValid,
                    isToolTipEnabled = isToolTipEnabled,
                )

                PlanBlock(
                    planRows = state.planRows,
                    isToolTipEnabled = isToolTipEnabled,
                )
            }

            if (state.phase == FuelPhase.PIT_WAITING) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(hudPanelSurfaceColor(SimAnalyzerTheme.material.surface, alpha = 0.72f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.fuel_hud_overlay_waiting),
                        color = SimAnalyzerTheme.material.onSurface,
                        style = SimAnalyzerTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderBlock(
    mainValue: String,
    peakValue: String,
    confidence: Double,
    displayLapNumber: Int,
    isSessionActive: Boolean,
    phase: FuelPhase,
    onResetAll: () -> Unit,
    isToolTipEnabled: Boolean = false,
) {
    val indicatorColor = when {
        !isSessionActive -> SimAnalyzerTheme.extended.middlePriorityOutline
        phase == FuelPhase.PIT_WAITING -> SimAnalyzerTheme.material.outline
        phase == FuelPhase.WARMUP -> SimAnalyzerTheme.material.secondary
        else -> SimAnalyzerTheme.material.tertiary
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val titleText = stringResource(Res.string.fuel_hud_title)
        val subtitleText = subtitleText(phase = phase, confidence = confidence, displayLapNumber = displayLapNumber)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Hinted(
                tooltip = stringResource(Res.string.fuel_hud_tooltip_title),
                isEnabled = isToolTipEnabled,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = titleText,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    style = SimAnalyzerTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.size(6.dp))

            Hinted(
                tooltip = stringResource(Res.string.fuel_hud_tooltip_reset),
                isEnabled = isToolTipEnabled,
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(Res.string.fuel_hud_reset_content_description),
                    tint = SimAnalyzerTheme.material.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(SimAnalyzerTheme.corners.indicator)
                        .clickable(onClick = onResetAll)
                        .padding(2.dp),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Hinted(
                tooltip = stringResource(Res.string.fuel_hud_tooltip_main_value),
                isEnabled = isToolTipEnabled,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = mainValue,
                    color = SimAnalyzerTheme.material.onSurface,
                    style = SimAnalyzerTheme.typography.headlineSmall.copy(fontFamily = SimAnalyzerTheme.fonts.mono),
                    maxLines = 1,
                )
            }

            Spacer(Modifier.size(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(Res.string.fuel_hud_peak_label),
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    style = SimAnalyzerTheme.typography.labelSmall,
                    maxLines = 1,
                )
                Hinted(
                    tooltip = stringResource(Res.string.fuel_hud_tooltip_peak_value),
                    isEnabled = isToolTipEnabled,
                ) {
                    Text(
                        text = peakValue,
                        color = SimAnalyzerTheme.material.primary,
                        style = SimAnalyzerTheme.typography.titleMedium.copy(fontFamily = SimAnalyzerTheme.fonts.mono),
                        maxLines = 1,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(indicatorColor, androidx.compose.foundation.shape.CircleShape),
            )

            Hinted(
                tooltip = phaseTooltip(phase = phase, confidence = confidence),
                isEnabled = isToolTipEnabled,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = subtitleText,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    style = SimAnalyzerTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SummaryBlock(
    fuelLeftText: String,
    lapsRemainingCount: Int?,
    lapsRemainingIsApprox: Boolean,
    lastLapTimeText: String,
    lapBasisText: String,
    lapBasisIsApprox: Boolean,
    isCurrentLapValid: Boolean,
    isToolTipEnabled: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.corners.item)
            .background(SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.20f))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SummaryMetric(
                label = stringResource(Res.string.fuel_hud_summary_fuel_left),
                value = fuelLeftText,
                tooltip = stringResource(Res.string.fuel_hud_tooltip_fuel_left),
                isToolTipEnabled = isToolTipEnabled,
                emphasized = true,
                modifier = Modifier.weight(1f),
            )
            SummaryMetric(
                label = stringResource(Res.string.fuel_hud_summary_laps_left),
                value = lapsRemainingText(
                    lapsRemainingCount = lapsRemainingCount,
                    lapsRemainingIsApprox = lapsRemainingIsApprox,
                ),
                tooltip = stringResource(Res.string.fuel_hud_tooltip_laps_left),
                isToolTipEnabled = isToolTipEnabled,
                emphasized = true,
                modifier = Modifier.weight(1f),
            )
        }

        if (lastLapTimeText.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SummaryMetric(
                    label = stringResource(Res.string.fuel_hud_summary_last_lap),
                    value = lastLapTimeText,
                    tooltip = stringResource(Res.string.fuel_hud_tooltip_last_lap),
                    isToolTipEnabled = isToolTipEnabled,
                    valueColor = if (isCurrentLapValid) {
                        SimAnalyzerTheme.material.onSurface
                    } else {
                        SimAnalyzerTheme.material.error
                    },
                    emphasized = false,
                    modifier = Modifier.weight(1f),
                )
                SummaryMetric(
                    label = stringResource(Res.string.fuel_hud_summary_basis),
                    value = lapBasisText,
                    tooltip = if (lapBasisIsApprox) {
                        stringResource(Res.string.fuel_hud_tooltip_basis_approx)
                    } else {
                        stringResource(Res.string.fuel_hud_tooltip_basis_measured)
                    },
                    isToolTipEnabled = isToolTipEnabled,
                    emphasized = false,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            SummaryMetric(
                label = stringResource(Res.string.fuel_hud_summary_basis),
                value = lapBasisText,
                tooltip = if (lapBasisIsApprox) {
                    stringResource(Res.string.fuel_hud_tooltip_basis_approx)
                } else {
                    stringResource(Res.string.fuel_hud_tooltip_basis_measured)
                },
                isToolTipEnabled = isToolTipEnabled,
                emphasized = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    tooltip: String,
    isToolTipEnabled: Boolean,
    modifier: Modifier = Modifier,
    valueColor: Color = SimAnalyzerTheme.material.onSurface,
    emphasized: Boolean = true,
) {
    Hinted(
        tooltip = tooltip,
        isEnabled = isToolTipEnabled,
        modifier = modifier,
    ) {
        Column {
            Text(
                text = label,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                color = valueColor,
                style = if (emphasized) {
                    SimAnalyzerTheme.typography.titleMedium.copy(fontFamily = SimAnalyzerTheme.fonts.mono)
                } else {
                    SimAnalyzerTheme.typography.labelLarge.copy(fontFamily = SimAnalyzerTheme.fonts.mono)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PlanBlock(planRows: List<PlanRowUi>, isToolTipEnabled: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.corners.item)
            .background(SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Hinted(
            tooltip = stringResource(Res.string.fuel_hud_tooltip_plan_title),
            isEnabled = isToolTipEnabled,
        ) {
            Text(
                text = stringResource(Res.string.fuel_hud_plan_title),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.labelMedium,
                maxLines = 1,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Hinted(
                tooltip = stringResource(Res.string.fuel_hud_tooltip_plan_horizon_time),
                isEnabled = isToolTipEnabled,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(Res.string.fuel_hud_plan_horizon_time),
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    style = SimAnalyzerTheme.typography.labelSmall,
                )
            }

            Hinted(
                tooltip = stringResource(Res.string.fuel_hud_tooltip_plan_now),
                isEnabled = isToolTipEnabled,
                modifier = Modifier.width(46.dp),
            ) {
                Text(
                    text = stringResource(Res.string.fuel_hud_plan_now),
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    style = SimAnalyzerTheme.typography.labelSmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }

            Spacer(Modifier.width(6.dp))

            Hinted(
                tooltip = stringResource(Res.string.fuel_hud_tooltip_plan_max),
                isEnabled = isToolTipEnabled,
                modifier = Modifier.width(46.dp),
            ) {
                Text(
                    text = stringResource(Res.string.fuel_hud_plan_max),
                    color = SimAnalyzerTheme.material.primary,
                    style = SimAnalyzerTheme.typography.labelSmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }
        }

        if (planRows.isEmpty()) {
            Text(
                text = stringResource(Res.string.fuel_hud_plan_empty),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
        } else {
            val rowShape = SimAnalyzerTheme.corners.item

            planRows.forEachIndexed { index, row ->
                val bg = if (index % 2 == 0) {
                    SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.32f)
                } else {
                    SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.18f)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bg, rowShape)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${row.laps} ${stringResource(Res.string.fuel_hud_unit_laps)} • ${row.timeText}",
                        color = SimAnalyzerTheme.material.onSurface,
                        style = SimAnalyzerTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = row.fuelText,
                        color = SimAnalyzerTheme.material.onSurface,
                        style = SimAnalyzerTheme.typography.titleSmall.copy(fontFamily = SimAnalyzerTheme.fonts.mono),
                        maxLines = 1,
                        modifier = Modifier.width(46.dp),
                        textAlign = TextAlign.End,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = row.peakFuelText,
                        color = SimAnalyzerTheme.material.primary,
                        style = SimAnalyzerTheme.typography.titleSmall.copy(fontFamily = SimAnalyzerTheme.fonts.mono),
                        maxLines = 1,
                        modifier = Modifier.width(46.dp),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

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

@Composable
private fun subtitleText(phase: FuelPhase, confidence: Double, displayLapNumber: Int): String = when (phase) {
    FuelPhase.PIT_WAITING -> stringResource(Res.string.fuel_hud_subtitle_waiting)

    FuelPhase.WARMUP -> stringResource(Res.string.fuel_hud_subtitle_warmup)

    FuelPhase.PREDICTIVE -> "${
        stringResource(
            Res.string.fuel_hud_subtitle_predictive,
        )
    } • ${(confidence * 100).toInt()}%"

    FuelPhase.PER_LAP -> "${
        stringResource(
            Res.string.fuel_hud_subtitle_per_lap,
        )
    } ($displayLapNumber) • ${(confidence * 100).toInt()}%"
}

@Composable
private fun phaseTooltip(phase: FuelPhase, confidence: Double): String {
    val description = when (phase) {
        FuelPhase.PIT_WAITING -> stringResource(Res.string.fuel_hud_tooltip_phase_waiting)
        FuelPhase.WARMUP -> stringResource(Res.string.fuel_hud_tooltip_phase_warmup)
        FuelPhase.PREDICTIVE -> stringResource(Res.string.fuel_hud_tooltip_phase_predictive)
        FuelPhase.PER_LAP -> stringResource(Res.string.fuel_hud_tooltip_phase_per_lap)
    }

    val confidenceText = if (confidence > 0.0) {
        "\n${stringResource(Res.string.fuel_hud_tooltip_confidence_prefix)} ${(confidence * 100).toInt()}%"
    } else {
        ""
    }

    return description + confidenceText
}

@Composable
private fun lapsRemainingText(lapsRemainingCount: Int?, lapsRemainingIsApprox: Boolean): String {
    val count = lapsRemainingCount ?: return "—"
    val prefix = if (lapsRemainingIsApprox) "≈" else "~"
    return "$prefix$count ${stringResource(Res.string.fuel_hud_unit_laps)}"
}

@Composable
@Preview
private fun FuelHudContentPreviewPredictive() {
    SimAnalyzerTheme {
        FuelHudContent(
            state = FuelHudUiState(
                isShow = true,
                isSessionActive = true,
                phase = FuelPhase.PREDICTIVE,
                mainValue = "1.45 L",
                peakValue = "1.52 L",
                fuelLeftText = "20.34 L",
                lapsRemainingCount = 14,
                lapsRemainingIsApprox = true,
                lapBasisText = "≈1:28.500",
                planRows = listOf(
                    PlanRowUi(5, "≈7:22", "7 L", "8 L"),
                    PlanRowUi(10, "≈14:45", "15 L", "16 L"),
                    PlanRowUi(15, "≈22:07", "22 L", "23 L"),
                ),
                confidence = 0.55,
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
