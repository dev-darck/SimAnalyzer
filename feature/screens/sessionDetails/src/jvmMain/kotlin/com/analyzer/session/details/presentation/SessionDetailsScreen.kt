package com.analyzer.session.details.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.analyzer.session.details.presentation.components.SessionDetailsHeader
import com.analyzer.session.details.presentation.components.SessionDetailsLapTable
import com.analyzer.session.details.presentation.components.SessionDetailsStatsRow
import com.analyzer.session.details.presentation.model.LapStatus
import com.analyzer.session.details.presentation.model.SessionDetailFilterKind
import com.analyzer.session.details.presentation.model.SessionDetailFilterOptionUi
import com.analyzer.session.details.presentation.model.SessionDetailFilterUiModel
import com.analyzer.session.details.presentation.model.SessionDetailHeaderUi
import com.analyzer.session.details.presentation.model.SessionDetailIntent
import com.analyzer.session.details.presentation.model.SessionDetailState
import com.analyzer.session.details.presentation.model.SessionDetailStatsUi
import com.analyzer.session.details.presentation.model.SessionLapRowUi
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.components.ScrollableScreenColumn
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun SessionDetailsScreen(sessionId: Long) {
    val viewModel = metroViewModel<SessionDetailViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(sessionId) {
        viewModel.dispatch(SessionDetailIntent.BindSession(sessionId))
    }

    SessionDetailsContent(
        state = state,
        onIntent = viewModel::dispatch,
    )
}

@Composable
internal fun SessionDetailsContent(
    state: SessionDetailState,
    modifier: Modifier = Modifier,
    onIntent: (SessionDetailIntent) -> Unit = {},
) {
    ScrollableScreenColumn(modifier = modifier) {
        SessionDetailsStatsRow(
            stats = state.stats,
            modifier = Modifier.fillMaxWidth(),
        )
        SessionDetailsHeader(
            header = state.header,
            sortFilter = state.sortFilter,
            showFilter = state.showFilter,
            sessionTypeFilter = state.sessionTypeFilter,
            modifier = Modifier.fillMaxWidth(),
            onSortSelect = { onIntent(SessionDetailIntent.ChangeSort(it)) },
            onShowSelect = { onIntent(SessionDetailIntent.ChangeFilter(it)) },
            onSessionTypeSelect = { onIntent(SessionDetailIntent.ChangeSessionTypeFilter(it)) },
        )
        SessionDetailsLapTable(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            onPageChange = { onIntent(SessionDetailIntent.ChangePage(it)) },
            onSortChange = { onIntent(SessionDetailIntent.ChangeSort(it)) },
        )
    }
}

@Preview
@Composable
private fun SessionDetailsContentPreview() {
    SimAnalyzerTheme {
        SessionDetailsContent(
            state = previewState(),
            onIntent = {},
        )
    }
}

private fun previewState(): SessionDetailState {
    val sortOptions = persistentListOf(
        SessionDetailFilterOptionUi(id = "lap"),
        SessionDetailFilterOptionUi(id = "best"),
    )
    val showOptions = persistentListOf(
        SessionDetailFilterOptionUi(id = "all"),
        SessionDetailFilterOptionUi(id = "valid"),
        SessionDetailFilterOptionUi(id = "invalid"),
    )
    val typeOptions = persistentListOf(
        SessionDetailFilterOptionUi(id = "all_session_types"),
        SessionDetailFilterOptionUi(id = "practice", label = "Practice"),
        SessionDetailFilterOptionUi(id = "qualifying", label = "Qualifying"),
        SessionDetailFilterOptionUi(id = "race", label = "Race"),
    )

    return SessionDetailState(
        isLoading = false,
        header = SessionDetailHeaderUi(
            subtitle = "",
            sessionTypeLabel = "Qualifying",
            airTempLabel = "00°C",
            trackTempLabel = "00°C",
            carLabel = "Car Name",
            trackLabel = "Location",
        ),
        stats = SessionDetailStatsUi(
            bestLapLabel = "0:00.000",
            averageLapLabel = "0:00.000",
            incidentsCount = 0,
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
            selectedId = "race",
            options = typeOptions,
        ),
        page = 1,
        pageCount = 4,
        visibleLaps = previewLaps(),
    )
}

private fun previewLaps(): ImmutableList<SessionLapRowUi> = persistentListOf(
    lap(
        number = 1,
        total = "0:00.000",
        s1 = "00.000",
        s2 = "00.000",
        s3 = "00.000",
        incidents = "0",
        delta = "-0.000",
        status = LapStatus.Clean,
    ),
    lap(
        number = 2,
        total = "0:00.000",
        s1 = "00.000",
        s2 = "00.000",
        s3 = "00.000",
        incidents = "0",
        delta = "-0.000",
        status = LapStatus.OutLap,
    ),
    lap(
        number = 5,
        total = "0:00.000",
        s1 = "00.000",
        s2 = "00.000",
        s3 = "00.000",
        incidents = "0",
        delta = "-0.000",
        status = LapStatus.BestLap,
    ),
    lap(
        number = 7,
        total = "0:00.000",
        s1 = "00.000",
        s2 = "00.000",
        s3 = "00.000",
        incidents = "0",
        delta = "+0.000",
        status = LapStatus.Dirty,
    ),
    lap(
        number = 8,
        total = "0:00.000",
        s1 = "00.000",
        s2 = "00.000",
        s3 = "00.000",
        incidents = "1",
        delta = "--",
        status = LapStatus.Invalid,
    ),
    lap(
        number = 9,
        total = "0:00.000",
        s1 = "00.000",
        s2 = "00.000",
        s3 = "00.000",
        incidents = "0",
        delta = "-0.000",
        status = LapStatus.PitIn,
    ),
)

private fun lap(
    number: Int,
    total: String,
    s1: String,
    s2: String,
    s3: String,
    incidents: String,
    delta: String,
    status: LapStatus,
): SessionLapRowUi = SessionLapRowUi(
    lapNumber = number,
    lapLabel = number.toString(),
    sessionTypeLabel = "Qualifying",
    totalTimeMs = null,
    totalTime = total,
    s1 = s1,
    s2 = s2,
    s3 = s3,
    incidents = incidents,
    delta = delta,
    deltaIsPositive = delta.startsWith("+"),
    status = status,
)
