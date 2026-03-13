package com.analyzer.session.details.presentation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
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
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_chip_air_prefix
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_chip_track_prefix
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_chip_type_prefix
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_title
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.components.FilterDropdown
import com.project.analyzer.ui.components.ResponsivePanelCard
import com.project.analyzer.ui.scrollbar.AppHorizontalScrollbar
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
    modifier: Modifier = Modifier,
) {
    ResponsivePanelCard(modifier = modifier.fillMaxWidth()) {
        val availableWidth = maxWidth
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SessionDetailsTitleBlock(header = header)
            if (availableWidth < SESSION_DETAILS_HEADER_COMPACT_BREAKPOINT) {
                SessionDetailsInfoRow(header = header)
                SessionDetailsFiltersRow(
                    sortFilter = sortFilter,
                    showFilter = showFilter,
                    sessionTypeFilter = sessionTypeFilter,
                    onSortSelect = onSortSelect,
                    onShowSelect = onShowSelect,
                    onSessionTypeSelect = onSessionTypeSelect,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SessionDetailsInfoRow(
                        header = header,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    SessionDetailsFiltersRow(
                        sortFilter = sortFilter,
                        showFilter = showFilter,
                        sessionTypeFilter = sessionTypeFilter,
                        onSortSelect = onSortSelect,
                        onShowSelect = onShowSelect,
                        onSessionTypeSelect = onSessionTypeSelect,
                    )
                }
            }
        }
    }
}

private val SESSION_DETAILS_HEADER_COMPACT_BREAKPOINT = 960.dp

@Composable
private fun SessionDetailsTitleBlock(header: SessionDetailHeaderUi) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(Res.string.session_details_title),
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (header.subtitle.isNotBlank()) {
            Text(
                text = header.subtitle,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SessionDetailsInfoRow(header: SessionDetailHeaderUi, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
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
                SessionInfoItem(icon = Icons.Filled.DirectionsCar, text = header.carLabel)
            }
            if (header.trackLabel.isNotBlank()) {
                SessionInfoItem(icon = Icons.Filled.LocationOn, text = header.trackLabel)
            }
        }
        AppHorizontalScrollbar(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(),
            adapter = rememberScrollbarAdapter(scrollState),
        )
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

@Composable
private fun SessionDetailsFiltersRow(
    sortFilter: SessionDetailFilterUiModel,
    showFilter: SessionDetailFilterUiModel,
    sessionTypeFilter: SessionDetailFilterUiModel,
    onSortSelect: (String) -> Unit,
    onShowSelect: (String) -> Unit,
    onSessionTypeSelect: (String) -> Unit,
) {
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterDropdown(
                filter = sortFilter.asDropdownFilter(),
                onSelect = onSortSelect,
            )
            FilterDropdown(
                filter = showFilter.asDropdownFilter(),
                onSelect = onShowSelect,
            )
            FilterDropdown(
                filter = sessionTypeFilter.asDropdownFilter(),
                onSelect = onSessionTypeSelect,
            )
        }
        AppHorizontalScrollbar(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(),
            adapter = rememberScrollbarAdapter(scrollState),
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
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
