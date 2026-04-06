package com.analyzer.session.analysis.presentation.components.layout.state

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.analyzer.session.analysis.presentation.components.inspector.model.SessionAnalysisInspectorTab
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSampleUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisState
import com.analyzer.session.analysis.presentation.preview.sessionAnalysisPreviewState
import com.analyzer.session.analysis.presentation.resolver.resolveActivePoint
import com.analyzer.session.analysis.presentation.resolver.resolveActiveSample
import com.project.analyzer.theme.SimAnalyzerTheme
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun rememberSessionAnalysisStudioInteractionState(
    state: SessionAnalysisState,
): SessionAnalysisStudioInteractionState {
    val studio = state.studio
    val interaction = studio?.interaction
    val graphState = studio?.graph
    val comparisonPoints = graphState?.comparisonPoints ?: persistentListOf()
    val visibleSamples = interaction?.visibleSamples ?: persistentListOf()
    val comparisonPointByFrameId = remember(comparisonPoints) {
        comparisonPoints.mapNotNull { point ->
            point.selectedFrameId?.let { frameId -> frameId to point }
        }.toMap()
    }
    val visibleSampleByFrameId = remember(visibleSamples) {
        visibleSamples.associateBy(SessionAnalysisSampleUi::frameId)
    }

    var inspectorTab by rememberSaveable { mutableStateOf(SessionAnalysisInspectorTab.Timing) }
    var hoverFraction by remember { mutableStateOf<Float?>(null) }
    var hoverFrameId by remember { mutableStateOf<Long?>(null) }
    var lockedFraction by rememberSaveable { mutableStateOf<Float?>(null) }
    var lockedFrameId by rememberSaveable { mutableStateOf<Long?>(null) }
    var focusMode by rememberSaveable { mutableStateOf(false) }
    var heroCollapsed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.selectedSegmentId, state.selectedLapNumber, state.referenceLapNumber) {
        hoverFraction = null
        hoverFrameId = null
        lockedFraction = null
        lockedFrameId = null
        inspectorTab = SessionAnalysisInspectorTab.Timing
    }

    val selectionLocked = lockedFraction != null || lockedFrameId != null
    val rawInteractionFraction = lockedFraction
        ?: hoverFraction
        ?: state.selectedComparisonPoint?.fraction
        ?: state.selectedSample?.trackPosition
    val shouldAnimateInteractionFraction = focusMode &&
        !selectionLocked &&
        rawInteractionFraction != null &&
        hoverFrameId == null
    val animatedInteractionFraction by animateFloatAsState(
        targetValue = rawInteractionFraction ?: 0f,
        animationSpec = if (shouldAnimateInteractionFraction) {
            tween(
                durationMillis = focusCursorSmoothingDurationMs,
                easing = LinearOutSlowInEasing,
            )
        } else {
            snap()
        },
        label = "sessionAnalysisFocusCursorFraction",
    )
    val interactionFraction = when {
        rawInteractionFraction == null -> null
        shouldAnimateInteractionFraction -> animatedInteractionFraction
        else -> rawInteractionFraction
    }

    val fractionDrivenActivePoint = remember(
        comparisonPoints,
        state.selectedComparisonPoint,
        interactionFraction,
    ) {
        if (interaction == null) {
            comparisonPoints.resolveActivePointFallback(
                fallback = state.selectedComparisonPoint,
                fraction = interactionFraction,
            )
        } else {
            interaction.resolveActivePoint(
                comparisonPoints = comparisonPoints,
                fallback = state.selectedComparisonPoint,
                fraction = interactionFraction,
            )
        }
    }
    val activeFrameId = lockedFrameId ?: hoverFrameId
    val frameDrivenActiveSample = remember(visibleSamples, state.selectedSample, activeFrameId) {
        if (activeFrameId == null) {
            null
        } else {
            visibleSampleByFrameId[activeFrameId] ?: state.selectedSample
        }
    }
    val activePoint = remember(
        comparisonPoints,
        state.selectedComparisonPoint,
        activeFrameId,
        frameDrivenActiveSample,
        fractionDrivenActivePoint,
    ) {
        if (activeFrameId == null) {
            fractionDrivenActivePoint
        } else {
            comparisonPointByFrameId[activeFrameId]
                ?: frameDrivenActiveSample?.trackPosition?.let(comparisonPoints::nearestToTrackPosition)
                ?: fractionDrivenActivePoint
        }
    }
    val fractionDrivenActiveSample = remember(visibleSamples, state.selectedSample, activePoint) {
        if (interaction == null) {
            visibleSamples.resolveActiveSampleFallback(
                fallback = state.selectedSample,
                activePoint = activePoint,
            )
        } else {
            interaction.resolveActiveSample(
                visibleSamples = visibleSamples,
                fallback = state.selectedSample,
                activePoint = activePoint,
            )
        }
    }
    val activeSample = frameDrivenActiveSample ?: fractionDrivenActiveSample

    val onGraphHoverFraction: (Float?) -> Unit = onGraphHoverFraction@{ fraction ->
        if (!selectionLocked) {
            if (shouldRetainFocusHoverOnExit(focusMode = focusMode, nextFraction = fraction, nextFrameId = null)) {
                return@onGraphHoverFraction
            }
            hoverFraction = fraction
            hoverFrameId = null
        }
    }
    val onGraphPressFraction: (Float?) -> Unit = { fraction ->
        val snapped = fraction
            ?: comparisonPoints.resolveActivePointFallback(
                fallback = activePoint,
                fraction = fraction,
            )?.fraction
        if (snapped != null) {
            val unlockSelection = lockedFrameId == null &&
                lockedFraction != null &&
                circularFractionDistance(lockedFraction!!, snapped) < 0.002f
            lockedFrameId = null
            lockedFraction = if (unlockSelection) null else snapped
            if (unlockSelection) {
                hoverFrameId = null
            }
        }
    }
    val onMapHover: (Float?, Long?) -> Unit = { fraction, frameId ->
        if (!selectionLocked) {
            if (
                shouldUpdateMapHover(
                    currentFraction = hoverFraction,
                    currentFrameId = hoverFrameId,
                    nextFraction = fraction,
                    nextFrameId = frameId,
                    focusMode = focusMode,
                )
            ) {
                hoverFraction = fraction
                hoverFrameId = frameId
            }
        }
    }
    val onMapPress: (Float?, Long?) -> Unit = { fraction, frameId ->
        val snapped = fraction
            ?: activePoint?.fraction
            ?: activeSample?.trackPosition
        if (snapped != null) {
            val unlockByFrame = frameId != null && lockedFrameId == frameId
            val unlockByFraction = lockedFraction != null &&
                circularFractionDistance(lockedFraction!!, snapped) < 0.002f
            if (unlockByFrame || unlockByFraction) {
                lockedFrameId = null
                lockedFraction = null
            } else {
                lockedFrameId = frameId
                lockedFraction = snapped
                hoverFrameId = frameId
                hoverFraction = snapped
            }
        }
    }

    return SessionAnalysisStudioInteractionState(
        inspectorTab = inspectorTab,
        activePoint = activePoint,
        activeSample = activeSample,
        selectionLocked = selectionLocked,
        focusMode = focusMode,
        heroCollapsed = heroCollapsed,
        cursorFraction = interactionFraction ?: activePoint?.fraction,
        cursorFrameId = lockedFrameId ?: hoverFrameId ?: activeSample?.frameId,
        onInspectorTabSelected = { inspectorTab = it },
        onFocusModeToggle = { focusMode = !focusMode },
        onHeroCollapseToggle = { heroCollapsed = !heroCollapsed },
        onGraphHoverFraction = onGraphHoverFraction,
        onGraphPressFraction = onGraphPressFraction,
        onMapHover = onMapHover,
        onMapPress = onMapPress,
    )
}

@Preview
@Composable
internal fun RememberSessionAnalysisStudioInteractionStatePreview() {
    val interactionState = rememberSessionAnalysisStudioInteractionState(sessionAnalysisPreviewState())

    SimAnalyzerTheme {
        Text(text = interactionState.cursorFraction?.toString() ?: "no-selection")
    }
}
