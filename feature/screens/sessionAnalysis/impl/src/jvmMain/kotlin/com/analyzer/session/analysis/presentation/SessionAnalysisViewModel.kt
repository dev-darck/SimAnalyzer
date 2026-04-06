package com.analyzer.session.analysis.presentation

import com.analyzer.session.analysis.domain.model.SessionAnalysisWorkspaceData
import com.analyzer.session.analysis.domain.usecase.SessionAnalysisUseCase
import com.analyzer.session.analysis.presentation.builder.state.toShellState
import com.analyzer.session.analysis.presentation.builder.state.toState
import com.analyzer.session.analysis.presentation.cache.SelectionStateCache
import com.analyzer.session.analysis.presentation.cache.SelectionStateKey
import com.analyzer.session.analysis.presentation.model.SessionAnalysisBindSessionIntent
import com.analyzer.session.analysis.presentation.model.SessionAnalysisError
import com.analyzer.session.analysis.presentation.model.SessionAnalysisIntent
import com.analyzer.session.analysis.presentation.model.SessionAnalysisRefreshIntent
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSelectLapIntent
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSelectReferenceLapIntent
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSelectSessionIntent
import com.analyzer.session.analysis.presentation.model.SessionAnalysisState
import com.project.analyzer.api.di.Default
import com.project.analyzer.leak.api.LeakAwareMviViewModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Inject
internal class SessionAnalysisViewModel(
    private val useCase: SessionAnalysisUseCase,
    @param:Default
    private val defaultDispatcher: CoroutineDispatcher,
) : LeakAwareMviViewModel<SessionAnalysisIntent, SessionAnalysisState>(SessionAnalysisState()) {

    private var binding: SessionBinding = SessionBinding()

    // Keep only a couple of recent selection permutations; full states are large for this screen.
    private val selectionStateCache = SelectionStateCache(maxEntries = 3)

    init {
        addCloseable {
            selectionStateCache.clear()
            binding = SessionBinding()
        }
    }

    override suspend fun handleIntent(intent: SessionAnalysisIntent) {
        when (intent) {
            is SessionAnalysisBindSessionIntent -> bindSession(intent.sessionId)
            SessionAnalysisRefreshIntent -> reload(forceRefresh = true)
            is SessionAnalysisSelectSessionIntent -> selectSession(intent.segmentId)
            is SessionAnalysisSelectLapIntent -> selectLap(intent.lapNumber)
            is SessionAnalysisSelectReferenceLapIntent -> selectReferenceLap(intent.lapNumber)
        }
    }

    private suspend fun bindSession(sessionId: Long) {
        if (binding.requestedSessionId == sessionId && binding.loadedSessionId == sessionId &&
            binding.data != null
        ) {
            return
        }
        binding = binding.copy(requestedSessionId = sessionId)
        load(sessionId)
    }

    @Suppress("UNUSED_PARAMETER")
    private suspend fun reload(forceRefresh: Boolean) {
        val sessionId = binding.requestedSessionId ?: return
        load(
            sessionId = sessionId,
            forceRefresh = forceRefresh,
        )
    }

    private suspend fun load(sessionId: Long, forceRefresh: Boolean = false) {
        val shouldPreserveSelection = binding.loadedSessionId == sessionId
        val currentSelection = state.value
        updateState { copy(isLoading = true, error = null) }
        val shellWorkspaceData = try {
            useCase.loadWorkspaceShell(
                sessionId = sessionId,
                forceRefresh = forceRefresh,
            )
        } catch (error: Throwable) {
            handleLoadFailure(
                error = error,
                fallbackState = currentSelection.takeIf { it.header != null },
            )
            return
        }
        if (shellWorkspaceData == null) {
            binding = binding.copy(
                loadedSessionId = null,
                data = null,
            )
            setState(
                SessionAnalysisState(
                    isLoading = false,
                    error = SessionAnalysisError.Unavailable,
                ),
            )
            return
        }

        binding = binding.copy(
            loadedSessionId = sessionId,
            data = shellWorkspaceData,
        )
        selectionStateCache.clear()

        val selectedSegmentId = if (shouldPreserveSelection) {
            currentSelection.selectedSegmentId
        } else {
            null
        }
        val selectedLapNumber = if (shouldPreserveSelection) {
            currentSelection.selectedLapNumber
        } else {
            null
        }
        val selectedReferenceLapNumber = if (shouldPreserveSelection && currentSelection.referenceLapIsCustom) {
            currentSelection.referenceLapNumber
        } else {
            null
        }

        val shellState = withContext(defaultDispatcher) {
            shellWorkspaceData.report.toShellState(
                authoredTrackMap = shellWorkspaceData.authoredTrackMap,
                sourceTrackMap = shellWorkspaceData.sourceTrackMap,
                displayTrackMap = shellWorkspaceData.displayTrackMap,
                selectedSegmentId = selectedSegmentId,
                selectedLapNumber = selectedLapNumber,
                selectedReferenceLapNumber = selectedReferenceLapNumber,
            )
        }
        setState(shellState)
        val workspaceData = try {
            useCase.enrichWorkspace(shellWorkspaceData)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            binding = binding.copy(
                loadedSessionId = sessionId,
                data = shellWorkspaceData,
            )
            setState(state.value.copy(isLoading = false, error = null))
            return
        }
        binding = binding.copy(
            loadedSessionId = sessionId,
            data = workspaceData,
        )
        selectionStateCache.clear()

        val nextState = resolveState(
            workspaceData = workspaceData,
            segmentId = selectedSegmentId,
            lapNumber = selectedLapNumber,
            referenceLapNumber = selectedReferenceLapNumber,
        )
        setState(nextState)
    }

    private suspend fun selectLap(lapNumber: Int?) {
        val workspaceData = binding.data ?: return
        publish(
            workspaceData = workspaceData,
            segmentId = state.value.selectedSegmentId,
            lapNumber = lapNumber,
            referenceLapNumber = state.value.referenceLapNumber.takeIf { state.value.referenceLapIsCustom },
        )
    }

    private suspend fun selectSession(segmentId: Long?) {
        val workspaceData = binding.data ?: return
        publish(
            workspaceData = workspaceData,
            segmentId = segmentId,
            lapNumber = null,
            referenceLapNumber = null,
        )
    }

    private suspend fun selectReferenceLap(lapNumber: Int?) {
        val workspaceData = binding.data ?: return
        publish(
            workspaceData = workspaceData,
            segmentId = state.value.selectedSegmentId,
            lapNumber = state.value.selectedLapNumber,
            referenceLapNumber = lapNumber,
        )
    }

    private suspend fun publish(
        workspaceData: SessionAnalysisWorkspaceData,
        segmentId: Long?,
        lapNumber: Int?,
        referenceLapNumber: Int?,
    ) {
        val cacheKey = SelectionStateKey(
            segmentId = segmentId,
            lapNumber = lapNumber,
            referenceLapNumber = referenceLapNumber,
        )
        selectionStateCache[cacheKey]?.let { cachedState ->
            publishStateIfChanged(cachedState)
            return
        }
        publishStateIfChanged(
            state.value.copy(
                isLoading = true,
                error = null,
            ),
        )
        val nextState = resolveState(
            workspaceData = workspaceData,
            segmentId = segmentId,
            lapNumber = lapNumber,
            referenceLapNumber = referenceLapNumber,
        )
        publishStateIfChanged(nextState)
    }

    private suspend fun resolveState(
        workspaceData: SessionAnalysisWorkspaceData,
        segmentId: Long?,
        lapNumber: Int?,
        referenceLapNumber: Int?,
    ): SessionAnalysisState {
        val cacheKey = SelectionStateKey(
            segmentId = segmentId,
            lapNumber = lapNumber,
            referenceLapNumber = referenceLapNumber,
        )
        selectionStateCache[cacheKey]?.let { cachedState ->
            return cachedState
        }
        val nextState = withContext(defaultDispatcher) {
            workspaceData.report.toState(
                calibration = workspaceData.calibration,
                authoredTrackMap = workspaceData.authoredTrackMap,
                sourceTrackMap = workspaceData.sourceTrackMap,
                displayTrackMap = workspaceData.displayTrackMap,
                selectedSegmentId = segmentId,
                selectedLapNumber = lapNumber,
                selectedReferenceLapNumber = referenceLapNumber,
                selectedFrameId = null,
            )
        }
        selectionStateCache[cacheKey] = nextState
        return nextState
    }

    private fun handleLoadFailure(error: Throwable, fallbackState: SessionAnalysisState?) {
        if (error is CancellationException) throw error
        if (fallbackState != null) {
            setState(fallbackState.copy(isLoading = false, error = null))
            return
        }
        binding = binding.copy(
            loadedSessionId = null,
            data = null,
        )
        setState(
            SessionAnalysisState(
                isLoading = false,
                error = SessionAnalysisError.Unavailable,
            ),
        )
    }

    private fun publishStateIfChanged(nextState: SessionAnalysisState) {
        if (state.value != nextState) {
            setState(nextState)
        }
    }
}
