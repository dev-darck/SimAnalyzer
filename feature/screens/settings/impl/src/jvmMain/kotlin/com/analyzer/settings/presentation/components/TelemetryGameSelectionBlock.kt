@file:Suppress("WildcardImport", "NoWildcardImports")

package com.analyzer.settings.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.analyzer.settings.presentation.GameSelectionUi
import com.analyzer.settings.presentation.buildGameSelectionUi
import com.project.analyzer.feature.screens.settings.impl.Res.*
import com.project.analyzer.game.api.GameId
import com.project.analyzer.game.api.GameSelection
import com.project.analyzer.theme.SimAnalyzerTheme
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TelemetryGameSelectionBlock(
    selectionUi: GameSelectionUi,
    modifier: Modifier = Modifier,
    onSelectionChange: (GameSelection) -> Unit = {},
) {
    Column(
        modifier = modifier
            .clip(shape = SimAnalyzerTheme.shapes.large)
            .background(color = SimAnalyzerTheme.material.surface)
            .padding(all = 16.dp),
    ) {
        Text(
            text = stringResource(Res.string.game_selection_title),
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.game_selection_description),
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.bodySmall,
        )

        Spacer(modifier = Modifier.height(12.dp))

        GameSelectionDropdown(
            selectionUi = selectionUi,
            onSelectionChange = onSelectionChange,
        )
    }
}

@Composable
private fun GameSelectionDropdown(
    selectionUi: GameSelectionUi,
    onSelectionChange: (GameSelection) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var menuWidthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val options = selectionUi.options
    val currentText = selectionPresentation(selectionUi.selection)
    val accentColor = if (selectionUi.selection is GameSelection.Auto) {
        SimAnalyzerTheme.extended.teal
    } else {
        SimAnalyzerTheme.material.primary
    }
    val containerColor = SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.2f)
    val borderColor = SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.6f)
    val chipColor = accentColor.copy(alpha = 0.2f)
    val menuWidthDp = if (menuWidthPx > 0) with(density) { menuWidthPx.toDp() } else null
    val shape = SimAnalyzerTheme.shapes.medium

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(containerColor)
                .border(1.dp, borderColor, shape)
                .clickable { expanded = true }
                .onSizeChanged { menuWidthPx = it.width }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GameModeChip(
                label = currentText.tag,
                background = chipColor,
                textColor = accentColor,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = currentText.label,
                    color = SimAnalyzerTheme.material.onSurface,
                    style = SimAnalyzerTheme.typography.labelLarge.copy(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = currentText.subtitle,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    style = SimAnalyzerTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(SimAnalyzerTheme.corners.item)
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = stringResource(Res.string.game_selection_select_game),
                    tint = accentColor,
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .then(if (menuWidthDp != null) Modifier.width(menuWidthDp) else Modifier)
                .clip(shape)
                .background(SimAnalyzerTheme.material.surface)
                .border(1.dp, SimAnalyzerTheme.material.outlineVariant, shape),
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = option.selection == selectionUi.selection
                val optionText = selectionPresentation(option.selection)
                val itemBackground = if (isSelected) {
                    accentColor.copy(alpha = 0.12f)
                } else {
                    Color.Transparent
                }
                DropdownMenuItem(
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = optionText.label,
                                style = SimAnalyzerTheme.typography.labelLarge.copy(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = optionText.subtitle,
                                style = SimAnalyzerTheme.typography.labelSmall,
                                color = SimAnalyzerTheme.material.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    leadingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = accentColor,
                            )
                        } else {
                            Spacer(modifier = Modifier.size(20.dp))
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelectionChange(option.selection)
                    },
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clip(SimAnalyzerTheme.corners.field)
                        .background(itemBackground),
                )
                if (index != options.lastIndex) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun GameModeChip(label: String, background: Color, textColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(SimAnalyzerTheme.corners.pill)
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = textColor,
            style = SimAnalyzerTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun selectionPresentation(selection: GameSelection): GameSelectionPresentation = when (selection) {
    GameSelection.Auto -> GameSelectionPresentation(
        label = stringResource(Res.string.game_selection_auto_detect),
        subtitle = stringResource(Res.string.game_selection_auto_subtitle),
        tag = stringResource(Res.string.game_selection_tag_auto),
    )

    is GameSelection.Manual -> GameSelectionPresentation(
        label = when (selection.game) {
            GameId.AC -> stringResource(Res.string.game_assetto_corsa)
            GameId.ACC -> stringResource(Res.string.game_assetto_corsa_competizione)
            GameId.ACE -> stringResource(Res.string.game_assetto_corsa_evo)
        },
        subtitle = stringResource(Res.string.game_selection_manual_subtitle),
        tag = stringResource(Res.string.game_selection_tag_manual),
    )
}

private data class GameSelectionPresentation(val label: String, val subtitle: String, val tag: String)

@Preview
@Composable
private fun TelemetryGameSelectionBlockPreview() {
    SimAnalyzerTheme {
        TelemetryGameSelectionBlock(
            selectionUi = buildGameSelectionUi(GameSelection.Manual(GameId.AC)),
        )
    }
}
