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
import com.analyzer.settings.api.AppCloseBehavior
import com.project.analyzer.feature.screens.settings.impl.Res.Res
import com.project.analyzer.feature.screens.settings.impl.Res.close_behavior_ask_description
import com.project.analyzer.feature.screens.settings.impl.Res.close_behavior_ask_title
import com.project.analyzer.feature.screens.settings.impl.Res.close_behavior_description
import com.project.analyzer.feature.screens.settings.impl.Res.close_behavior_exit_description
import com.project.analyzer.feature.screens.settings.impl.Res.close_behavior_exit_title
import com.project.analyzer.feature.screens.settings.impl.Res.close_behavior_minimize_description
import com.project.analyzer.feature.screens.settings.impl.Res.close_behavior_minimize_title
import com.project.analyzer.feature.screens.settings.impl.Res.close_behavior_title
import com.project.analyzer.theme.SimAnalyzerTheme
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CloseBehaviorBlock(
    selectedBehavior: AppCloseBehavior,
    onBehaviorSelected: (AppCloseBehavior) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        CloseBehaviorOptionUi(
            behavior = AppCloseBehavior.AskEveryTime,
            title = stringResource(Res.string.close_behavior_ask_title),
            description = stringResource(Res.string.close_behavior_ask_description),
        ),
        CloseBehaviorOptionUi(
            behavior = AppCloseBehavior.Exit,
            title = stringResource(Res.string.close_behavior_exit_title),
            description = stringResource(Res.string.close_behavior_exit_description),
        ),
        CloseBehaviorOptionUi(
            behavior = AppCloseBehavior.MinimizeToTray,
            title = stringResource(Res.string.close_behavior_minimize_title),
            description = stringResource(Res.string.close_behavior_minimize_description),
        ),
    )

    Column(
        modifier = modifier
            .clip(shape = SimAnalyzerTheme.shapes.large)
            .background(color = SimAnalyzerTheme.material.surface)
            .padding(all = 16.dp),
    ) {
        Text(
            text = stringResource(Res.string.close_behavior_title),
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.close_behavior_description),
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.bodySmall,
        )

        Spacer(modifier = Modifier.height(12.dp))

        CloseBehaviorDropdown(
            selectedBehavior = selectedBehavior,
            options = options,
            onBehaviorSelected = onBehaviorSelected,
        )
    }
}

@Composable
private fun CloseBehaviorDropdown(
    selectedBehavior: AppCloseBehavior,
    options: List<CloseBehaviorOptionUi>,
    onBehaviorSelected: (AppCloseBehavior) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var menuWidthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val selectedOption = options.firstOrNull { option -> option.behavior == selectedBehavior } ?: options.first()
    val menuWidthDp = if (menuWidthPx > 0) with(density) { menuWidthPx.toDp() } else null
    val shape = SimAnalyzerTheme.shapes.medium
    val containerColor = SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.2f)
    val borderColor = if (expanded) {
        SimAnalyzerTheme.chrome.borderInteractive
    } else {
        SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.6f)
    }
    val accentColor = SimAnalyzerTheme.material.primary

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
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = selectedOption.title,
                    color = SimAnalyzerTheme.material.onSurface,
                    style = SimAnalyzerTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = selectedOption.description,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    style = SimAnalyzerTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(SimAnalyzerTheme.corners.item)
                    .background(
                        if (expanded) {
                            accentColor.copy(alpha = 0.16f)
                        } else {
                            SimAnalyzerTheme.chrome.fillMuted
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (expanded) accentColor else SimAnalyzerTheme.material.onSurfaceVariant,
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
                val isSelected = option.behavior == selectedBehavior
                DropdownMenuItem(
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = option.title,
                                color = SimAnalyzerTheme.material.onSurface,
                                style = SimAnalyzerTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = option.description,
                                color = SimAnalyzerTheme.material.onSurfaceVariant,
                                style = SimAnalyzerTheme.typography.bodySmall,
                                maxLines = 2,
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
                        onBehaviorSelected(option.behavior)
                    },
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clip(SimAnalyzerTheme.corners.field)
                        .background(
                            if (isSelected) accentColor.copy(alpha = 0.10f) else Color.Transparent,
                        ),
                )
                if (index != options.lastIndex) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

private data class CloseBehaviorOptionUi(val behavior: AppCloseBehavior, val title: String, val description: String)

@Preview
@Composable
private fun CloseBehaviorBlockPreview() {
    SimAnalyzerTheme {
        CloseBehaviorBlock(
            selectedBehavior = AppCloseBehavior.MinimizeToTray,
            onBehaviorSelected = {},
        )
    }
}
