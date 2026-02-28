@file:OptIn(ExperimentalFoundationApi::class)

@file:Suppress("WildcardImport", "NoWildcardImports")

package com.project.analyzer.live.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.analyzer.feature.screens.live.Res.*
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.tooltip.Tooltip
import org.jetbrains.compose.resources.stringResource
import java.util.Locale

enum class WheelPos {
    FL,
    FR,
    RL,
    RR,
}

data class WheelUi(
    val pos: WheelPos,
    val psi: Float = 0f,
    val tyreTempC: Float = 0f,
    val susMm: Int = 0,
    val psiOk: Boolean = false,
    val slip: Float = 0f,
    val brakeTempC: Float = 0f,
)

@Composable
fun WheelsBlock(wheels: List<WheelUi>, modifier: Modifier = Modifier, tileHeight: Dp = 108.dp, gap: Dp = 22.dp) {
    val tileShape = SimAnalyzerTheme.corners.display
    val badgeShape = SimAnalyzerTheme.corners.badge

    val surface = SimAnalyzerTheme.material.surface
    val tileBg = SimAnalyzerTheme.material.surfaceVariant
    val badgeBg = SimAnalyzerTheme.material.surface

    val titleColor = SimAnalyzerTheme.material.onSurface
    val labelColor = SimAnalyzerTheme.material.onSurface.copy(alpha = 0.45f)
    val subLineColor = SimAnalyzerTheme.material.onSurface.copy(alpha = 0.65f)

    fun wheel(pos: WheelPos): WheelUi = wheels.firstOrNull { it.pos == pos }
        ?: WheelUi(pos = pos)

    Column(
        modifier = modifier
            .clip(SimAnalyzerTheme.shapes.large)
            .background(surface)
            .padding(22.dp),
    ) {
        Text(
            text = stringResource(Res.string.wheels_title),
            color = titleColor,
            style = SimAnalyzerTheme.typography.titleSmall,
        )

        Spacer(Modifier.height(gap))

        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(tileHeight),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                WheelWideTile(
                    wheel = wheel(WheelPos.FL),
                    tileShape = tileShape,
                    badgeShape = badgeShape,
                    tileBg = tileBg,
                    badgeBg = badgeBg,
                    labelColor = labelColor,
                    subLineColor = subLineColor,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                WheelWideTile(
                    wheel = wheel(WheelPos.FR),
                    tileShape = tileShape,
                    badgeShape = badgeShape,
                    tileBg = tileBg,
                    badgeBg = badgeBg,
                    labelColor = labelColor,
                    subLineColor = subLineColor,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().height(tileHeight),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                WheelWideTile(
                    wheel = wheel(WheelPos.RL),
                    tileShape = tileShape,
                    badgeShape = badgeShape,
                    tileBg = tileBg,
                    badgeBg = badgeBg,
                    labelColor = labelColor,
                    subLineColor = subLineColor,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                WheelWideTile(
                    wheel = wheel(WheelPos.RR),
                    tileShape = tileShape,
                    badgeShape = badgeShape,
                    tileBg = tileBg,
                    badgeBg = badgeBg,
                    labelColor = labelColor,
                    subLineColor = subLineColor,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelWideTile(
    wheel: WheelUi,
    tileShape: RoundedCornerShape,
    badgeShape: RoundedCornerShape,
    tileBg: Color,
    badgeBg: Color,
    labelColor: Color,
    subLineColor: Color,
    modifier: Modifier = Modifier,
) {
    val psiColor = if (wheel.psiOk) SimAnalyzerTheme.extended.teal else SimAnalyzerTheme.material.onSurface
    val valueColor = SimAnalyzerTheme.material.onSurface

    val slipColor = when {
        wheel.slip > 0.15f -> SimAnalyzerTheme.extended.red
        wheel.slip > 0.05f -> SimAnalyzerTheme.extended.amber
        else -> subLineColor
    }

    val brakeColor = when {
        wheel.brakeTempC > 600f -> SimAnalyzerTheme.extended.red
        wheel.brakeTempC > 400f -> SimAnalyzerTheme.extended.amber
        else -> subLineColor
    }

    val psiText = String.format(Locale.US, "%04.1f", wheel.psi)
    val tyText = String.format(Locale.US, "%04.1f", wheel.tyreTempC)
    val slipText = String.format(Locale.US, "%.2f", wheel.slip)
    val brakeText = String.format(Locale.US, "%.0f", wheel.brakeTempC)

    Row(
        modifier = modifier
            .clip(tileShape)
            .background(tileBg)
            .padding(start = 16.dp, end = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(badgeShape)
                .background(badgeBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = wheelBadge(wheel.pos),
                color = valueColor,
                style = SimAnalyzerTheme.typography.titleSmall,
            )
        }

        InfoBlock(
            label = stringResource(Res.string.wheel_metric_psi),
            value = psiText,
            labelColor = labelColor,
            valueColor = psiColor,
            tooltip = stringResource(Res.string.wheel_tooltip_psi),
        )

        InfoBlock(
            label = stringResource(Res.string.wheel_metric_slip),
            value = slipText,
            labelColor = labelColor,
            valueColor = slipColor,
            tooltip = stringResource(Res.string.wheel_tooltip_slip),
        )

        InfoBlock(
            label = stringResource(Res.string.wheel_metric_sus),
            value = stringResource(Res.string.wheel_suspension_value, wheel.susMm),
            labelColor = labelColor,
            valueColor = subLineColor,
            tooltip = stringResource(Res.string.wheel_tooltip_sus),
        )

        InfoBlock(
            label = stringResource(Res.string.wheel_metric_brk),
            value = stringResource(Res.string.wheel_degree_value, brakeText),
            labelColor = labelColor,
            valueColor = brakeColor,
            tooltip = stringResource(Res.string.wheel_tooltip_brk),
        )

        InfoBlock(
            label = stringResource(Res.string.wheel_metric_ty),
            value = stringResource(Res.string.wheel_degree_value, tyText),
            labelColor = labelColor,
            valueColor = valueColor,
            tooltip = stringResource(Res.string.wheel_tooltip_ty),
        )
    }
}

@Composable
private fun InfoBlock(label: String, value: String, labelColor: Color, valueColor: Color, tooltip: String) {
    Tooltip(
        tooltip = tooltip,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = labelColor,
                style = SimAnalyzerTheme.typography.labelMedium,
            )
            Text(
                text = value,
                color = valueColor,
                style = SimAnalyzerTheme.typography.titleSmall.copy(fontFamily = SimAnalyzerTheme.fonts.mono),
            )
        }
    }
}

@Composable
private fun wheelBadge(pos: WheelPos): String = when (pos) {
    WheelPos.FL -> stringResource(Res.string.wheel_position_fl)
    WheelPos.FR -> stringResource(Res.string.wheel_position_fr)
    WheelPos.RL -> stringResource(Res.string.wheel_position_rl)
    WheelPos.RR -> stringResource(Res.string.wheel_position_rr)
}

@Preview
@Composable
private fun WheelsBlockPreview() {
    SimAnalyzerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SimAnalyzerTheme.material.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            WheelsBlock(
                wheels = listOf(
                    WheelUi(
                        pos = WheelPos.FL,
                        psi = 24.5f,
                        tyreTempC = 65.3f,
                        susMm = 12,
                        psiOk = true,
                        slip = 0.03f,
                        brakeTempC = 238f,
                    ),
                    WheelUi(
                        pos = WheelPos.FR,
                        psi = 24.2f,
                        tyreTempC = 62.4f,
                        susMm = 12,
                        psiOk = true,
                        slip = 0.02f,
                        brakeTempC = 236f,
                    ),
                    WheelUi(
                        pos = WheelPos.RL,
                        psi = 23.9f,
                        tyreTempC = 64.6f,
                        susMm = 15,
                        psiOk = false,
                        slip = 0.07f,
                        brakeTempC = 133f,
                    ),
                    WheelUi(
                        pos = WheelPos.RR,
                        psi = 23.8f,
                        tyreTempC = 63.4f,
                        susMm = 15,
                        psiOk = false,
                        slip = 0.08f,
                        brakeTempC = 133f,
                    ),
                ),
                modifier = Modifier.fillMaxWidth(),
                tileHeight = 108.dp,
            )
        }
    }
}
