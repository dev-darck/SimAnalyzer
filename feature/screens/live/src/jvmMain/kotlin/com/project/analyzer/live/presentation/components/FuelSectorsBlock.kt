@file:Suppress("WildcardImport", "NoWildcardImports")

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.analyzer.feature.screens.live.Res.*
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.format.formatDecimal
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal data class Sector(val index: Int, val value: String, val status: ValueStatus)

@Composable
internal fun FuelSectorsBlock(
    fuelLiters: Float,
    estLaps: Float,
    fuelPerLap: Float = 0F,
    sectors: ImmutableList<Sector>,
    modifier: Modifier = Modifier,
) {
    val cardBg = SimAnalyzerTheme.material.surface
    val titleColor = SimAnalyzerTheme.material.onSurface
    val mutedColor = SimAnalyzerTheme.material.onSurfaceVariant

    Column(
        modifier = modifier
            .clip(SimAnalyzerTheme.shapes.large)
            .background(cardBg)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(Res.string.race_info_title),
            color = titleColor,
            style = SimAnalyzerTheme.typography.titleSmall,
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(Res.drawable.fuel),
                contentDescription = null,
            )

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    text = stringResource(Res.string.race_info_fuel),
                    color = mutedColor,
                    style = SimAnalyzerTheme.typography.labelMedium,
                )
                Text(
                    text = stringResource(Res.string.race_info_fuel_value, formatDecimal(fuelLiters, decimals = 1)),
                    color = titleColor,
                    style = SimAnalyzerTheme.typography.titleSmall.copy(fontFamily = SimAnalyzerTheme.fonts.mono),
                )
            }

            Spacer(Modifier.weight(1f))

            if (fuelPerLap > 0f) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(Res.string.race_info_fuel_per_lap),
                        color = mutedColor,
                        style = SimAnalyzerTheme.typography.labelMedium,
                    )
                    Text(
                        text = formatDecimal(fuelPerLap, decimals = 2),
                        color = titleColor,
                        style = SimAnalyzerTheme.typography.titleSmall.copy(fontFamily = SimAnalyzerTheme.fonts.mono),
                    )
                }

                Spacer(Modifier.width(18.dp))
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(Res.string.race_info_est_laps),
                    color = mutedColor,
                    style = SimAnalyzerTheme.typography.labelMedium,
                )
                Text(
                    text = formatDecimal(estLaps, decimals = 1),
                    color = titleColor,
                    style = SimAnalyzerTheme.typography.titleSmall.copy(fontFamily = SimAnalyzerTheme.fonts.mono),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        HorizontalDivider(
            color = SimAnalyzerTheme.material.outlineVariant,
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            sectors.forEach { sector ->
                SectorItem(
                    sector = sector,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SectorItem(sector: Sector, modifier: Modifier = Modifier) {
    val tileShape = SimAnalyzerTheme.corners.card
    val tileBg = SimAnalyzerTheme.material.surfaceVariant
    val mutedColor = SimAnalyzerTheme.material.onSurfaceVariant

    Column(
        modifier = modifier
            .heightIn(min = 58.dp)
            .clip(tileShape)
            .background(tileBg)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.race_info_sector, sector.index),
            color = mutedColor,
            style = SimAnalyzerTheme.typography.labelMedium,
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = sector.value,
            color = sector.status.statusColor(),
            style = SimAnalyzerTheme.typography.titleSmall.copy(fontFamily = SimAnalyzerTheme.fonts.mono),
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
            contentAlignment = Alignment.Center,
        ) {
            FuelSectorsBlock(
                fuelLiters = 42.5f,
                estLaps = 3.2f,
                fuelPerLap = 2.85f,
                sectors = persistentListOf(
                    Sector(1, "32.45", ValueStatus.BEST),
                    Sector(2, "28.12", ValueStatus.COMPLETED),
                    Sector(3, "--.--", ValueStatus.NORMAL),
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
