package com.analyzer.session.details.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.analyzer.session.details.presentation.components.SessionDetailsCompareSessionPickerDialog
import com.analyzer.session.details.presentation.components.SessionDetailsHeader
import com.analyzer.session.details.presentation.components.SessionDetailsLapTable
import com.analyzer.session.details.presentation.components.SessionDetailsShareResultsDialog
import com.analyzer.session.details.presentation.components.SessionDetailsStatsRow
import com.analyzer.session.details.presentation.components.SessionDetailsWorkspaceFabBar
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
import com.project.analyzer.chooser.FileChooserDialog
import com.project.analyzer.chooser.SelectionMode
import com.project.analyzer.chooser.rememberFileChooserState
import com.project.analyzer.navigation.api.LocalNavigator
import com.project.analyzer.navigation.api.Route
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.adaptive.ResponsiveGridMode
import com.project.analyzer.ui.adaptive.ResponsiveScreen
import com.project.analyzer.ui.components.InfoBarSnackbarHost
import com.project.analyzer.ui.components.InfoBarSnackbarVisuals
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun SessionDetailsScreen(sessionId: Long) {
    val viewModel = metroViewModel<SessionDetailViewModel>()
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val exportDirectoryChooserState = rememberFileChooserState(
        title = "Choose export folder",
        selectionMode = SelectionMode.DIRECTORY,
        onResult = { selectedPath ->
            selectedPath?.let { path ->
                viewModel.dispatch(SessionDetailIntent.ExportShareResultsToDirectory(path))
            }
        },
    )
    val importCompareSessionChooserState = rememberFileChooserState(
        title = "Choose compare session folder",
        selectionMode = SelectionMode.DIRECTORY,
        onResult = { selectedPath ->
            selectedPath?.let { path ->
                viewModel.dispatch(SessionDetailIntent.ImportCompareSession(path))
            }
        },
    )

    LaunchedEffect(viewModel, sessionId) {
        viewModel.dispatch(SessionDetailIntent.BindSession(sessionId))
    }

    LaunchedEffect(viewModel) {
        viewModel.actions.collect { action ->
            snackbarHostState.showSnackbar(
                visuals = InfoBarSnackbarVisuals(
                    title = action.title,
                    message = action.message,
                    severity = action.severity,
                    duration = SnackbarDuration.Short,
                ),
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SessionDetailsContent(
            state = state,
            modifier = Modifier.fillMaxSize(),
            onRequestShareExportDirectory = { exportDirectoryChooserState.show() },
            onRequestCompareImportDirectory = { importCompareSessionChooserState.show() },
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
            onCompareSessionSelect = { referenceSessionId ->
                viewModel.dispatch(SessionDetailIntent.DismissCompareSessionPicker)
                navigator.navigate(
                    Route.SessionRoot.SessionAnalysis(
                        sessionId = sessionId,
                        referenceSessionId = referenceSessionId,
                    ),
                )
            },
            onIntent = viewModel::dispatch,
        )

        InfoBarSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .widthIn(max = 560.dp),
        )
    }

    FileChooserDialog(state = exportDirectoryChooserState)
    FileChooserDialog(state = importCompareSessionChooserState)
}

@Composable
internal fun SessionDetailsContent(
    state: SessionDetailState,
    modifier: Modifier = Modifier,
    onAnalysisClick: () -> Unit = {},
    onCompareConfirm: () -> Unit = {},
    onCompareSessionSelect: (Long) -> Unit = {},
    onRequestShareExportDirectory: () -> Unit = {},
    onRequestCompareImportDirectory: () -> Unit = {},
    onIntent: (SessionDetailIntent) -> Unit = {},
) {
    Box(modifier = modifier) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = SimAnalyzerTheme.material.background,
            floatingActionButton = {
                SessionDetailsWorkspaceFabBar(
                    isCompareSelectionMode = state.isCompareSelectionMode,
                    selectedCompareLaps = state.selectedCompareLaps,
                    compareConfirmEnabled = state.compareConfirmEnabled,
                    compareStartEnabled = !state.isLoading && state.visibleLaps.size >= 2,
                    actionsEnabled = !state.isLoading && state.error == null,
                    onStartCompare = {
                        onIntent(SessionDetailIntent.StartCompareSelection)
                    },
                    onCancelCompare = {
                        onIntent(SessionDetailIntent.CancelCompareSelection)
                    },
                    onConfirmCompare = onCompareConfirm,
                    onOpenCompareSessionPicker = {
                        onIntent(SessionDetailIntent.OpenCompareSessionPicker)
                    },
                    onShareResults = {
                        onIntent(SessionDetailIntent.OpenShareResults)
                    },
                )
            },
        ) { innerPadding ->
            ResponsiveScreen(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    bottom = if (state.isCompareSelectionMode) 104.dp else 176.dp,
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

        SessionDetailsCompareSessionPickerDialog(
            picker = state.compareSessionPicker,
            onDismiss = { onIntent(SessionDetailIntent.DismissCompareSessionPicker) },
            onSelectSession = onCompareSessionSelect,
            onBrowseImportSession = onRequestCompareImportDirectory,
            onImportSessionPath = { path ->
                onIntent(SessionDetailIntent.ImportCompareSession(path))
            },
        )

        SessionDetailsShareResultsDialog(
            shareDialog = state.shareDialog,
            onDismiss = { onIntent(SessionDetailIntent.DismissShareResults) },
            onCopySummary = { onIntent(SessionDetailIntent.CopyShareResults) },
            onExportReport = onRequestShareExportDirectory,
            onOpenRawFiles = { onIntent(SessionDetailIntent.OpenShareSessionFiles) },
        )
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
