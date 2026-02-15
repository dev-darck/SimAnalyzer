package com.analyzer.session.details.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.analyzer.session.details.presentation.model.DropdownFilterUi
import com.analyzer.session.details.presentation.model.DropdownOptionUi
import com.analyzer.session.details.presentation.model.SessionDetailHeaderUi
import com.project.analyzer.theme.SimAnalyzerTheme

@Composable
internal fun SessionDetailsHeader(
    header: SessionDetailHeaderUi,
    sortFilter: DropdownFilterUi,
    showFilter: DropdownFilterUi,
    onSortSelect: (String) -> Unit,
    onShowSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.shapes.large)
            .background(SimAnalyzerTheme.material.surface)
            .border(
                width = 1.dp,
                color = SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.25f),
                shape = SimAnalyzerTheme.shapes.large
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (maxWidth < 960.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SessionDetailsInfoRow(chips = header.chips)
                SessionDetailsFiltersRow(
                    sortFilter = sortFilter,
                    showFilter = showFilter,
                    onSortSelect = onSortSelect,
                    onShowSelect = onShowSelect
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SessionDetailsInfoRow(
                    chips = header.chips,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                SessionDetailsFiltersRow(
                    sortFilter = sortFilter,
                    showFilter = showFilter,
                    onSortSelect = onSortSelect,
                    onShowSelect = onShowSelect
                )
            }
        }
    }
}

@Composable
private fun SessionDetailsInfoRow(
    chips: List<String>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        chips.forEachIndexed { index, chip ->
            SessionInfoItem(
                icon = when (index) {
                    0 -> Icons.Filled.DeviceThermostat
                    1 -> Icons.Filled.DirectionsCar
                    else -> Icons.Filled.LocationOn
                },
                text = chip
            )
        }
    }
}

@Composable
private fun SessionInfoItem(
    icon: ImageVector,
    text: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SimAnalyzerTheme.material.onSurfaceVariant
        )
        Text(
            text = text,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SessionDetailsFiltersRow(
    sortFilter: DropdownFilterUi,
    showFilter: DropdownFilterUi,
    onSortSelect: (String) -> Unit,
    onShowSelect: (String) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterDropdown(
            filter = sortFilter,
            onSelect = onSortSelect
        )
        FilterDropdown(
            filter = showFilter,
            onSelect = onShowSelect
        )
    }
}

@Preview
@Composable
private fun SessionDetailsHeaderPreview() {
    val sortOptions = listOf(
        DropdownOptionUi(id = "lap", label = "Lap"),
        DropdownOptionUi(id = "best", label = "Best lap")
    )
    val showOptions = listOf(
        DropdownOptionUi(id = "all", label = "All laps"),
        DropdownOptionUi(id = "valid", label = "Valid laps"),
        DropdownOptionUi(id = "invalid", label = "Invalid laps")
    )

    SimAnalyzerTheme {
        SessionDetailsHeader(
            header = SessionDetailHeaderUi(
                title = "Session",
                subtitle = "Jan 18, 2026, 22:50",
                chips = listOf("Air: 00°C / Track: 00°C", "Car Name", "Location")
            ),
            sortFilter = DropdownFilterUi(
                label = "Sort by",
                selectedId = "lap",
                selectedLabel = "Lap",
                options = sortOptions
            ),
            showFilter = DropdownFilterUi(
                label = "Show",
                selectedId = "all",
                selectedLabel = "All laps",
                options = showOptions
            ),
            onSortSelect = {},
            onShowSelect = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}
