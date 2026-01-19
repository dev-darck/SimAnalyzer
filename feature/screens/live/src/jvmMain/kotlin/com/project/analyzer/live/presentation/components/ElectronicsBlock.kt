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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.analyzer.theme.SimAnalyzerTheme

internal data class ElectronicItemUi(
    val title: String,
    val value: String,
    val highlighted: Boolean = false
)

internal data class ElectronicsBlockUi(
    val title: String = "Electronics",
    val items: List<ElectronicItemUi> = emptyList()
)

@Composable
internal fun ElectronicsBlock(
    data: ElectronicsBlockUi,
    modifier: Modifier = Modifier,
    gap: Dp = 12.dp,
) {
    val cardShape = RoundedCornerShape(26.dp)
    val tileShape = RoundedCornerShape(16.dp)

    val cardBg = SimAnalyzerTheme.material.surface
    val tileBg = SimAnalyzerTheme.material.surfaceVariant
    val highlightBg = SimAnalyzerTheme.material.primary

    val titleColor = SimAnalyzerTheme.material.onSurface
    val muted = SimAnalyzerTheme.material.onSurface.copy(alpha = 0.45f)
    val valueColor = SimAnalyzerTheme.material.onSurface

    val isWarning = data.title.contains("⚠")
    val headerColor = if (isWarning) Color(0xFFFF9800) else titleColor

    Column(
        modifier = modifier
            .clip(cardShape)
            .background(cardBg)
            .padding(22.dp)
    ) {
        Text(
            text = data.title,
            color = headerColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                ElectronicsTile(
                    item = data.items.getOrNull(0) ?: ElectronicItemUi("TC", "0"),
                    tileShape = tileShape,
                    tileBg = tileBg,
                    highlightBg = highlightBg,
                    titleColor = muted,
                    valueColor = valueColor,
                    modifier = Modifier.weight(1f)
                )
                ElectronicsTile(
                    item = data.items.getOrNull(1) ?: ElectronicItemUi("ABS", "0"),
                    tileShape = tileShape,
                    tileBg = tileBg,
                    highlightBg = highlightBg,
                    titleColor = muted,
                    valueColor = valueColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                ElectronicsTile(
                    item = data.items.getOrNull(2) ?: ElectronicItemUi("MAP", "0"),
                    tileShape = tileShape,
                    tileBg = tileBg,
                    highlightBg = highlightBg,
                    titleColor = muted,
                    valueColor = valueColor,
                    modifier = Modifier.weight(1f)
                )
                ElectronicsTile(
                    item = data.items.getOrNull(3) ?: ElectronicItemUi("BB", "0%"),
                    tileShape = tileShape,
                    tileBg = tileBg,
                    highlightBg = highlightBg,
                    titleColor = muted,
                    valueColor = valueColor,
                    modifier = Modifier.weight(1f)
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
    modifier: Modifier = Modifier
) {
    val bg = if (item.highlighted) highlightBg else tileBg
    val value = if (item.highlighted) SimAnalyzerTheme.material.onPrimary else valueColor
    val title = if (item.highlighted) SimAnalyzerTheme.material.onPrimary.copy(alpha = 0.55f) else titleColor

    Column(
        modifier = modifier
            .clip(tileShape)
            .background(bg)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = item.title,
            color = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = item.value,
            color = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
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
            contentAlignment = Alignment.Center
        ) {
            ElectronicsBlock(
                data = ElectronicsBlockUi(
                    title = "⚠ TC Active",
                    items = listOf(
                        ElectronicItemUi("TC", "5", highlighted = true),
                        ElectronicItemUi("ABS", "3"),
                        ElectronicItemUi("MAP", "2"),
                        ElectronicItemUi("BB", "66.6%")
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
