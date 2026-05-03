package com.analyzer.session.analysis.presentation

import com.analyzer.session.analysis.domain.model.SessionAnalysisWorkspaceData
import com.analyzer.session.analysis.domain.model.SessionAnalysisWorkspaceRequest
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

@Suppress("TooManyFunctions")
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
            is SessionAnalysisBindSessionIntent -> bindSession(intent.toBindingRequest())
            SessionAnalysisRefreshIntent -> reload(forceRefresh = true)
            is SessionAnalysisSelectSessionIntent -> selectSession(intent.segmentId)
            is SessionAnalysisSelectLapIntent -> selectLap(intent.lapNumber)
            is SessionAnalysisSelectReferenceLapIntent -> selectReferenceLap(intent.lapNumber)
        }
    }

    private suspend fun bindSession(request: SessionAnalysisBindingRequest) {
        if (binding.matches(request) && binding.data != null) {
            return
        }
        updateBinding(request)
        binding.data?.takeIf { binding.loadedSessionId == request.sessionId }?.let { workspaceData ->
            publish(workspaceData = workspaceData, selection = request.selection)
            return
        }
        load(request = request, forceRefresh = false)
    }

    @Suppress("UNUSED_PARAMETER")
    private suspend fun reload(forceRefresh: Boolean) {
        val request = currentBindingRequest() ?: return
        val currentState = state.value
        load(
            request = request.copy(
                selection = request.selection.copy(
                    segmentId = currentState.selectedSegmentId,
                    lapNumber = currentState.selectedLapNumber,
                    referenceLapNumber = currentState.referenceLapNumber.takeIf {
                        currentState.referenceLapIsCustom
                    },
                ),
            ),
            forceRefresh = forceRefresh,
        )
    }

    @Suppress("LongMethod")
    private suspend fun load(request: SessionAnalysisBindingRequest, forceRefresh: Boolean = false) {
        val sessionId = request.sessionId
        val selection = request.selection
        val shouldPreserveSelection = binding.loadedSessionId == sessionId
        val hasExplicitSelection = selection.hasExplicitSelection()
        val currentSelection = state.value
        updateState { copy(isLoading = true, error = null) }
        val shellWorkspaceData = try {
            useCase.loadWorkspaceShell(
                request = SessionAnalysisWorkspaceRequest(
                    sessionId = sessionId,
                    referenceSessionId = selection.referenceSessionId,
                ),
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
            referenceSessionId = selection.referenceSessionId,
            referenceSegmentId = selection.referenceSegmentId,
            selectedSegmentId = selection.segmentId,
            selectedLapNumber = selection.lapNumber,
            referenceLapNumber = selection.referenceLapNumber,
            data = shellWorkspaceData,
        )
        selectionStateCache.clear()

        val resolvedSelection = selection.resolveAgainst(
            currentState = currentSelection,
            preserveSelection = shouldPreserveSelection && !hasExplicitSelection,
        )
        binding = binding.copy(
            selectedSegmentId = resolvedSelection.segmentId,
            selectedLapNumber = resolvedSelection.lapNumber,
            referenceSegmentId = resolvedSelection.referenceSegmentId,
            referenceLapNumber = resolvedSelection.referenceLapNumber,
        )

        val shellState = withContext(defaultDispatcher) {
            shellWorkspaceData.report.toShellState(
                referenceReport = shellWorkspaceData.referenceReport,
                authoredTrackMap = shellWorkspaceData.authoredTrackMap,
                sourceTrackMap = shellWorkspaceData.sourceTrackMap,
                displayTrackMap = shellWorkspaceData.displayTrackMap,
                selectedSegmentId = resolvedSelection.segmentId,
                selectedLapNumber = resolvedSelection.lapNumber,
                selectedReferenceSegmentId = resolvedSelection.referenceSegmentId,
                selectedReferenceLapNumber = resolvedSelection.referenceLapNumber,
            )
        }
        setState(shellState)
        val workspaceData = try {
            useCase.enrichWorkspace(shellWorkspaceData)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
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
            selection = resolvedSelection,
        )
        setState(nextState)
    }

    private suspend fun selectLap(lapNumber: Int?) {
        val workspaceData = binding.data ?: return
        publish(
            workspaceData = workspaceData,
            selection = currentSelection()
                .copy(
                    segmentId = state.value.selectedSegmentId,
                    lapNumber = lapNumber,
                    referenceLapNumber = state.value.referenceLapNumber.takeIf { state.value.referenceLapIsCustom },
                ),
        )
    }

    private suspend fun selectSession(segmentId: Long?) {
        val workspaceData = binding.data ?: return
        publish(
            workspaceData = workspaceData,
            selection = currentSelection()
                .copy(
                    segmentId = segmentId,
                    lapNumber = null,
                    referenceLapNumber = null,
                ),
        )
    }

    private suspend fun selectReferenceLap(lapNumber: Int?) {
        val workspaceData = binding.data ?: return
        publish(
            workspaceData = workspaceData,
            selection = currentSelection()
                .copy(
                    segmentId = state.value.selectedSegmentId,
                    lapNumber = state.value.selectedLapNumber,
                    referenceLapNumber = lapNumber,
                ),
        )
    }

    private suspend fun publish(workspaceData: SessionAnalysisWorkspaceData, selection: SessionAnalysisSelection) {
        binding = binding.copy(
            selectedSegmentId = selection.segmentId,
            selectedLapNumber = selection.lapNumber,
            referenceSessionId = selection.referenceSessionId,
            referenceSegmentId = selection.referenceSegmentId,
            referenceLapNumber = selection.referenceLapNumber,
        )
        val cacheKey = selection.toCacheKey()
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
            selection = selection,
        )
        publishStateIfChanged(nextState)
    }

    private suspend fun resolveState(
        workspaceData: SessionAnalysisWorkspaceData,
        selection: SessionAnalysisSelection,
    ): SessionAnalysisState {
        val cacheKey = selection.toCacheKey()
        selectionStateCache[cacheKey]?.let { cachedState ->
            return cachedState
        }
        val nextState = withContext(defaultDispatcher) {
            workspaceData.report.toState(
                referenceReport = workspaceData.referenceReport,
                calibration = workspaceData.calibration,
                authoredTrackMap = workspaceData.authoredTrackMap,
                sourceTrackMap = workspaceData.sourceTrackMap,
                displayTrackMap = workspaceData.displayTrackMap,
                selectedSegmentId = selection.segmentId,
                selectedLapNumber = selection.lapNumber,
                selectedReferenceSegmentId = selection.referenceSegmentId,
                selectedReferenceLapNumber = selection.referenceLapNumber,
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

    private fun updateBinding(request: SessionAnalysisBindingRequest) {
        binding = binding.copy(
            requestedSessionId = request.sessionId,
            referenceSessionId = request.selection.referenceSessionId,
            referenceSegmentId = request.selection.referenceSegmentId,
            selectedSegmentId = request.selection.segmentId,
            selectedLapNumber = request.selection.lapNumber,
            referenceLapNumber = request.selection.referenceLapNumber,
        )
    }

    private fun currentBindingRequest(): SessionAnalysisBindingRequest? {
        val sessionId = binding.requestedSessionId ?: return null
        return SessionAnalysisBindingRequest(
            sessionId = sessionId,
            selection = currentSelection(),
        )
    }

    private fun currentSelection(): SessionAnalysisSelection = SessionAnalysisSelection(
        segmentId = binding.selectedSegmentId,
        lapNumber = binding.selectedLapNumber,
        referenceSessionId = binding.referenceSessionId,
        referenceSegmentId = binding.referenceSegmentId,
        referenceLapNumber = binding.referenceLapNumber,
    )

    private fun SessionBinding.matches(request: SessionAnalysisBindingRequest): Boolean =
        requestedSessionId == request.sessionId &&
            loadedSessionId == request.sessionId &&
            referenceSessionId == request.selection.referenceSessionId &&
            referenceSegmentId == request.selection.referenceSegmentId &&
            selectedSegmentId == request.selection.segmentId &&
            selectedLapNumber == request.selection.lapNumber &&
            referenceLapNumber == request.selection.referenceLapNumber

    private fun SessionAnalysisBindSessionIntent.toBindingRequest(): SessionAnalysisBindingRequest =
        SessionAnalysisBindingRequest(
            sessionId = sessionId,
            selection = SessionAnalysisSelection(
                segmentId = segmentId,
                lapNumber = lapNumber,
                referenceSessionId = referenceSessionId,
                referenceSegmentId = referenceSegmentId,
                referenceLapNumber = referenceLapNumber,
            ),
        )

    private fun SessionAnalysisSelection.hasExplicitSelection(): Boolean =
        segmentId != null || lapNumber != null || referenceLapNumber != null

    private fun SessionAnalysisSelection.resolveAgainst(
        currentState: SessionAnalysisState,
        preserveSelection: Boolean,
    ): SessionAnalysisSelection {
        if (!preserveSelection) return this
        return copy(
            segmentId = currentState.selectedSegmentId,
            lapNumber = currentState.selectedLapNumber,
            referenceLapNumber = currentState.referenceLapNumber.takeIf { currentState.referenceLapIsCustom },
        )
    }

    private fun SessionAnalysisSelection.toCacheKey(): SelectionStateKey = SelectionStateKey(
        segmentId = segmentId,
        lapNumber = lapNumber,
        referenceSessionId = referenceSessionId,
        referenceSegmentId = referenceSegmentId,
        referenceLapNumber = referenceLapNumber,
    )
}

private data class SessionAnalysisBindingRequest(val sessionId: Long, val selection: SessionAnalysisSelection)

private data class SessionAnalysisSelection(
    val segmentId: Long? = null,
    val lapNumber: Int? = null,
    val referenceSessionId: Long? = null,
    val referenceSegmentId: Long? = null,
    val referenceLapNumber: Int? = null,
)
