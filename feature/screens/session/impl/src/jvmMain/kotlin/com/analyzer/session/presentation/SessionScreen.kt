package com.analyzer.session.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.analyzer.session.presentation.components.previewTrackMapData
import com.analyzer.session.presentation.components.SessionScreenHeader
import com.analyzer.session.presentation.components.SessionScreenStatsRow
import com.analyzer.session.presentation.components.SessionScreenTable
import com.analyzer.session.presentation.model.FILTER_ALL_ID
import com.analyzer.session.presentation.model.SessionFilterKind
import com.analyzer.session.presentation.model.SessionFilterOptionUi
import com.analyzer.session.presentation.model.SessionFilterUiModel
import com.analyzer.session.presentation.model.SessionListIntent
import com.analyzer.session.presentation.model.SessionListState
import com.analyzer.session.presentation.model.SessionRowUi
import com.analyzer.session.presentation.model.SessionStatsUi
import com.project.analyzer.navigation.api.LocalNavigator
import com.project.analyzer.navigation.api.Route
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.components.ScrollableScreenColumn
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
internal fun SessionScreen() {
    val viewModel = metroViewModel<SessionListViewModel>()
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val existingSessionIds = remember(state.sessions) {
        state.sessions.mapTo(HashSet(state.sessions.size)) { it.sessionId }
    }

    LaunchedEffect(Unit) {
        viewModel.dispatch(SessionListIntent.Start)
    }

    DisposableEffect(navigator, existingSessionIds) {
        navigator.registerForwardValidator(Route.SessionRoot.SessionDetails::class) { route ->
            val detailsRoute = route as? Route.SessionRoot.SessionDetails ?: return@registerForwardValidator true
            existingSessionIds.contains(detailsRoute.sessionId)
        }
        onDispose {
            navigator.unregisterForwardValidator(Route.SessionRoot.SessionDetails::class)
        }
    }

    SessionListContent(
        state = state,
        onIntent = viewModel::dispatch,
        onOpenDetails = { sessionId ->
            navigator.navigate(Route.SessionRoot.SessionDetails(sessionId))
        },
    )
}

@Composable
internal fun SessionListContent(
    state: SessionListState,
    onIntent: (SessionListIntent) -> Unit,
    modifier: Modifier = Modifier,
    onOpenDetails: (Long) -> Unit = {},
) {
    ScrollableScreenColumn(modifier = modifier) {
        SessionScreenStatsRow(
            stats = state.stats,
            modifier = Modifier.fillMaxWidth(),
        )
        SessionScreenHeader(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            onIntent = onIntent,
        )
        SessionScreenTable(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            onOpenDetails = onOpenDetails,
            onIntent = onIntent,
        )
    }
}

@Preview
@Composable
private fun SessionListContentPreview() {
    SimAnalyzerTheme {
        SessionListContent(
            state = previewState(),
            onIntent = {},
        )
    }
}

private fun previewState(): SessionListState = SessionListState(
    isLoading = false,
    stats = SessionStatsUi(
        totalDistanceLabel = "0.000",
        sessionsCount = 8,
        incidentsCount = 0,
        favoriteCar = "Car Name",
    ),
    gameFilter = SessionFilterUiModel(
        kind = SessionFilterKind.Game,
        selectedId = FILTER_ALL_ID,
        options = listOf(
            SessionFilterOptionUi(FILTER_ALL_ID),
            SessionFilterOptionUi("acc", "ACC"),
        ),
    ),
    trackFilter = SessionFilterUiModel(
        kind = SessionFilterKind.Track,
        selectedId = FILTER_ALL_ID,
        options = listOf(
            SessionFilterOptionUi(FILTER_ALL_ID),
            SessionFilterOptionUi("spa", "Spa"),
        ),
    ),
    carFilter = SessionFilterUiModel(
        kind = SessionFilterKind.Car,
        selectedId = FILTER_ALL_ID,
        options = listOf(
            SessionFilterOptionUi(FILTER_ALL_ID),
            SessionFilterOptionUi("car_name", "Car Name"),
        ),
    ),
    dateFilter = SessionFilterUiModel(
        kind = SessionFilterKind.Date,
        selectedId = FILTER_ALL_ID,
        options = listOf(
            SessionFilterOptionUi(FILTER_ALL_ID),
            SessionFilterOptionUi("oct_2025", "Oct 2025"),
        ),
    ),
    sortFilter = SessionFilterUiModel(
        kind = SessionFilterKind.Sort,
        selectedId = "newest",
        options = listOf(
            SessionFilterOptionUi("newest"),
            SessionFilterOptionUi("oldest"),
            SessionFilterOptionUi("best"),
        ),
    ),
    page = 1,
    pageCount = 9,
    sessions = previewSessions(),
    visibleSessions = previewSessions(),
)

private fun previewSessions(): List<SessionRowUi> = List(size = 8) { index ->
    val trackMap = previewTrackMapData()
    SessionRowUi(
        sessionId = (index + 1).toLong(),
        dateLabel = "Oct 24, 2025",
        timeLabel = "20:40",
        gameId = "acc",
        trackId = "spa",
        gameLabel = "ACC",
        sessionTypeLabel = if (index % 3 == 0) {
            "Practice"
        } else if (index % 3 == 1) {
            "Qualifying"
        } else {
            "Race"
        },
        trackLabel = "Location",
        carLabel = "Car Name",
        lapsLabel = "0",
        bestLapLabel = "0:00.000",
        isSaved = index % 3 == 0,
        trackMap = trackMap,
    )
}
