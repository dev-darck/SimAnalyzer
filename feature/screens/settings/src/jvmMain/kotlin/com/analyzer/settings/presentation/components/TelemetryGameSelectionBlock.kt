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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.analyzer.settings.presentation.GameSelectionUi
import com.analyzer.settings.presentation.buildGameSelectionUi
import com.project.analyzer.game.api.GameId
import com.project.analyzer.game.api.GameSelection
import com.project.analyzer.theme.SimAnalyzerTheme

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
            text = "Game selection",
            color = SimAnalyzerTheme.material.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Choose auto detection or lock telemetry to a single game.",
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            fontSize = 12.sp,
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
    val currentLabel = selectionUi.label
    val currentSubtitle = selectionUi.subtitle
    val accentColor = if (selectionUi.isAuto) SimAnalyzerTheme.extended.teal else SimAnalyzerTheme.material.primary
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
                label = selectionUi.tag,
                background = chipColor,
                textColor = accentColor,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = currentLabel,
                    color = SimAnalyzerTheme.material.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = currentSubtitle,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Select game",
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
                val itemBackground = if (isSelected) {
                    accentColor.copy(alpha = 0.12f)
                } else {
                    Color.Transparent
                }
                DropdownMenuItem(
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = option.label,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = option.subtitle,
                                fontSize = 11.sp,
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
                        .clip(RoundedCornerShape(12.dp))
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
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp,
        )
    }
}

@Preview
@Composable
private fun TelemetryGameSelectionBlockPreview() {
    SimAnalyzerTheme {
        TelemetryGameSelectionBlock(
            selectionUi = buildGameSelectionUi(GameSelection.Manual(GameId.AC)),
        )
    }
}
