@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.project.analyzer.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.project.analyzer.core.ui.Res.Res
import com.project.analyzer.core.ui.Res.dropdown_preview_all_games
import com.project.analyzer.core.ui.Res.dropdown_preview_assetto_corsa
import com.project.analyzer.core.ui.Res.dropdown_preview_assetto_corsa_competizione
import com.project.analyzer.core.ui.Res.dropdown_preview_automobilista_2
import com.project.analyzer.core.ui.Res.dropdown_preview_ea_sports_wrc
import com.project.analyzer.core.ui.Res.dropdown_preview_f1_24
import com.project.analyzer.core.ui.Res.dropdown_preview_iracing
import com.project.analyzer.core.ui.Res.dropdown_preview_label_game
import com.project.analyzer.core.ui.Res.dropdown_preview_le_mans_ultimate
import com.project.analyzer.core.ui.Res.dropdown_preview_rfactor_2
import com.project.analyzer.theme.SimAnalyzerTheme
import org.jetbrains.compose.resources.stringResource

private const val DROPDOWN_FIELD_MAX_WIDTH = 220
private const val DROPDOWN_FIELD_TEXT_MAX_WIDTH = 180
private const val DROPDOWN_POPUP_MAX_HEIGHT = 320
private const val DROPDOWN_POPUP_MAX_WIDTH = 280
private const val DROPDOWN_POPUP_CONTENT_PADDING = 8
private const val DROPDOWN_ITEM_HORIZONTAL_PADDING = 12
private const val DROPDOWN_ITEM_SPACING = 8
private const val DROPDOWN_ITEM_TRAILING_ICON_SIZE = 18

public data class DropdownFilterUi(
    val label: String = "",
    val selectedId: String = "",
    val selectedLabel: String = "",
    val options: List<DropdownOptionUi> = emptyList(),
)

public data class DropdownOptionUi(val id: String, val label: String)

public enum class FilterDropdownStyle {
    Inline,
    Stacked,
}

private data class DropdownFilterPresentation(
    val labelText: String,
    val enabled: Boolean,
    val selectedIndex: Int,
    val selectedLabel: String,
    val selectedId: String,
    val options: List<DropdownOptionUi>,
)

private val DropdownFieldShape = SimAnalyzerTheme.corners.field
private val DropdownPopupShape = SimAnalyzerTheme.corners.overlay
private val DropdownItemShape = SimAnalyzerTheme.corners.item

@Composable
public fun FilterDropdown(
    filter: DropdownFilterUi,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    style: FilterDropdownStyle = FilterDropdownStyle.Inline,
) {
    val presentation = remember(filter) { buildDropdownFilterPresentation(filter) }

    when (style) {
        FilterDropdownStyle.Inline -> InlineDropdown(
            presentation = presentation,
            onSelect = onSelect,
            modifier = modifier,
        )

        FilterDropdownStyle.Stacked -> StackedDropdown(
            presentation = presentation,
            onSelect = onSelect,
            modifier = modifier,
        )
    }
}

@Composable
private fun InlineDropdown(presentation: DropdownFilterPresentation, onSelect: (String) -> Unit, modifier: Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DropdownLabel(text = presentation.labelText)
        DropdownSelectorAnchor(
            presentation = presentation,
            onSelect = onSelect,
            modifier = Modifier.height(34.dp),
        )
    }
}

@Composable
private fun StackedDropdown(presentation: DropdownFilterPresentation, onSelect: (String) -> Unit, modifier: Modifier) {
    Column(modifier = modifier) {
        DropdownLabel(text = presentation.labelText)
        Spacer(modifier = Modifier.height(6.dp))
        DropdownSelectorAnchor(
            presentation = presentation,
            onSelect = onSelect,
        )
    }
}

