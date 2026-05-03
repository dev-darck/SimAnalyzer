package com.analyzer.session.analysis.presentation.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.analyzer.session.analysis.presentation.SessionAnalysisUiTokens
import com.analyzer.session.analysis.presentation.components.graph.SessionAnalysisWorkspaceGraphDock
import com.analyzer.session.analysis.presentation.components.header.SessionAnalysisStudioHeader
import com.analyzer.session.analysis.presentation.components.hero.SessionAnalysisWorkspaceHero
import com.analyzer.session.analysis.presentation.components.inspector.SessionAnalysisWorkspaceInspectorPane
import com.analyzer.session.analysis.presentation.components.layout.model.SessionAnalysisPaneLayoutMode
import com.analyzer.session.analysis.presentation.components.layout.model.SessionAnalysisStudioLayoutMetrics
import com.analyzer.session.analysis.presentation.components.layout.state.SessionAnalysisStudioInteractionState
import com.analyzer.session.analysis.presentation.components.layout.state.rememberSessionAnalysisStudioInteractionState
import com.analyzer.session.analysis.presentation.components.layout.support.SessionAnalysisEdgeExpandablePane
import com.analyzer.session.analysis.presentation.components.layout.support.SessionAnalysisPaneHoverEdge
import com.analyzer.session.analysis.presentation.components.layout.support.SessionAnalysisWideLayoutBreakpoint
import com.analyzer.session.analysis.presentation.components.layout.support.resolveHeroCollapsedHeight
import com.analyzer.session.analysis.presentation.components.layout.support.resolveHeroExpandedHeight
import com.analyzer.session.analysis.presentation.components.layout.support.resolveSessionAnalysisStudioLayoutMetrics
import com.analyzer.session.analysis.presentation.components.navigator.SessionAnalysisStudioNavigatorPane
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHeaderUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisScreenMode
import com.analyzer.session.analysis.presentation.model.SessionAnalysisState
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisGraphState
import com.analyzer.session.analysis.presentation.preview.sessionAnalysisPreviewState
import com.project.analyzer.theme.SimAnalyzerTheme
import kotlinx.coroutines.launch

/**
 * Coordinates the hero, navigator, graph dock, and inspector into one responsive analysis workspace.
 */
