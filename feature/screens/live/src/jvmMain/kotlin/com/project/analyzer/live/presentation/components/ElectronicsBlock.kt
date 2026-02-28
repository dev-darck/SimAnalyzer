@file:Suppress("WildcardImport", "NoWildcardImports")

package com.project.analyzer.live.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.analyzer.feature.screens.live.Res.*
import com.project.analyzer.theme.SimAnalyzerTheme
import org.jetbrains.compose.resources.stringResource

internal enum class ElectronicItemLabel {
    TC,
    ABS,
    Map,
    BrakeBias,
    SlipMax,
    Load,
    TyreAverage,
}

internal data class ElectronicItemUi(
    val label: ElectronicItemLabel,
    val value: String,
    val highlighted: Boolean = false,
)

internal enum class ElectronicsTitle {
    Electronics,
    Dynamics,
    TcAbsActive,
    TcActive,
    AbsActive,
}

internal data class ElectronicsBlockUi(
    val title: ElectronicsTitle = ElectronicsTitle.Electronics,
    val items: List<ElectronicItemUi> = emptyList(),
)

@Composable
internal fun ElectronicsBlock(data: ElectronicsBlockUi, modifier: Modifier = Modifier, gap: Dp = 12.dp) {
    val tileShape = SimAnalyzerTheme.corners.card

    val cardBg = SimAnalyzerTheme.material.surface
    val tileBg = SimAnalyzerTheme.material.surfaceVariant
    val highlightBg = SimAnalyzerTheme.material.primary

    val titleColor = SimAnalyzerTheme.material.onSurface
    val muted = SimAnalyzerTheme.material.onSurfaceVariant
    val valueColor = SimAnalyzerTheme.material.onSurface

    val isWarning = data.title in setOf(
        ElectronicsTitle.TcAbsActive,
        ElectronicsTitle.TcActive,
        ElectronicsTitle.AbsActive,
    )
    val headerColor = if (isWarning) SimAnalyzerTheme.extended.amber else titleColor

    Column(
        modifier = modifier
            .clip(SimAnalyzerTheme.shapes.large)
            .background(cardBg)
            .padding(22.dp),
    ) {
        Text(
            text = electronicsTitleText(data.title),
            color = headerColor,
            style = SimAnalyzerTheme.typography.titleSmall,
        )

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                ElectronicsTile(
                    item = data.items.getOrNull(0) ?: ElectronicItemUi(ElectronicItemLabel.TC, "0"),
                    tileShape = tileShape,
                    tileBg = tileBg,
                    highlightBg = highlightBg,
                    titleColor = muted,
                    valueColor = valueColor,
                    modifier = Modifier.weight(1f),
                )
                ElectronicsTile(
                    item = data.items.getOrNull(1) ?: ElectronicItemUi(ElectronicItemLabel.ABS, "0"),
                    tileShape = tileShape,
                    tileBg = tileBg,
                    highlightBg = highlightBg,
                    titleColor = muted,
                    valueColor = valueColor,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                ElectronicsTile(
                    item = data.items.getOrNull(2) ?: ElectronicItemUi(ElectronicItemLabel.Map, "0"),
                    tileShape = tileShape,
                    tileBg = tileBg,
                    highlightBg = highlightBg,
                    titleColor = muted,
                    valueColor = valueColor,
                    modifier = Modifier.weight(1f),
                )
                ElectronicsTile(
                    item = data.items.getOrNull(3) ?: ElectronicItemUi(ElectronicItemLabel.BrakeBias, "0%"),
                    tileShape = tileShape,
                    tileBg = tileBg,
                    highlightBg = highlightBg,
                    titleColor = muted,
                    valueColor = valueColor,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ElectronicsTile(
    item: ElectronicItemUi,
    tileShape: RoundedCornerShape,
    tileBg: Color,
    highlightBg: Color,
    titleColor: Color,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    val bg = if (item.highlighted) highlightBg else tileBg
    val value = if (item.highlighted) SimAnalyzerTheme.material.onPrimary else valueColor
    val title = if (item.highlighted) SimAnalyzerTheme.extended.onPrimaryContainer50 else titleColor

    Column(
        modifier = modifier
            .clip(tileShape)
            .background(bg)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = electronicsItemLabel(item.label),
            color = title,
            style = SimAnalyzerTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = item.value,
            color = value,
            style = SimAnalyzerTheme.typography.labelLarge.copy(fontFamily = SimAnalyzerTheme.fonts.mono),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun electronicsTitleText(title: ElectronicsTitle): String = when (title) {
    ElectronicsTitle.Electronics -> stringResource(Res.string.electronics_title)
    ElectronicsTitle.Dynamics -> stringResource(Res.string.electronics_dynamics)
    ElectronicsTitle.TcAbsActive -> stringResource(Res.string.electronics_warning_tc_abs)
    ElectronicsTitle.TcActive -> stringResource(Res.string.electronics_warning_tc)
    ElectronicsTitle.AbsActive -> stringResource(Res.string.electronics_warning_abs)
}

@Composable
private fun electronicsItemLabel(label: ElectronicItemLabel): String = when (label) {
    ElectronicItemLabel.TC -> stringResource(Res.string.electronics_item_tc)
    ElectronicItemLabel.ABS -> stringResource(Res.string.electronics_item_abs)
    ElectronicItemLabel.Map -> stringResource(Res.string.electronics_item_map)
    ElectronicItemLabel.BrakeBias -> stringResource(Res.string.electronics_item_bb)
    ElectronicItemLabel.SlipMax -> stringResource(Res.string.electronics_item_slip_max)
    ElectronicItemLabel.Load -> stringResource(Res.string.electronics_item_load)
    ElectronicItemLabel.TyreAverage -> stringResource(Res.string.electronics_item_tyre_avg)
}

@Preview
@Composable
private fun ElectronicsBlockPreview() {
    SimAnalyzerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SimAnalyzerTheme.material.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            ElectronicsBlock(
                data = ElectronicsBlockUi(
                    title = ElectronicsTitle.TcActive,
                    items = listOf(
                        ElectronicItemUi(ElectronicItemLabel.TC, "5", highlighted = true),
                        ElectronicItemUi(ElectronicItemLabel.ABS, "3"),
                        ElectronicItemUi(ElectronicItemLabel.Map, "2"),
                        ElectronicItemUi(ElectronicItemLabel.BrakeBias, "66.6%"),
                    ),
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