@Composable
private fun DropdownSelectorAnchor(
    presentation: DropdownFilterPresentation,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val popupWidth = rememberDropdownPopupWidth(presentation = presentation)
    val popupPositionProvider = remember(density) {
        DropdownPopupPositionProvider(verticalOffset = density.run { 6.dp.roundToPx() })
    }

    LaunchedEffect(expanded, presentation.selectedIndex, presentation.options) {
        if (expanded && presentation.options.isNotEmpty()) {
            listState.scrollToItem(presentation.selectedIndex)
        }
    }

    Box(
        modifier = modifier,
    ) {
        DropdownTriggerField(
            text = presentation.selectedLabel,
            enabled = presentation.enabled,
            expanded = expanded,
            onClick = { expanded = true },
            modifier = Modifier,
        )

        if (expanded) {
            Popup(
                popupPositionProvider = popupPositionProvider,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                DropdownPopupPanel(
                    presentation = presentation,
                    listState = listState,
                    popupWidth = popupWidth,
                    onSelect = { selectedId ->
                        expanded = false
                        onSelect(selectedId)
                    },
                )
            }
        }
    }
}

@Composable
private fun DropdownTriggerField(
    text: String,
    enabled: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = SimAnalyzerTheme.material.secondaryContainer
    val contentColor = SimAnalyzerTheme.material.onSecondaryContainer
    val disabledContentColor = contentColor.copy(alpha = 0.55f)
    val borderColor = if (expanded) {
        SimAnalyzerTheme.chrome.borderInteractive
    } else {
        SimAnalyzerTheme.chrome.borderSecondary
    }

    Row(
        modifier = modifier
            .widthIn(max = DROPDOWN_FIELD_MAX_WIDTH.dp)
            .clip(DropdownFieldShape)
            .background(containerColor)
            .border(1.dp, borderColor, DropdownFieldShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = text,
            color = if (enabled) contentColor else disabledContentColor,
            style = SimAnalyzerTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = DROPDOWN_FIELD_TEXT_MAX_WIDTH.dp),
        )
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = if (enabled) contentColor else disabledContentColor,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun DropdownPopupPanel(
    presentation: DropdownFilterPresentation,
    listState: LazyListState,
    popupWidth: Dp,
    onSelect: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.width(popupWidth),
        shape = DropdownPopupShape,
        color = SimAnalyzerTheme.material.surface,
        border = BorderStroke(1.dp, SimAnalyzerTheme.material.outlineVariant),
        shadowElevation = 10.dp,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = DROPDOWN_POPUP_MAX_HEIGHT.dp)
                .padding(DROPDOWN_POPUP_CONTENT_PADDING.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(
                items = presentation.options,
                key = DropdownOptionUi::id,
            ) { option ->
                DropdownPopupItem(
                    option = option,
                    selected = option.id == presentation.selectedId,
                    onClick = { onSelect(option.id) },
                )
            }
        }
    }
}

@Composable
private fun DropdownPopupItem(option: DropdownOptionUi, selected: Boolean, onClick: () -> Unit) {
    val containerColor = if (selected) {
        SimAnalyzerTheme.material.secondaryContainer
    } else {
        Color.Transparent
    }
    val contentColor = if (selected) {
        SimAnalyzerTheme.material.onSecondaryContainer
    } else {
        SimAnalyzerTheme.material.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DropdownItemShape)
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = DROPDOWN_ITEM_HORIZONTAL_PADDING.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DROPDOWN_ITEM_SPACING.dp),
    ) {
        Text(
            text = option.label,
            color = contentColor,
            style = SimAnalyzerTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier.size(DROPDOWN_ITEM_TRAILING_ICON_SIZE.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(DROPDOWN_ITEM_TRAILING_ICON_SIZE.dp),
                )
            }
        }
    }
}

@Composable
private fun DropdownLabel(text: String) {
    Text(
        text = text,
        color = SimAnalyzerTheme.material.onSurfaceVariant,
        style = SimAnalyzerTheme.typography.labelMedium,
    )
}

@Composable
private fun rememberDropdownPopupWidth(presentation: DropdownFilterPresentation): Dp {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textStyle = SimAnalyzerTheme.typography.labelMedium
    val labelCandidates = remember(presentation) { dropdownPopupLabelCandidates(presentation) }

    return remember(labelCandidates, density, textMeasurer, textStyle) {
        val longestLabelWidthPx = labelCandidates
            .maxOfOrNull { label ->
                textMeasurer.measure(
                    text = label,
                    style = textStyle,
                    maxLines = 1,
                ).size.width
            } ?: 0

        with(density) {
            (
                longestLabelWidthPx.toDp() +
                    (DROPDOWN_POPUP_CONTENT_PADDING * 2).dp +
                    (DROPDOWN_ITEM_HORIZONTAL_PADDING * 2).dp +
                    DROPDOWN_ITEM_SPACING.dp +
                    DROPDOWN_ITEM_TRAILING_ICON_SIZE.dp
                ).coerceAtMost(DROPDOWN_POPUP_MAX_WIDTH.dp)
        }
    }
}

