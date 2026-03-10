package com.analyzer.trackmap.presentation

import androidx.lifecycle.viewModelScope
import com.analyzer.trackmap.domain.usecase.editor.TrackMapCalibrationEditorReducer
import com.analyzer.trackmap.domain.usecase.editor.TrackMapCalibrationEditorUseCase
import com.analyzer.trackmap.domain.usecase.live.ObserveTrackMapLivePositionUseCase
import com.analyzer.trackmap.presentation.state.TrackMapCalibrationEditorState
import com.analyzer.trackmap.presentation.state.toSaveFailureTrackMapCalibrationEditorState
import com.analyzer.trackmap.presentation.state.toSavedTrackMapCalibrationEditorState
import com.analyzer.trackmap.presentation.state.toSavingTrackMapCalibrationEditorState
import com.analyzer.trackmap.presentation.state.toTrackMapCalibrationEditorState
import com.analyzer.trackmap.presentation.state.trackMapCalibrationEditorLoadFailureState
import com.analyzer.trackmap.presentation.state.trackMapCalibrationEditorLoadingState
import com.analyzer.trackmap.presentation.state.trackMapCalibrationEditorNotFoundState
import com.project.analyzer.leak.api.LeakAwareViewModel
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Inject
internal class TrackMapCalibrationEditorViewModel(
    private val useCase: TrackMapCalibrationEditorUseCase,
    private val reducer: TrackMapCalibrationEditorReducer,
    private val observeLivePositionUseCase: ObserveTrackMapLivePositionUseCase,
) : LeakAwareViewModel() {

    private val logger = logger()

    private val _state = MutableStateFlow(TrackMapCalibrationEditorState())
    val state: StateFlow<TrackMapCalibrationEditorState> = _state.asStateFlow()

    private var loadedMapKey: String? = null
    private var loadJob: Job? = null
    private var livePositionJob: Job? = null

    init {
        logger.info { "TrackMapEditor.vm init instance=${System.identityHashCode(this)}" }
    }

    fun load(gameId: String, trackId: String, layoutId: String?) {
        val mapKey = buildTrackMapCalibrationEditorMapKey(
            gameId = gameId,
            trackId = trackId,
            layoutId = layoutId,
        )
        if (mapKey == loadedMapKey && (_state.value.item != null || _state.value.isLoading)) return

        loadedMapKey = mapKey
        loadJob?.cancel()
        livePositionJob?.cancel()
        _state.value = trackMapCalibrationEditorLoadingState()
        logger.info { "TrackMapEditor.load start mapKey=$mapKey instance=${System.identityHashCode(this)}" }

        loadJob = viewModelScope.launch {
            runCatching {
                useCase.load(
                    gameId = gameId,
                    trackId = trackId,
                    layoutId = layoutId,
                )
            }.onSuccess { content ->
                if (!isActive || loadedMapKey != mapKey) return@onSuccess
                if (content == null) {
                    logger.warn { "TrackMapEditor.load not found mapKey=$mapKey" }
                    _state.value = trackMapCalibrationEditorNotFoundState(trackId)
                    return@onSuccess
                }

                _state.value = content.toTrackMapCalibrationEditorState()
                logger.info {
                    buildString {
                        append("TrackMapEditor.load success mapKey=").append(mapKey)
                        append(" source=").append(content.snapshot.source)
                        append(" gates=").append(content.snapshot.gates.size)
                    }
                }
                observeLivePosition(mapKey = mapKey, item = content.item)
            }.onFailure { error ->
                if (!isActive || loadedMapKey != mapKey) return@onFailure
                logger.error(error) { "TrackMapEditor.load failure mapKey=$mapKey" }
                _state.value = trackMapCalibrationEditorLoadFailureState(
                    error.message ?: "Failed to load track map",
                )
            }
        }
    }

    fun selectMarker(gateId: String) {
        _state.update { state ->
            state.copy(
                current = reducer.selectMarker(
                    snapshot = state.current,
                    gateId = gateId,
                ),
            )
        }
    }

    fun updateGate(gateId: String, gate: Gate) {
        val item = _state.value.item ?: return
        _state.update { state ->
            state.copy(
                current = reducer.updateGate(
                    item = item,
                    snapshot = state.current,
                    gateId = gateId,
                    gate = gate,
                ),
                message = null,
            )
        }
    }

    fun addGateAfterSelected(gate: Gate) {
        val item = _state.value.item ?: return
        _state.update { state ->
            val hadNoGates = state.gates.isEmpty()
            state.copy(
                current = reducer.addGateAfterSelected(
                    item = item,
                    snapshot = state.current,
                    gate = gate,
                ),
                message = if (hadNoGates) "Start / Finish created" else "Point added",
            )
        }
    }

    fun deleteGate(gateId: String) {
        val item = _state.value.item ?: return
        _state.update { state ->
            state.copy(
                current = reducer.deleteGate(
                    item = item,
                    snapshot = state.current,
                    gateId = gateId,
                ),
                message = "Point removed",
            )
        }
    }

    fun reset() {
        _state.update { state ->
            state.copy(
                current = state.original,
                message = "Changes reset",
            )
        }
    }

    fun save() {
        val state = _state.value
        val item = state.item ?: return
        if (!state.canSave) return
        val snapshotToSave = state.current

        viewModelScope.launch {
            _state.update(TrackMapCalibrationEditorState::toSavingTrackMapCalibrationEditorState)

            runCatching {
                useCase.save(
                    item = item,
                    snapshot = snapshotToSave,
                )
            }.onSuccess { savedSnapshot ->
                _state.update { currentState ->
                    currentState.toSavedTrackMapCalibrationEditorState(
                        snapshot = savedSnapshot,
                        trackId = item.map.trackId,
                    )
                }
            }.onFailure { error ->
                _state.update { currentState ->
                    currentState.toSaveFailureTrackMapCalibrationEditorState(
                        message = error.message ?: "Failed to save calibration",
                    )
                }
            }
        }
    }

    private fun observeLivePosition(mapKey: String, item: com.analyzer.trackmap.domain.model.TrackMapLibraryItem) {
        livePositionJob?.cancel()
        livePositionJob = viewModelScope.launch {
            observeLivePositionUseCase.observe(item).collect { position ->
                if (!isActive || loadedMapKey != mapKey) return@collect
                _state.update { currentState ->
                    currentState.copy(livePosition = position)
                }
            }
        }
    }
}
