package com.analyzer.session.details.presentation.components

import androidx.compose.runtime.Composable
import com.analyzer.session.details.presentation.model.SessionDetailFilterIdsUi
import com.analyzer.session.details.presentation.model.SessionDetailFilterKind
import com.analyzer.session.details.presentation.model.SessionDetailFilterOptionUi
import com.analyzer.session.details.presentation.model.SessionDetailFilterUiModel
import com.project.analyzer.feature.screens.sessionDetails.Res.Res
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_filter_all_laps
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_filter_all_session_types
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_filter_invalid_laps
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_filter_pit_laps
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_filter_session_type
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_filter_show
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_filter_sort
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_filter_valid_laps
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_sort_delta_asc
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_sort_delta_desc
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_sort_incidents_asc
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_sort_incidents_desc
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_sort_lap_asc
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_sort_lap_desc
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_sort_s1_asc
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_sort_s1_desc
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_sort_s2_asc
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_sort_s2_desc
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_sort_s3_asc
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_sort_s3_desc
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_sort_status_asc
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_sort_status_desc
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_sort_total_asc
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_sort_total_desc
import com.project.analyzer.ui.components.DropdownFilterUi
import com.project.analyzer.ui.components.DropdownOptionUi
import com.project.analyzer.ui.components.buildDropdownFilterUi
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SessionDetailFilterUiModel.asDropdownFilter(): DropdownFilterUi {
    val dropdownOptions = options.map { option ->
        DropdownOptionUi(
            id = option.id,
            label = sessionDetailFilterOptionLabel(kind = kind, option = option),
        )
    }
    return buildDropdownFilterUi(
        label = sessionDetailFilterLabel(kind),
        selectedId = selectedId,
        options = dropdownOptions,
    )
}

@Composable
internal fun sessionDetailFilterLabel(kind: SessionDetailFilterKind): String = when (kind) {
    SessionDetailFilterKind.Sort -> stringResource(Res.string.session_details_filter_sort)
    SessionDetailFilterKind.Show -> stringResource(Res.string.session_details_filter_show)
    SessionDetailFilterKind.SessionType -> stringResource(Res.string.session_details_filter_session_type)
}

@Composable
private fun sessionDetailFilterOptionLabel(
    kind: SessionDetailFilterKind,
    option: SessionDetailFilterOptionUi,
): String = when {
    kind == SessionDetailFilterKind.Sort -> sessionDetailSortLabel(option.id)

    kind == SessionDetailFilterKind.Show -> sessionDetailShowLabel(option.id)

    option.id == SessionDetailFilterIdsUi.SessionTypeAll -> stringResource(
        Res.string.session_details_filter_all_session_types,
    )

    !option.label.isNullOrBlank() -> option.label

    else -> option.id
}

@Composable
internal fun sessionDetailSortLabel(sortId: String): String = when (sortId) {
    SessionDetailFilterIdsUi.SortLap -> stringResource(Res.string.session_details_sort_lap_asc)
    SessionDetailFilterIdsUi.SortLapDesc -> stringResource(Res.string.session_details_sort_lap_desc)
    SessionDetailFilterIdsUi.SortBest -> stringResource(Res.string.session_details_sort_total_asc)
    SessionDetailFilterIdsUi.SortTotalDesc -> stringResource(Res.string.session_details_sort_total_desc)
    SessionDetailFilterIdsUi.SortS1 -> stringResource(Res.string.session_details_sort_s1_asc)
    SessionDetailFilterIdsUi.SortS1Desc -> stringResource(Res.string.session_details_sort_s1_desc)
    SessionDetailFilterIdsUi.SortS2 -> stringResource(Res.string.session_details_sort_s2_asc)
    SessionDetailFilterIdsUi.SortS2Desc -> stringResource(Res.string.session_details_sort_s2_desc)
    SessionDetailFilterIdsUi.SortS3 -> stringResource(Res.string.session_details_sort_s3_asc)
    SessionDetailFilterIdsUi.SortS3Desc -> stringResource(Res.string.session_details_sort_s3_desc)
    SessionDetailFilterIdsUi.SortIncidents -> stringResource(Res.string.session_details_sort_incidents_asc)
    SessionDetailFilterIdsUi.SortIncidentsDesc -> stringResource(Res.string.session_details_sort_incidents_desc)
    SessionDetailFilterIdsUi.SortDelta -> stringResource(Res.string.session_details_sort_delta_asc)
    SessionDetailFilterIdsUi.SortDeltaDesc -> stringResource(Res.string.session_details_sort_delta_desc)
    SessionDetailFilterIdsUi.SortStatus -> stringResource(Res.string.session_details_sort_status_asc)
    SessionDetailFilterIdsUi.SortStatusDesc -> stringResource(Res.string.session_details_sort_status_desc)
    else -> sortId
}

@Composable
internal fun sessionDetailShowLabel(showId: String): String = when (showId) {
    SessionDetailFilterIdsUi.ShowAll -> stringResource(Res.string.session_details_filter_all_laps)
    SessionDetailFilterIdsUi.ShowValid -> stringResource(Res.string.session_details_filter_valid_laps)
    SessionDetailFilterIdsUi.ShowInvalid -> stringResource(Res.string.session_details_filter_invalid_laps)
    SessionDetailFilterIdsUi.ShowPit -> stringResource(Res.string.session_details_filter_pit_laps)
    else -> showId
}