private fun buildDropdownFilterPresentation(filter: DropdownFilterUi): DropdownFilterPresentation =
    DropdownFilterPresentation(
        labelText = filter.label.uppercase(),
        enabled = filter.options.isNotEmpty(),
        selectedIndex = filter.options.indexOfFirst { option -> option.id == filter.selectedId }.coerceAtLeast(0),
        selectedLabel = filter.selectedLabel,
        selectedId = filter.selectedId,
        options = filter.options,
    )

private fun dropdownPopupLabelCandidates(presentation: DropdownFilterPresentation): List<String> = buildList {
    add(presentation.selectedLabel)
    addAll(presentation.options.map(DropdownOptionUi::label))
}.filter(String::isNotBlank)

private class DropdownPopupPositionProvider(private val verticalOffset: Int) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = when (layoutDirection) {
            LayoutDirection.Ltr -> anchorBounds.left
            LayoutDirection.Rtl -> anchorBounds.right - popupContentSize.width
        }.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))

        val spaceBelow = windowSize.height - anchorBounds.bottom
        val spaceAbove = anchorBounds.top
        val preferredBelow = anchorBounds.bottom + verticalOffset
        val preferredAbove = anchorBounds.top - popupContentSize.height - verticalOffset
        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        val y = if (spaceBelow >= popupContentSize.height + verticalOffset || spaceBelow >= spaceAbove) {
            preferredBelow
        } else {
            preferredAbove
        }.coerceIn(0, maxY)

        return IntOffset(x = x, y = y)
    }
}

@Preview(name = "Dropdown Inline")
@Composable
private fun FilterDropdownInlinePreview() {
    SimAnalyzerTheme {
        FilterDropdown(
            filter = previewDropdownFilter(),
            onSelect = {},
        )
    }
}

@Preview(name = "Dropdown Stacked")
@Composable
private fun FilterDropdownStackedPreview() {
    SimAnalyzerTheme {
        FilterDropdown(
            filter = previewDropdownFilter(),
            onSelect = {},
            style = FilterDropdownStyle.Stacked,
        )
    }
}

@Preview(name = "Dropdown Popup Content")
@Composable
private fun DropdownPopupPanelPreview() {
    SimAnalyzerTheme {
        DropdownPopupPanel(
            presentation = buildDropdownFilterPresentation(previewDropdownFilter()),
            listState = rememberLazyListState(),
            popupWidth = rememberDropdownPopupWidth(
                presentation = buildDropdownFilterPresentation(previewDropdownFilter()),
            ),
            onSelect = {},
        )
    }
}

@Composable
private fun previewDropdownFilter(): DropdownFilterUi = DropdownFilterUi(
    label = stringResource(Res.string.dropdown_preview_label_game),
    selectedId = "acc",
    selectedLabel = stringResource(Res.string.dropdown_preview_assetto_corsa_competizione),
    options = listOf(
        DropdownOptionUi(id = "all", label = stringResource(Res.string.dropdown_preview_all_games)),
        DropdownOptionUi(id = "acc", label = stringResource(Res.string.dropdown_preview_assetto_corsa_competizione)),
        DropdownOptionUi(id = "ac", label = stringResource(Res.string.dropdown_preview_assetto_corsa)),
        DropdownOptionUi(id = "ams2", label = stringResource(Res.string.dropdown_preview_automobilista_2)),
        DropdownOptionUi(id = "rf2", label = stringResource(Res.string.dropdown_preview_rfactor_2)),
        DropdownOptionUi(id = "iracing", label = stringResource(Res.string.dropdown_preview_iracing)),
        DropdownOptionUi(id = "lmuu", label = stringResource(Res.string.dropdown_preview_le_mans_ultimate)),
        DropdownOptionUi(id = "ea-wrc", label = stringResource(Res.string.dropdown_preview_ea_sports_wrc)),
        DropdownOptionUi(id = "f1", label = stringResource(Res.string.dropdown_preview_f1_24)),
    ),
)
