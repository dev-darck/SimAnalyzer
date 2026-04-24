package com.analyzer.session.presentation.components

import androidx.compose.runtime.Composable
import com.analyzer.session.presentation.model.FILTER_ALL_ID
import com.analyzer.session.presentation.model.SessionFilterKind
import com.analyzer.session.presentation.model.SessionFilterOptionUi
import com.analyzer.session.presentation.model.SessionFilterUiModel
import com.analyzer.session.presentation.model.SessionListSortIdsUi
import com.project.analyzer.feature.screens.session.impl.Res.Res
import com.project.analyzer.feature.screens.session.impl.Res.session_filter_all
import com.project.analyzer.feature.screens.session.impl.Res.session_filter_car
import com.project.analyzer.feature.screens.session.impl.Res.session_filter_date
import com.project.analyzer.feature.screens.session.impl.Res.session_filter_game
import com.project.analyzer.feature.screens.session.impl.Res.session_filter_sort
import com.project.analyzer.feature.screens.session.impl.Res.session_filter_track
import com.project.analyzer.feature.screens.session.impl.Res.session_sort_best_asc
import com.project.analyzer.feature.screens.session.impl.Res.session_sort_best_desc
import com.project.analyzer.feature.screens.session.impl.Res.session_sort_car_asc
import com.project.analyzer.feature.screens.session.impl.Res.session_sort_car_desc
import com.project.analyzer.feature.screens.session.impl.Res.session_sort_game_asc
import com.project.analyzer.feature.screens.session.impl.Res.session_sort_game_desc
import com.project.analyzer.feature.screens.session.impl.Res.session_sort_laps_asc
import com.project.analyzer.feature.screens.session.impl.Res.session_sort_laps_desc
import com.project.analyzer.feature.screens.session.impl.Res.session_sort_newest
import com.project.analyzer.feature.screens.session.impl.Res.session_sort_oldest
import com.project.analyzer.feature.screens.session.impl.Res.session_sort_track_asc
import com.project.analyzer.feature.screens.session.impl.Res.session_sort_track_desc
import com.project.analyzer.ui.components.DropdownFilterUi
import com.project.analyzer.ui.components.DropdownOptionUi
import com.project.analyzer.ui.components.buildDropdownFilterUi
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SessionFilterUiModel.asDropdownFilter(): DropdownFilterUi {
    val dropdownOptions = options.map { option ->
        DropdownOptionUi(
            id = option.id,
            label = sessionFilterOptionLabel(kind = kind, option = option),
        )
    }
    return buildDropdownFilterUi(
        label = sessionFilterLabel(kind),
        selectedId = selectedId,
        options = dropdownOptions,
    )
}

@Composable
internal fun sessionFilterLabel(kind: SessionFilterKind): String = when (kind) {
    SessionFilterKind.Game -> stringResource(Res.string.session_filter_game)
    SessionFilterKind.Track -> stringResource(Res.string.session_filter_track)
    SessionFilterKind.Car -> stringResource(Res.string.session_filter_car)
    SessionFilterKind.Date -> stringResource(Res.string.session_filter_date)
    SessionFilterKind.Sort -> stringResource(Res.string.session_filter_sort)
}

@Composable
private fun sessionFilterOptionLabel(kind: SessionFilterKind, option: SessionFilterOptionUi): String = when {
    option.id == FILTER_ALL_ID -> stringResource(Res.string.session_filter_all)
    kind == SessionFilterKind.Sort -> sessionSortLabel(option.id)
    !option.label.isNullOrBlank() -> option.label
    else -> option.id
}

@Composable
internal fun sessionSortLabel(sortId: String): String = when (sortId) {
    SessionListSortIdsUi.Newest -> stringResource(Res.string.session_sort_newest)
    SessionListSortIdsUi.Oldest -> stringResource(Res.string.session_sort_oldest)
    SessionListSortIdsUi.GameAsc -> stringResource(Res.string.session_sort_game_asc)
    SessionListSortIdsUi.GameDesc -> stringResource(Res.string.session_sort_game_desc)
    SessionListSortIdsUi.TrackAsc -> stringResource(Res.string.session_sort_track_asc)
    SessionListSortIdsUi.TrackDesc -> stringResource(Res.string.session_sort_track_desc)
    SessionListSortIdsUi.CarAsc -> stringResource(Res.string.session_sort_car_asc)
    SessionListSortIdsUi.CarDesc -> stringResource(Res.string.session_sort_car_desc)
    SessionListSortIdsUi.LapsAsc -> stringResource(Res.string.session_sort_laps_asc)
    SessionListSortIdsUi.LapsDesc -> stringResource(Res.string.session_sort_laps_desc)
    SessionListSortIdsUi.Best -> stringResource(Res.string.session_sort_best_asc)
    SessionListSortIdsUi.BestDesc -> stringResource(Res.string.session_sort_best_desc)
    else -> sortId
}
