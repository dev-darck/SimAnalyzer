@file:OptIn(ExperimentalFoundationApi::class)

package com.project.analyzer.fuel.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.analyzer.fuel.domain.model.FuelPhase
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.tooltip.Tooltip

@Composable
fun FuelDemoContent(modifier: Modifier = Modifier) {
    FuelHudContent(
        state = FuelHudUiState(
            isShow = true,
            isSessionActive = true,
            phase = FuelPhase.PREDICTIVE,
            title = "Fuel / lap",
            subtitle = "Predictive • 55%",
            mainValue = "1.45 L",
            peakValue = "1.52 L",
            fuelLeftText = "20.34 L",
            lapsRemainingText = "≈14 laps",
            lapBasisText = "≈1:28.500",
            planRows = listOf(
                PlanRowUi("5 laps", "≈7:22", "7 L", "8 L"),
                PlanRowUi("10 laps", "≈14:45", "15 L", "16 L"),
                PlanRowUi("15 laps", "≈22:07", "22 L", "23 L"),
            ),
            confidence = 0.55
        ),
        isToolTipEnabled = true,
        modifier = modifier.padding(16.dp)
    )
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
        visible = state.isShow
    ) {
        val shape = RoundedCornerShape(14.dp)
        Box(
            modifier = modifier
                .width(300.dp)
                .background(
                    color = SimAnalyzerTheme.material.surface.copy(alpha = 0.35f),
                    shape = shape
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                HeaderBlock(
                    title = state.title,
                    mainValue = state.mainValue,
                    peakValue = state.peakValue,
                    subtitle = state.subtitle,
                    isSessionActive = state.isSessionActive,
                    phase = state.phase,
                    onResetAll = onResetAll,
                    isToolTipEnabled = isToolTipEnabled
                )

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.8f))
                Spacer(Modifier.height(8.dp))

                MetaBlock(
                    fuelLeftText = state.fuelLeftText,
                    lapsRemainingText = state.lapsRemainingText,
                    lastLapTimeText = state.lastLapTimeText,
                    lapBasisText = state.lapBasisText,
                    isCurrentLapValid = state.isCurrentLapValid
                )

                Spacer(Modifier.height(12.dp))

                PlanBlock(planRows = state.planRows)
            }

            if (state.phase == FuelPhase.PIT_WAITING) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(SimAnalyzerTheme.material.surface.copy(alpha = 0.7f), shape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Waiting in pits…",
                        color = SimAnalyzerTheme.material.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderBlock(
    title: String,
    mainValue: String,
    peakValue: String,
    subtitle: String,
    isSessionActive: Boolean,
    phase: FuelPhase,
    onResetAll: () -> Unit,
    isToolTipEnabled: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(8.dp))

            val indicatorColor = when {
                !isSessionActive -> SimAnalyzerTheme.extended.middlePriorityOutline
                phase == FuelPhase.PIT_WAITING -> SimAnalyzerTheme.material.outline
                phase == FuelPhase.WARMUP -> SimAnalyzerTheme.material.secondary
                else -> SimAnalyzerTheme.material.tertiary
            }

            Box(
                modifier = Modifier
                    .size(size = 8.dp)
                    .background(
                        color = indicatorColor,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )

            Spacer(Modifier.width(8.dp))

            Tooltip(
                tooltip = "Reset fuel info. Delete all saved fuel \ncalculations and reset UI state to default.",
                isShowTooltip = isToolTipEnabled,
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = SimAnalyzerTheme.material.onSurfaceVariant,
                    modifier = Modifier
                        .size(size = 18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            onResetAll()
                        }
                        .padding(2.dp)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subtitle,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = mainValue,
                    color = SimAnalyzerTheme.material.onSurface,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
                Text(
                    text = "max: $peakValue",
                    color = SimAnalyzerTheme.material.primary,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun MetaBlock(
    fuelLeftText: String,
    lapsRemainingText: String,
    lastLapTimeText: String,
    lapBasisText: String,
    isCurrentLapValid: Boolean,
) {
    KeyValueRow(key = "Fuel left", value = fuelLeftText)
    Spacer(Modifier.height(4.dp))
    KeyValueRow(key = "Laps remaining", value = lapsRemainingText)
    Spacer(Modifier.height(4.dp))

    if (lastLapTimeText.isNotEmpty()) {
        KeyValueRow(
            key = "Last lap",
            value = lastLapTimeText,
            valueColor = if (isCurrentLapValid) {
                SimAnalyzerTheme.material.onSurface
            } else {
                SimAnalyzerTheme.material.error
            }
        )
        Spacer(Modifier.height(4.dp))
    }

    KeyValueRow(key = "Lap time basis", value = lapBasisText)
}

@Composable
private fun PlanBlock(planRows: List<PlanRowUi>) {
    Text(
        text = "Fuel plan",
        color = SimAnalyzerTheme.material.onSurfaceVariant,
        maxLines = 1
    )
    Spacer(Modifier.height(6.dp))

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Horizon • Time",
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Now",
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                modifier = Modifier.width(50.dp),
                textAlign = TextAlign.End
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Max",
                color = SimAnalyzerTheme.material.primary,
                modifier = Modifier.width(50.dp),
                textAlign = TextAlign.End
            )
        }

        Spacer(Modifier.height(6.dp))

        val rowShape = RoundedCornerShape(10.dp)

        planRows.forEachIndexed { index, row ->
            val bg = if (index % 2 == 0) {
                SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.40f)
            } else {
                SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.22f)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bg, rowShape)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${row.label} • ${row.timeText}",
                    color = SimAnalyzerTheme.material.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = row.fuelText,
                    color = SimAnalyzerTheme.material.onSurface,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    modifier = Modifier.width(50.dp),
                    textAlign = TextAlign.End
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = row.peakFuelText,
                    color = SimAnalyzerTheme.material.primary,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    modifier = Modifier.width(50.dp),
                    textAlign = TextAlign.End
                )
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun KeyValueRow(
    key: String,
    value: String,
    valueColor: Color = SimAnalyzerTheme.material.onSurface,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = key,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            color = valueColor,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
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
                title = "Fuel / lap",
                subtitle = "Predictive • 55%",
                mainValue = "1.45 L",
                peakValue = "1.52 L",
                fuelLeftText = "20.34 L",
                lapsRemainingText = "≈14 laps",
                lapBasisText = "≈1:28.500",
                planRows = listOf(
                    PlanRowUi("5 laps", "≈7:22", "7 L", "8 L"),
                    PlanRowUi("10 laps", "≈14:45", "15 L", "16 L"),
                    PlanRowUi("15 laps", "≈22:07", "22 L", "23 L"),
                ),
                confidence = 0.55
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
