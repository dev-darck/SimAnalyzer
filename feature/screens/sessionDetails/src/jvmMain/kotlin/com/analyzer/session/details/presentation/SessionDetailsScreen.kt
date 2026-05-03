package com.analyzer.session.details.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.analyzer.session.details.presentation.components.SessionDetailsCompareFabBar
import com.analyzer.session.details.presentation.components.SessionDetailsHeader
import com.analyzer.session.details.presentation.components.SessionDetailsLapTable
import com.analyzer.session.details.presentation.components.SessionDetailsStatsRow
import com.analyzer.session.details.presentation.model.LapStatus
import com.analyzer.session.details.presentation.model.SessionDetailCompareLapUi
import com.analyzer.session.details.presentation.model.SessionDetailFilterKind
import com.analyzer.session.details.presentation.model.SessionDetailFilterOptionUi
import com.analyzer.session.details.presentation.model.SessionDetailFilterUiModel
import com.analyzer.session.details.presentation.model.SessionDetailHeaderUi
import com.analyzer.session.details.presentation.model.SessionDetailIntent
import com.analyzer.session.details.presentation.model.SessionDetailState
import com.analyzer.session.details.presentation.model.SessionDetailStatsUi
import com.analyzer.session.details.presentation.model.SessionLapRowUi
import com.project.analyzer.navigation.api.LocalNavigator
import com.project.analyzer.navigation.api.Route
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.adaptive.ResponsiveGridMode
import com.project.analyzer.ui.adaptive.ResponsiveScreen
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun SessionDetailsScreen(sessionId: Long) {
    val viewModel = metroViewModel<SessionDetailViewModel>()
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, sessionId) {
        viewModel.dispatch(SessionDetailIntent.BindSession(sessionId))
    }

    SessionDetailsContent(
        state = state,
        onAnalysisClick = {
            navigator.navigate(Route.SessionRoot.SessionAnalysis(sessionId = sessionId))
        },
        onCompareConfirm = {
            val selection = state.selectedCompareLaps.toComparisonSelection()
            if (selection.size == 2) {
                val baseLap = selection[0]
                val referenceLap = selection[1]
                viewModel.dispatch(SessionDetailIntent.CancelCompareSelection)
                navigator.navigate(
                    Route.SessionRoot.SessionAnalysis(
                        sessionId = sessionId,
                        segmentId = baseLap.segmentId,
                        lapNumber = baseLap.lapNumber,
                        referenceSegmentId = referenceLap.segmentId,
                        referenceLapNumber = referenceLap.lapNumber,
                    ),
                )
            }
        },
        onIntent = viewModel::dispatch,
    )
}

@Composable
internal fun SessionDetailsContent(
    state: SessionDetailState,
    modifier: Modifier = Modifier,
    onAnalysisClick: () -> Unit = {},
    onCompareConfirm: () -> Unit = {},
    onIntent: (SessionDetailIntent) -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        containerColor = SimAnalyzerTheme.material.background,
        floatingActionButton = {
            SessionDetailsCompareFabBar(
                isCompareSelectionMode = state.isCompareSelectionMode,
                selectedCompareLaps = state.selectedCompareLaps,
                compareConfirmEnabled = state.compareConfirmEnabled,
                onStartCompare = {
                    onIntent(SessionDetailIntent.StartCompareSelection)
                },
                onCancelCompare = {
                    onIntent(SessionDetailIntent.CancelCompareSelection)
                },
                onConfirmCompare = onCompareConfirm,
            )
        },
    ) { innerPadding ->
        ResponsiveScreen(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(
                start = 12.dp,
                top = 0.dp,
                end = 12.dp,
                bottom = 104.dp,
            ),
            verticalSpacing = 10.dp,
            gridMode = ResponsiveGridMode.Grid,
            mediumColumns = 1,
            expandedColumns = 1,
            backgroundColor = SimAnalyzerTheme.material.background,
        ) {
            item(key = "session-details-stats", isContentFull = true) {
                SessionDetailsStatsRow(
                    stats = state.stats,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item(key = "session-details-header", isContentFull = true) {
                SessionDetailsHeader(
                    header = state.header,
                    sortFilter = state.sortFilter,
                    showFilter = state.showFilter,
                    sessionTypeFilter = state.sessionTypeFilter,
                    modifier = Modifier.fillMaxWidth(),
                    onSortSelect = { onIntent(SessionDetailIntent.ChangeSort(it)) },
                    onShowSelect = { onIntent(SessionDetailIntent.ChangeFilter(it)) },
                    onSessionTypeSelect = { onIntent(SessionDetailIntent.ChangeSessionTypeFilter(it)) },
                    onAnalysisClick = onAnalysisClick,
                )
            }
            item(key = "session-details-table", isContentFull = true) {
                SessionDetailsLapTable(
                    state = state,
                    modifier = Modifier.fillMaxWidth(),
                    onPageChange = { onIntent(SessionDetailIntent.ChangePage(it)) },
                    onSortChange = { onIntent(SessionDetailIntent.ChangeSort(it)) },
                    onCompareToggle = { segmentId, lapNumber ->
                        onIntent(
                            SessionDetailIntent.ToggleCompareLap(
                                segmentId = segmentId,
                                lapNumber = lapNumber,
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview
@Composable
private fun SessionDetailsContentPreview() {
    SimAnalyzerTheme {
        SessionDetailsContent(
            state = previewState(),
            onAnalysisClick = {},
            onCompareConfirm = {},
            onIntent = {},
        )
    }
}

@Suppress("LongMethod")
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
        isCompareSelectionMode = true,
        selectedCompareLaps = persistentListOf(
            SessionDetailCompareLapUi(
                segmentId = 1L,
                lapNumber = 2,
                lapLabel = "2",
                sessionTypeLabel = "Qualifying",
                totalTimeMs = 101_450,
            ),
        ),
    )
}

private fun previewLaps(): ImmutableList<SessionLapRowUi> = persistentListOf(
    previewLap(number = 1, delta = "-0.000", status = LapStatus.Clean),
    previewLap(number = 2, delta = "-0.000", status = LapStatus.OutLap, compareSelected = true),
    previewLap(number = 5, delta = "-0.000", status = LapStatus.BestLap),
    previewLap(number = 7, delta = "+0.000", status = LapStatus.Dirty),
    previewLap(number = 8, incidents = "1", delta = "--", status = LapStatus.Invalid),
    previewLap(number = 9, delta = "-0.000", status = LapStatus.PitIn),
)

private fun previewLap(
    number: Int,
    incidents: String = "0",
    delta: String,
    status: LapStatus,
    compareSelected: Boolean = false,
): SessionLapRowUi = SessionLapRowUi(
    segmentId = 1L,
    lapNumber = number,
    lapLabel = number.toString(),
    sessionTypeLabel = "Qualifying",
    totalTimeMs = null,
    totalTime = "0:00.000",
    s1 = "00.000",
    s2 = "00.000",
    s3 = "00.000",
    incidents = incidents,
    delta = delta,
    deltaIsPositive = delta.startsWith("+"),
    status = status,
    compareAvailable = true,
    compareSelected = compareSelected,
    compareSelectionOrdinal = if (compareSelected) 1 else null,
)

private fun ImmutableList<SessionDetailCompareLapUi>.toComparisonSelection(): List<SessionDetailCompareLapUi> =
    sortedWith(
        compareByDescending<SessionDetailCompareLapUi> { it.totalTimeMs ?: Int.MIN_VALUE }
            .thenBy(SessionDetailCompareLapUi::lapNumber),
    )
