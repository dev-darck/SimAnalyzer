package com.project.analyzer.live.presentation.components

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.analyzer.live.Res.Res
import com.project.analyzer.live.Res.fuel
import com.project.analyzer.theme.SimAnalyzerTheme
import org.jetbrains.compose.resources.painterResource

internal data class Sector(
    val index: Int,
    val value: String,
    val status: ValueStatus
)

@Composable
internal fun FuelSectorsBlock(
    fuelLiters: Float,
    estLaps: Float,
    fuelPerLap: Float = 0F,
    sectors: List<Sector>,
    modifier: Modifier = Modifier,
) {
    val cardBg = SimAnalyzerTheme.material.surface
    val titleColor = SimAnalyzerTheme.material.onSurface
    val mutedColor = SimAnalyzerTheme.material.onSurfaceVariant

    Column(
        modifier = modifier
            .clip(SimAnalyzerTheme.shapes.large)
            .background(cardBg)
            .padding(22.dp)
    ) {
        Text(
            text = "Race Info",
            color = titleColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(Res.drawable.fuel),
                contentDescription = null,
            )

            Spacer(Modifier.width(14.dp))

            Column {
                Text(
                    text = "Fuel",
                    color = mutedColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = String.format("%.1f L", fuelLiters),
                    color = titleColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(Modifier.weight(1f))

            if (fuelPerLap > 0f) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "L/lap",
                        color = mutedColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = String.format("%.2f", fuelPerLap),
                        color = titleColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(Modifier.width(24.dp))
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Est. laps",
                    color = mutedColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = String.format("%.1f", estLaps),
                    color = titleColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        HorizontalDivider(
            color = SimAnalyzerTheme.material.outlineVariant
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            sectors.forEach { sector ->
                SectorItem(
                    sector = sector,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SectorItem(
    sector: Sector,
    modifier: Modifier = Modifier
) {
    val tileShape = RoundedCornerShape(16.dp)
    val tileBg = SimAnalyzerTheme.material.surfaceVariant
    val mutedColor = SimAnalyzerTheme.material.onSurfaceVariant

    Column(
        modifier = modifier
            .clip(tileShape)
            .background(tileBg)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "S${sector.index}",
            color = mutedColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = sector.value,
            color = sector.status.statusColor(),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Preview
@Composable
private fun FuelSectorsBlockPreview() {
    SimAnalyzerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SimAnalyzerTheme.material.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            FuelSectorsBlock(
                fuelLiters = 42.5f,
                estLaps = 3.2f,
                fuelPerLap = 2.85f,
                sectors = listOf(
                    Sector(1, "32.45", ValueStatus.BEST),
                    Sector(2, "28.12", ValueStatus.COMPLETED),
                    Sector(3, "--.--", ValueStatus.NORMAL),
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
