package com.analyzer.session.details.presentation.components

import androidx.compose.runtime.Composable
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SHOW_ALL
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SHOW_INVALID
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SHOW_PIT
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SHOW_VALID
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_BEST
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_DELTA
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_DELTA_DESC
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_INCIDENTS
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_INCIDENTS_DESC
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_LAP
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_LAP_DESC
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_S1
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_S1_DESC
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_S2
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_S2_DESC
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_S3
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_S3_DESC
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_STATUS
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_STATUS_DESC
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_TOTAL_DESC
import com.analyzer.session.details.domain.model.SESSION_DETAIL_TYPE_ALL
import com.analyzer.session.details.presentation.model.SessionDetailFilterKind
import com.analyzer.session.details.presentation.model.SessionDetailFilterOptionUi
import com.analyzer.session.details.presentation.model.SessionDetailFilterUiModel
import com.project.analyzer.feature.screens.sessionDetails.Res.*
import com.project.analyzer.ui.components.DropdownFilterUi
import com.project.analyzer.ui.components.DropdownOptionUi
import org.jetbrains.compose.resources.stringResource
import java.util.Locale

@Composable
internal fun SessionDetailFilterUiModel.asDropdownFilter(): DropdownFilterUi {
    val dropdownOptions = options.map { option ->
        DropdownOptionUi(
            id = option.id,
            label = sessionDetailFilterOptionLabel(kind = kind, option = option),
        )
    }
    val selectedLabel = dropdownOptions.firstOrNull { it.id == selectedId }?.label
        ?: dropdownOptions.firstOrNull()?.label
        ?: sessionDetailFilterLabel(kind)

    return DropdownFilterUi(
        label = sessionDetailFilterLabel(kind),
        selectedId = selectedId,
        selectedLabel = selectedLabel,
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
private fun sessionDetailFilterOptionLabel(kind: SessionDetailFilterKind, option: SessionDetailFilterOptionUi): String =
    when {
        kind == SessionDetailFilterKind.Sort -> sessionDetailSortLabel(option.id)
        kind == SessionDetailFilterKind.Show -> sessionDetailShowLabel(option.id)
        option.id == SESSION_DETAIL_TYPE_ALL -> stringResource(Res.string.session_details_filter_all_session_types)
        !option.label.isNullOrBlank() -> option.label
        else -> option.id
    }

@Composable
internal fun sessionDetailSortLabel(sortId: String): String = when (sortId) {
    SESSION_DETAIL_SORT_LAP -> stringResource(Res.string.session_details_sort_lap_asc)
    SESSION_DETAIL_SORT_LAP_DESC -> stringResource(Res.string.session_details_sort_lap_desc)
    SESSION_DETAIL_SORT_BEST -> stringResource(Res.string.session_details_sort_total_asc)
    SESSION_DETAIL_SORT_TOTAL_DESC -> stringResource(Res.string.session_details_sort_total_desc)
    SESSION_DETAIL_SORT_S1 -> stringResource(Res.string.session_details_sort_s1_asc)
    SESSION_DETAIL_SORT_S1_DESC -> stringResource(Res.string.session_details_sort_s1_desc)
    SESSION_DETAIL_SORT_S2 -> stringResource(Res.string.session_details_sort_s2_asc)
    SESSION_DETAIL_SORT_S2_DESC -> stringResource(Res.string.session_details_sort_s2_desc)
    SESSION_DETAIL_SORT_S3 -> stringResource(Res.string.session_details_sort_s3_asc)
    SESSION_DETAIL_SORT_S3_DESC -> stringResource(Res.string.session_details_sort_s3_desc)
    SESSION_DETAIL_SORT_INCIDENTS -> stringResource(Res.string.session_details_sort_incidents_asc)
    SESSION_DETAIL_SORT_INCIDENTS_DESC -> stringResource(Res.string.session_details_sort_incidents_desc)
    SESSION_DETAIL_SORT_DELTA -> stringResource(Res.string.session_details_sort_delta_asc)
    SESSION_DETAIL_SORT_DELTA_DESC -> stringResource(Res.string.session_details_sort_delta_desc)
    SESSION_DETAIL_SORT_STATUS -> stringResource(Res.string.session_details_sort_status_asc)
    SESSION_DETAIL_SORT_STATUS_DESC -> stringResource(Res.string.session_details_sort_status_desc)
    else -> sortId
}

@Composable
internal fun sessionDetailShowLabel(showId: String): String = when (showId) {
    SESSION_DETAIL_SHOW_ALL -> stringResource(Res.string.session_details_filter_all_laps)
    SESSION_DETAIL_SHOW_VALID -> stringResource(Res.string.session_details_filter_valid_laps)
    SESSION_DETAIL_SHOW_INVALID -> stringResource(Res.string.session_details_filter_invalid_laps)
    SESSION_DETAIL_SHOW_PIT -> stringResource(Res.string.session_details_filter_pit_laps)
    else -> showId
}

@Composable
internal fun sessionTypeDisplayLabel(sessionTypeId: String, fallbackLabel: String? = null): String {
    if (!fallbackLabel.isNullOrBlank()) return fallbackLabel
    if (sessionTypeId == SESSION_DETAIL_TYPE_ALL) {
        return stringResource(Res.string.session_details_filter_all_session_types)
    }
    return sessionTypeId
        .replace('_', ' ')
        .split(' ')
        .joinToString(" ") { part ->
            part.replaceFirstChar { char -> char.titlecase(Locale.US) }
        }
}
