package com.analyzer.session.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.analyzer.session.presentation.components.SessionScreenHeader
import com.analyzer.session.presentation.components.SessionScreenStatsRow
import com.analyzer.session.presentation.components.SessionScreenTable
import com.analyzer.session.presentation.model.DropdownFilterUi
import com.analyzer.session.presentation.model.DropdownOptionUi
import com.analyzer.session.presentation.model.FILTER_ALL_ID
import com.analyzer.session.presentation.model.SessionListIntent
import com.analyzer.session.presentation.model.SessionListState
import com.analyzer.session.presentation.model.SessionRowUi
import com.analyzer.session.presentation.model.SessionStatsUi
import com.project.analyzer.navigation.api.LocalNavigator
import com.project.analyzer.navigation.api.Route
import com.project.analyzer.theme.SimAnalyzerTheme
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
internal fun SessionScreen() {
    val viewModel = metroViewModel<SessionListViewModel>()
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    SessionListContent(
        state = state,
        onIntent = viewModel::dispatch,
        onOpenDetails = { sessionId ->
            navigator.navigate(Route.SessionRoot.SessionDetails(sessionId))
        }
    )
}

@Composable
internal fun SessionListContent(
    state: SessionListState,
    onIntent: (SessionListIntent) -> Unit,
    modifier: Modifier = Modifier,
    onOpenDetails: (Long) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SimAnalyzerTheme.material.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SessionScreenStatsRow(
            stats = state.stats,
            modifier = Modifier.fillMaxWidth()
        )
        SessionScreenHeader(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            onIntent = onIntent
        )
        SessionScreenTable(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            onOpenDetails = onOpenDetails,
            onIntent = onIntent
        )
    }
}

@Preview
@Composable
private fun SessionListContentPreview() {
    SimAnalyzerTheme {
        SessionListContent(
            state = previewState(),
            onIntent = {}
        )
    }
}

private fun previewState(): SessionListState {
    return SessionListState(
        isLoading = false,
        stats = SessionStatsUi(
            totalDistanceLabel = "0.000",
            sessionsCount = 8,
            incidentsCount = 0,
            favoriteCar = "Car Name"
        ),
        gameFilter = DropdownFilterUi(
            label = "Game",
            selectedId = FILTER_ALL_ID,
            selectedLabel = "All",
            options = listOf(
                DropdownOptionUi(FILTER_ALL_ID, "All"),
                DropdownOptionUi("acc", "ACC")
            )
        ),
        trackFilter = DropdownFilterUi(
            label = "Track",
            selectedId = FILTER_ALL_ID,
            selectedLabel = "All",
            options = listOf(
                DropdownOptionUi(FILTER_ALL_ID, "All"),
                DropdownOptionUi("spa", "Spa")
            )
        ),
        carFilter = DropdownFilterUi(
            label = "Car",
            selectedId = FILTER_ALL_ID,
            selectedLabel = "All",
            options = listOf(
                DropdownOptionUi(FILTER_ALL_ID, "All"),
                DropdownOptionUi("car_name", "Car Name")
            )
        ),
        dateFilter = DropdownFilterUi(
            label = "Date",
            selectedId = FILTER_ALL_ID,
            selectedLabel = "All",
            options = listOf(
                DropdownOptionUi(FILTER_ALL_ID, "All"),
                DropdownOptionUi("oct_2025", "Oct 2025")
            )
        ),
        sortFilter = DropdownFilterUi(
            label = "Sort by",
            selectedId = "best",
            selectedLabel = "Best lap",
            options = listOf(
                DropdownOptionUi("best", "Best lap"),
                DropdownOptionUi("latest", "Latest")
            )
        ),
        page = 1,
        pageCount = 9,
        sessions = previewSessions(),
        visibleSessions = previewSessions()
    )
}

private fun previewSessions(): List<SessionRowUi> {
    return List(size = 8) { index ->
        SessionRowUi(
            sessionId = (index + 1).toLong(),
            dateLabel = "Oct 24, 2025",
            timeLabel = "20:40",
            gameLabel = "ACC",
            trackLabel = "Location",
            carLabel = "Car Name",
            lapsLabel = "0",
            bestLapLabel = "0:00.000",
            isSaved = index % 3 == 0
        )
    }
}