@Composable
internal fun SessionAnalysisStudioLayout(
    state: SessionAnalysisState,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
    onSessionSelected: (Long?) -> Unit = {},
    onLapSelected: (Int?) -> Unit = {},
    onReferenceLapSelected: (Int?) -> Unit = {},
) {
    val header = state.header ?: return
    val studio = state.studio
    val graphState = studio?.graph ?: SessionAnalysisGraphState()
    val interactionState = rememberSessionAnalysisStudioInteractionState(state = state)
    val screenMode = state.screenMode

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SimAnalyzerTheme.material.background)
            .padding(SessionAnalysisUiTokens.screenInset),
    ) {
        val density = LocalDensity.current
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()
        val heroExpandedHeight = resolveHeroExpandedHeight(maxWidth = maxWidth, maxHeight = maxHeight)
        val heroCollapsedHeight = resolveHeroCollapsedHeight(maxWidth = maxWidth, maxHeight = maxHeight)
        val useThreePaneLayout = maxWidth >= SessionAnalysisWideLayoutBreakpoint
        val heroTravelPx = with(density) { (heroExpandedHeight - heroCollapsedHeight).toPx() }
            .coerceAtLeast(1f)
        var wideHeroScrollOffsetPx by rememberSaveable(useThreePaneLayout) { mutableFloatStateOf(0f) }
        val compactScrollCollapseProgress by remember(listState, heroTravelPx) {
            derivedStateOf {
                val consumedPx = when {
                    listState.firstVisibleItemIndex > 0 -> heroTravelPx
                    else -> listState.firstVisibleItemScrollOffset.toFloat().coerceIn(0f, heroTravelPx)
                }
                (consumedPx / heroTravelPx).coerceIn(0f, 1f)
            }
        }
        val scrollCollapseProgress = if (useThreePaneLayout) {
            (wideHeroScrollOffsetPx / heroTravelPx).coerceIn(0f, 1f)
        } else {
            compactScrollCollapseProgress
        }
        val layoutMetrics = remember(maxWidth, maxHeight, scrollCollapseProgress, interactionState.heroCollapsed) {
            resolveSessionAnalysisStudioLayoutMetrics(
                maxWidth = maxWidth,
                maxHeight = maxHeight,
                scrollCollapseProgress = scrollCollapseProgress,
                heroManuallyCollapsed = interactionState.heroCollapsed,
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(SessionAnalysisUiTokens.blockGap),
        ) {
            SessionAnalysisStudioHeader(
                header = header,
                screenMode = screenMode,
                summary = state.summary,
                sessionOptions = state.sessionOptions,
                selectedSessionId = state.selectedSegmentId,
                selectedLapNumber = state.selectedLapNumber,
                referenceLapNumber = state.referenceLapNumber,
                selectionLocked = interactionState.selectionLocked,
                modifier = Modifier.fillMaxWidth(),
                onRefresh = onRefresh,
                onSessionSelected = onSessionSelected,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                if (layoutMetrics.useThreePaneLayout) {
                    SessionAnalysisStudioWideWorkspace(
                        state = state,
                        header = header,
                        screenMode = screenMode,
                        studio = studio,
                        graphState = graphState,
                        interactionState = interactionState,
                        layoutMetrics = layoutMetrics,
                        modifier = Modifier.fillMaxSize(),
                        onSessionSelected = onSessionSelected,
                        onLapSelected = onLapSelected,
                        onReferenceLapSelected = onReferenceLapSelected,
                        onVerticalScroll = { delta ->
                            wideHeroScrollOffsetPx = (wideHeroScrollOffsetPx + delta)
                                .coerceIn(0f, heroTravelPx)
                        },
                    )
                } else {
                    SessionAnalysisStudioCompactWorkspace(
                        state = state,
                        studio = studio,
                        header = header,
                        screenMode = screenMode,
                        graphState = graphState,
                        interactionState = interactionState,
                        heroHeight = layoutMetrics.heroHeight,
                        listState = listState,
                        modifier = Modifier.fillMaxSize(),
                        onSessionSelected = onSessionSelected,
                        onLapSelected = onLapSelected,
                        onReferenceLapSelected = onReferenceLapSelected,
                        onVerticalScroll = { delta ->
                            scope.launch {
                                listState.scrollBy(delta)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionAnalysisStudioWideWorkspace(
    state: SessionAnalysisState,
    studio: com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisStudioState?,
    header: SessionAnalysisHeaderUi,
    screenMode: SessionAnalysisScreenMode,
    graphState: SessionAnalysisGraphState,
    interactionState: SessionAnalysisStudioInteractionState,
    layoutMetrics: SessionAnalysisStudioLayoutMetrics,
    modifier: Modifier = Modifier,
    onSessionSelected: (Long?) -> Unit,
    onLapSelected: (Int?) -> Unit,
    onReferenceLapSelected: (Int?) -> Unit,
    onVerticalScroll: (Float) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(SessionAnalysisUiTokens.workspaceBlockGap),
    ) {
        SessionAnalysisWorkspaceHero(
            trackCanvasState = studio?.trackCanvas,
            screenMode = screenMode,
            activePoint = interactionState.activePoint,
            activeSample = interactionState.activeSample,
            diagnosticSummary = state.diagnosticSummary,
            diagnosticsLoading = state.isLoading,
            highlightsCount = state.highlights.size,
            selectedLapNumber = state.selectedLapNumber,
            referenceLapNumber = state.referenceLapNumber,
            selectionLocked = interactionState.selectionLocked,
            cursorFraction = interactionState.cursorFraction,
            cursorFrameId = interactionState.cursorFrameId,
            focusMode = interactionState.focusMode,
            collapsed = interactionState.heroCollapsed,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 2.dp)
                .height(layoutMetrics.heroHeight),
            onHoverFraction = interactionState.onMapHover,
            onPressFraction = interactionState.onMapPress,
            onFocusModeToggle = interactionState.onFocusModeToggle,
            onCollapseToggle = interactionState.onHeroCollapseToggle,
            onVerticalScroll = onVerticalScroll,
        )
        SessionAnalysisStudioWideContent(
            state = state,
            header = header,
            screenMode = screenMode,
            graphState = graphState,
            interactionState = interactionState,
            layoutMetrics = layoutMetrics,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            onSessionSelected = onSessionSelected,
            onLapSelected = onLapSelected,
            onReferenceLapSelected = onReferenceLapSelected,
        )
    }
}

@Composable
private fun SessionAnalysisStudioWideContent(
    state: SessionAnalysisState,
    header: SessionAnalysisHeaderUi,
    screenMode: SessionAnalysisScreenMode,
    graphState: SessionAnalysisGraphState,
    interactionState: SessionAnalysisStudioInteractionState,
    layoutMetrics: SessionAnalysisStudioLayoutMetrics,
    modifier: Modifier = Modifier,
    onSessionSelected: (Long?) -> Unit,
    onLapSelected: (Int?) -> Unit,
    onReferenceLapSelected: (Int?) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(SessionAnalysisUiTokens.blockGap),
    ) {
        SessionAnalysisEdgeExpandablePane(
            baseWidth = layoutMetrics.leftPaneWidth,
            edge = SessionAnalysisPaneHoverEdge.Trailing,
        ) { paneModifier ->
            SessionAnalysisStudioNavigatorPane(
                header = header,
                screenMode = screenMode,
                sessionOptions = state.sessionOptions,
                selectedSessionId = state.selectedSegmentId,
                laps = state.laps,
                selectedLapNumber = state.selectedLapNumber,
                referenceLap = state.referenceLapSummary,
                referenceLapNumber = state.referenceLapNumber,
                referenceLapIsCustom = state.referenceLapIsCustom,
                hasExternalReference = state.hasExternalReference,
                highlights = state.highlights,
                layoutMode = SessionAnalysisPaneLayoutMode.Bounded,
                modifier = paneModifier,
                onSessionSelected = onSessionSelected,
                onLapSelected = onLapSelected,
                onReferenceLapSelected = onReferenceLapSelected,
            )
        }
        SessionAnalysisWorkspaceGraphDock(
            graphState = graphState,
            activePoint = interactionState.activePoint,
            selectionLocked = interactionState.selectionLocked,
            selectedLapNumber = state.selectedLapNumber,
            referenceLapNumber = state.referenceLapNumber,
            boundedHeight = true,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            onHoverFraction = interactionState.onGraphHoverFraction,
            onPressFraction = interactionState.onGraphPressFraction,
        )
        SessionAnalysisEdgeExpandablePane(
            baseWidth = layoutMetrics.rightPaneWidth,
            edge = SessionAnalysisPaneHoverEdge.Leading,
        ) { paneModifier ->
            SessionAnalysisWorkspaceInspectorPane(
                header = header,
                screenMode = screenMode,
                summary = state.summary,
                sectors = state.sectors,
                lapCoach = state.lapCoach,
                diagnosticSummary = state.diagnosticSummary,
                highlights = state.highlights,
                activePoint = interactionState.activePoint,
                activeSample = interactionState.activeSample,
                selectedLapNumber = state.selectedLapNumber,
                referenceLapNumber = state.referenceLapNumber,
                selectedTab = interactionState.inspectorTab,
                inspectorState = state.studio?.inspector,
                layoutMode = SessionAnalysisPaneLayoutMode.Bounded,
                modifier = paneModifier,
                onTabSelected = interactionState.onInspectorTabSelected,
                onTrackPositionSelected = { interactionState.onMapPress(it, null) },
            )
        }
    }
}

@Composable
private fun SessionAnalysisStudioCompactWorkspace(
    state: SessionAnalysisState,
    studio: com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisStudioState?,
    header: SessionAnalysisHeaderUi,
    screenMode: SessionAnalysisScreenMode,
    graphState: SessionAnalysisGraphState,
    interactionState: SessionAnalysisStudioInteractionState,
    heroHeight: Dp,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    onSessionSelected: (Long?) -> Unit,
    onLapSelected: (Int?) -> Unit,
    onReferenceLapSelected: (Int?) -> Unit,
    onVerticalScroll: (Float) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(SessionAnalysisUiTokens.workspaceBlockGap),
    ) {
        SessionAnalysisWorkspaceHero(
            trackCanvasState = studio?.trackCanvas,
            screenMode = screenMode,
            activePoint = interactionState.activePoint,
            activeSample = interactionState.activeSample,
            diagnosticSummary = state.diagnosticSummary,
            diagnosticsLoading = state.isLoading,
            highlightsCount = state.highlights.size,
            selectedLapNumber = state.selectedLapNumber,
            referenceLapNumber = state.referenceLapNumber,
            selectionLocked = interactionState.selectionLocked,
            cursorFraction = interactionState.cursorFraction,
            cursorFrameId = interactionState.cursorFrameId,
            focusMode = interactionState.focusMode,
            collapsed = interactionState.heroCollapsed,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 2.dp)
                .height(heroHeight),
            onHoverFraction = interactionState.onMapHover,
            onPressFraction = interactionState.onMapPress,
            onFocusModeToggle = interactionState.onFocusModeToggle,
            onCollapseToggle = interactionState.onHeroCollapseToggle,
            onVerticalScroll = onVerticalScroll,
        )
        SessionAnalysisStudioCompactContent(
            state = state,
            header = header,
            screenMode = screenMode,
            graphState = graphState,
            interactionState = interactionState,
            listState = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            onSessionSelected = onSessionSelected,
            onLapSelected = onLapSelected,
            onReferenceLapSelected = onReferenceLapSelected,
        )
    }
}

@Composable
private fun SessionAnalysisStudioCompactContent(
    state: SessionAnalysisState,
    header: SessionAnalysisHeaderUi,
    screenMode: SessionAnalysisScreenMode,
    graphState: SessionAnalysisGraphState,
    interactionState: SessionAnalysisStudioInteractionState,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    onSessionSelected: (Long?) -> Unit,
    onLapSelected: (Int?) -> Unit,
    onReferenceLapSelected: (Int?) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = SessionAnalysisUiTokens.screenInset,
        ),
        verticalArrangement = Arrangement.spacedBy(SessionAnalysisUiTokens.blockGap),
    ) {
        item {
            SessionAnalysisWorkspaceGraphDock(
                graphState = graphState,
                activePoint = interactionState.activePoint,
                selectionLocked = interactionState.selectionLocked,
                selectedLapNumber = state.selectedLapNumber,
                referenceLapNumber = state.referenceLapNumber,
                boundedHeight = false,
                modifier = Modifier.fillMaxWidth(),
                onHoverFraction = interactionState.onGraphHoverFraction,
                onPressFraction = interactionState.onGraphPressFraction,
            )
        }
        item {
            SessionAnalysisWorkspaceInspectorPane(
                header = header,
                screenMode = screenMode,
                summary = state.summary,
                sectors = state.sectors,
                lapCoach = state.lapCoach,
                diagnosticSummary = state.diagnosticSummary,
                highlights = state.highlights,
                activePoint = interactionState.activePoint,
                activeSample = interactionState.activeSample,
                selectedLapNumber = state.selectedLapNumber,
                referenceLapNumber = state.referenceLapNumber,
                selectedTab = interactionState.inspectorTab,
                inspectorState = state.studio?.inspector,
                layoutMode = SessionAnalysisPaneLayoutMode.Embedded,
                modifier = Modifier.fillMaxWidth(),
                onTabSelected = interactionState.onInspectorTabSelected,
                onTrackPositionSelected = { interactionState.onMapPress(it, null) },
            )
        }
        item {
            SessionAnalysisStudioNavigatorPane(
                header = header,
                screenMode = screenMode,
                sessionOptions = state.sessionOptions,
                selectedSessionId = state.selectedSegmentId,
                laps = state.laps,
                selectedLapNumber = state.selectedLapNumber,
                referenceLap = state.referenceLapSummary,
                referenceLapNumber = state.referenceLapNumber,
                referenceLapIsCustom = state.referenceLapIsCustom,
                hasExternalReference = state.hasExternalReference,
                highlights = state.highlights,
                layoutMode = SessionAnalysisPaneLayoutMode.Embedded,
                modifier = Modifier.fillMaxWidth(),
                onSessionSelected = onSessionSelected,
                onLapSelected = onLapSelected,
                onReferenceLapSelected = onReferenceLapSelected,
            )
        }
    }
}

@Preview
@Composable
internal fun SessionAnalysisStudioLayoutPreview() {
    SimAnalyzerTheme {
        SessionAnalysisStudioLayout(
            state = sessionAnalysisPreviewState(),
            modifier = Modifier
                .width(1440.dp)
                .height(900.dp),
        )
    }
}
