package com.analyzer.session.details.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.analyzer.session.details.presentation.components.SessionDetailsHeader
import com.analyzer.session.details.presentation.components.SessionDetailsLapTable
import com.analyzer.session.details.presentation.components.SessionDetailsStatsRow
import com.analyzer.session.details.presentation.model.DropdownFilterUi
import com.analyzer.session.details.presentation.model.DropdownOptionUi
import com.analyzer.session.details.presentation.model.LapStatus
import com.analyzer.session.details.presentation.model.SessionDetailHeaderUi
import com.analyzer.session.details.presentation.model.SessionDetailIntent
import com.analyzer.session.details.presentation.model.SessionDetailState
import com.analyzer.session.details.presentation.model.SessionDetailStatsUi
import com.analyzer.session.details.presentation.model.SessionLapRowUi
import com.project.analyzer.theme.SimAnalyzerTheme
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
internal fun SessionDetailsScreen(sessionId: Long) {
    val viewModel = metroViewModel<SessionDetailViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(sessionId) {
        viewModel.setSession(sessionId)
    }

    SessionDetailsContent(
        state = state,
        onIntent = viewModel::dispatch,
    )
}

@Composable
internal fun SessionDetailsContent(
    state: SessionDetailState,
    onIntent: (SessionDetailIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SimAnalyzerTheme.material.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SessionDetailsStatsRow(
            stats = state.stats,
            modifier = Modifier.fillMaxWidth(),
        )
        SessionDetailsHeader(
            header = state.header,
            sortFilter = state.sortFilter,
            showFilter = state.showFilter,
            modifier = Modifier.fillMaxWidth(),
            onSortSelect = { onIntent(SessionDetailIntent.ChangeSort(it)) },
            onShowSelect = { onIntent(SessionDetailIntent.ChangeFilter(it)) },
        )
        SessionDetailsLapTable(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            onPageChange = { onIntent(SessionDetailIntent.ChangePage(it)) },
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
    val sortOptions = listOf(
        DropdownOptionUi(id = "lap", label = "Lap"),
        DropdownOptionUi(id = "best", label = "Best lap"),
    )
    val showOptions = listOf(
        DropdownOptionUi(id = "all", label = "All laps"),
        DropdownOptionUi(id = "valid", label = "Valid laps"),
        DropdownOptionUi(id = "invalid", label = "Invalid laps"),
    )

    return SessionDetailState(
        isLoading = false,
        header = SessionDetailHeaderUi(
            title = "Session",
            subtitle = "",
            chips = listOf(
                "Air: 00°C / Track: 00°C",
                "Car Name",
                "Location",
            ),
        ),
        stats = SessionDetailStatsUi(
            bestLapLabel = "0:00.000",
            averageLapLabel = "0:00.000",
            incidentsCount = 0,
        ),
        sortFilter = DropdownFilterUi(
            label = "Sort by",
            selectedId = "lap",
            selectedLabel = "Lap",
            options = sortOptions,
        ),
        showFilter = DropdownFilterUi(
            label = "Show",
            selectedId = "all",
            selectedLabel = "All laps",
            options = showOptions,
        ),
        page = 1,
        pageCount = 4,
        visibleLaps = previewLaps(),
    )
}

private fun previewLaps(): List<SessionLapRowUi> = listOf(
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
