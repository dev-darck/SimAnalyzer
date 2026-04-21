package com.analyzer.session.details.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.analyzer.session.details.presentation.model.SessionDetailFilterKind
import com.analyzer.session.details.presentation.model.SessionDetailFilterOptionUi
import com.analyzer.session.details.presentation.model.SessionDetailFilterUiModel
import com.analyzer.session.details.presentation.model.SessionDetailHeaderUi
import com.project.analyzer.feature.screens.sessionDetails.Res.Res
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_action_analysis
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_chip_air_prefix
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_chip_track_prefix
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_chip_type_prefix
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.components.Button
import com.project.analyzer.ui.components.DropdownFilterUi
import com.project.analyzer.ui.components.FilterDropdown
import com.project.analyzer.ui.components.FilterDropdownStyle
import com.project.analyzer.ui.components.ResponsivePanelCard
import com.project.analyzer.ui.components.SimAnalyzerButtonSize
import com.project.analyzer.ui.components.SimAnalyzerButtonVariant
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SessionDetailsHeader(
    header: SessionDetailHeaderUi,
    sortFilter: SessionDetailFilterUiModel,
    showFilter: SessionDetailFilterUiModel,
    sessionTypeFilter: SessionDetailFilterUiModel,
    onSortSelect: (String) -> Unit,
    onShowSelect: (String) -> Unit,
    onSessionTypeSelect: (String) -> Unit,
    onAnalysisClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ResponsivePanelCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        val isCompactLayout = maxWidth < SESSION_DETAILS_HEADER_COMPACT_BREAKPOINT
        val sortDropdown = sortFilter.asDropdownFilter()
        val showDropdown = showFilter.asDropdownFilter()
        val sessionTypeDropdown = sessionTypeFilter.asDropdownFilter()
        val analysisLabel = stringResource(Res.string.session_details_action_analysis)

        if (isCompactLayout) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SessionDetailsInfoRow(
                    header = header,
                )
                SessionDetailsFiltersRow(
                    sortFilter = sortDropdown,
                    showFilter = showDropdown,
                    sessionTypeFilter = sessionTypeDropdown,
                    analysisLabel = analysisLabel,
                    onSortSelect = onSortSelect,
                    onShowSelect = onShowSelect,
                    onSessionTypeSelect = onSessionTypeSelect,
                    onAnalysisClick = onAnalysisClick,
                    modifier = Modifier.fillMaxWidth(),
                    compact = true,
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SessionDetailsInfoRow(
                    header = header,
                    modifier = Modifier.weight(1f),
                )
                SessionDetailsFiltersRow(
                    sortFilter = sortDropdown,
                    showFilter = showDropdown,
                    sessionTypeFilter = sessionTypeDropdown,
                    analysisLabel = analysisLabel,
                    onSortSelect = onSortSelect,
                    onShowSelect = onShowSelect,
                    onSessionTypeSelect = onSessionTypeSelect,
                    onAnalysisClick = onAnalysisClick,
                    compact = false,
                )
            }
        }
    }
}

private val SESSION_DETAILS_HEADER_COMPACT_BREAKPOINT = 920.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionDetailsInfoRow(header: SessionDetailHeaderUi, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SessionInfoItem(
            icon = Icons.Filled.Flag,
            text = "${stringResource(Res.string.session_details_chip_type_prefix)} ${header.sessionTypeLabel}",
        )
        SessionInfoItem(
            icon = Icons.Filled.DeviceThermostat,
            text = "${stringResource(Res.string.session_details_chip_air_prefix)} ${header.airTempLabel} / " +
                "${stringResource(Res.string.session_details_chip_track_prefix)} ${header.trackTempLabel}",
        )
        if (header.carLabel.isNotBlank()) {
            SessionInfoItem(
                icon = Icons.Filled.DirectionsCar,
                text = header.carLabel,
            )
        }
        if (header.trackLabel.isNotBlank()) {
            SessionInfoItem(
                icon = Icons.Filled.LocationOn,
                text = header.trackLabel,
            )
        }
    }
}

@Composable
private fun SessionInfoItem(icon: ImageVector, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SimAnalyzerTheme.material.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = text,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionDetailsFiltersRow(
    sortFilter: DropdownFilterUi,
    showFilter: DropdownFilterUi,
    sessionTypeFilter: DropdownFilterUi,
    analysisLabel: String,
    onSortSelect: (String) -> Unit,
    onShowSelect: (String) -> Unit,
    onSessionTypeSelect: (String) -> Unit,
    onAnalysisClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean,
) {
    if (compact) {
        FlowRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterDropdown(
                filter = sortFilter,
                onSelect = onSortSelect,
                style = FilterDropdownStyle.Inline,
            )
            FilterDropdown(
                filter = showFilter,
                onSelect = onShowSelect,
                style = FilterDropdownStyle.Inline,
            )
            FilterDropdown(
                filter = sessionTypeFilter,
                onSelect = onSessionTypeSelect,
                style = FilterDropdownStyle.Inline,
            )
            Button(
                text = analysisLabel,
                onClick = onAnalysisClick,
                variant = SimAnalyzerButtonVariant.Outline,
                size = SimAnalyzerButtonSize.Compact,
                leadingIcon = Icons.Filled.Timeline,
            )
        }
        return
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterDropdown(
            filter = sortFilter,
            onSelect = onSortSelect,
            style = FilterDropdownStyle.Inline,
        )
        FilterDropdown(
            filter = showFilter,
            onSelect = onShowSelect,
            style = FilterDropdownStyle.Inline,
        )
        FilterDropdown(
            filter = sessionTypeFilter,
            onSelect = onSessionTypeSelect,
            style = FilterDropdownStyle.Inline,
        )
        Button(
            text = analysisLabel,
            onClick = onAnalysisClick,
            variant = SimAnalyzerButtonVariant.Outline,
            size = SimAnalyzerButtonSize.Compact,
            leadingIcon = Icons.Filled.Timeline,
        )
    }
}

@Preview
@Composable
private fun SessionDetailsHeaderPreview() {
    val sortOptions = persistentListOf(
        SessionDetailFilterOptionUi(id = "lap"),
        SessionDetailFilterOptionUi(id = "best"),
    )
    val showOptions = persistentListOf(
        SessionDetailFilterOptionUi(id = "all"),
        SessionDetailFilterOptionUi(id = "valid"),
        SessionDetailFilterOptionUi(id = "invalid"),
    )

    SimAnalyzerTheme {
        SessionDetailsHeader(
            header = SessionDetailHeaderUi(
                subtitle = "Jan 18, 2026, 22:50",
                sessionTypeLabel = "Qualifying",
                airTempLabel = "00°C",
                trackTempLabel = "00°C",
                carLabel = "Car Name",
                trackLabel = "Location",
            ),
            sortFilter = SessionDetailFilterUiModel(
                kind = SessionDetailFilterKind.Sort,
                selectedId = "lap",
                options = sortOptions,
            ),
            showFilter = SessionDetailFilterUiModel(
                kind = SessionDetailFilterKind.Show,
                selectedId = "all",
                options = showOptions,
            ),
            sessionTypeFilter = SessionDetailFilterUiModel(
                kind = SessionDetailFilterKind.SessionType,
                selectedId = "all_session_types",
                options = persistentListOf(
                    SessionDetailFilterOptionUi(id = "all_session_types"),
                    SessionDetailFilterOptionUi(id = "qualifying", label = "Qualifying"),
                    SessionDetailFilterOptionUi(id = "race", label = "Race"),
                ),
            ),
            onSortSelect = {},
            onShowSelect = {},
            onSessionTypeSelect = {},
            onAnalysisClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
