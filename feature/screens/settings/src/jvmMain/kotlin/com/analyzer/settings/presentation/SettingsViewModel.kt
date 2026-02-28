package com.analyzer.settings.presentation

import androidx.lifecycle.viewModelScope
import com.analyzer.settings.domain.interactor.SettingsUseCase
import com.project.analyzer.game.api.GameSelection
import com.project.analyzer.leak.api.LeakAwareMviViewModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@Inject
internal class SettingsViewModel(private val useCase: SettingsUseCase) :
    LeakAwareMviViewModel<SettingsIntent, SettingsState>(SettingsState()) {

    private var storageSizeJob: Job? = null
    private var lastStorageLocation: String? = null

    init {
        observeTheme()
        observeTelemetrySettings()
        observeHudEnabled()
        viewModelScope.launch {
            useCase.warmUp()
        }
    }

    override suspend fun handleIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.ChangeTheme -> handleChangeTheme(intent.mode)
            is SettingsIntent.ChangeSamplingRate -> handleChangeSamplingRate(intent.hz)
            is SettingsIntent.ChangeStorageLocation -> handleChangeStorageLocation(intent.path)
            is SettingsIntent.ChangeHudEnabled -> handleChangeHudEnabled(intent.enabled)
            is SettingsIntent.ChangeRecordingEnabled -> handleChangeRecordingEnabled(intent.enabled)
            SettingsIntent.DismissRecordingEnabledNotice -> handleDismissRecordingEnabledNotice()
            is SettingsIntent.ChangeMaxRecordedLaps -> handleChangeMaxRecordedLaps(intent.laps)
            is SettingsIntent.ChangeGameSelection -> handleChangeGameSelection(intent.selection)
        }
    }

    private fun handleChangeHudEnabled(enabled: Boolean) {
        viewModelScope.launch {
            useCase.updateHudEnabled(enabled)
        }
    }

    private fun observeTheme() {
        viewModelScope.launch {
            useCase.observeThemeMode().collect { mode ->
                updateState { copy(themeMode = mode) }
            }
        }
    }

    private fun observeHudEnabled() {
        viewModelScope.launch {
            useCase.observeHudEnabled().collect { enabled ->
                updateState { copy(hudEnabled = enabled) }
            }
        }
    }

    private fun observeTelemetrySettings() {
        viewModelScope.launch {
            combine(
                useCase.observeTelemetrySettings(),
                useCase.observeRecordingNoticeShown(),
            ) { telemetry, recordingNoticeShown ->
                telemetry to recordingNoticeShown
            }.collect { (telemetry, recordingNoticeShown) ->
                val (settings, variant) = telemetry
                val warning = buildRecordingWarning(
                    recordingEnabled = settings.recordingEnabled,
                    samplingRateHz = settings.samplingRateHz,
                    maxRecordedLaps = settings.maxRecordedLaps,
                )
                val selectionUi = buildGameSelectionUi(settings.gameSelection, variant)
                var shouldPersistNoticeShown = false
                updateState {
                    val shouldShowRecordingNotice =
                        !recordingNoticeShown && !recordingEnabled && settings.recordingEnabled
                    shouldPersistNoticeShown = shouldShowRecordingNotice
                    copy(
                        samplingRateHz = settings.samplingRateHz,
                        storageLocation = settings.storageLocation,
                        recordingEnabled = settings.recordingEnabled,
                        maxRecordedLaps = settings.maxRecordedLaps,
                        gameSelection = settings.gameSelection,
                        gameSelectionUi = selectionUi,
                        recordingWarning = warning,
                        showRecordingEnabledNotice = shouldShowRecordingNotice,
                    )
                }
                if (shouldPersistNoticeShown) {
                    useCase.markRecordingNoticeShown()
                }
                updateStorageSize(settings.storageLocation)
            }
        }
    }

    private fun handleChangeTheme(mode: com.project.analyzer.theme.ThemeMode) {
        viewModelScope.launch {
            useCase.setThemeMode(mode)
        }
    }

    private fun handleChangeSamplingRate(hz: Int) {
        viewModelScope.launch {
            useCase.updateSamplingRate(hz)
        }
    }

    private fun handleChangeRecordingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            useCase.updateRecordingEnabled(enabled)
        }
    }

    private fun handleDismissRecordingEnabledNotice() {
        updateState { copy(showRecordingEnabledNotice = false) }
    }

    private fun handleChangeMaxRecordedLaps(laps: Int) {
        viewModelScope.launch {
            useCase.updateMaxRecordedLaps(laps)
        }
    }

    private fun handleChangeGameSelection(selection: GameSelection) {
        viewModelScope.launch {
            useCase.updateGameSelection(selection)
        }
    }

    private fun handleChangeStorageLocation(path: String) {
        viewModelScope.launch {
            useCase.updateStorageLocationIfValid(path)
        }
    }

    private fun updateStorageSize(path: String) {
        if (path == lastStorageLocation) return
        lastStorageLocation = path
        if (path.isBlank()) {
            updateState {
                copy(
                    storageSizeBytes = null,
                )
            }
            return
        }

        storageSizeJob?.cancel()
        storageSizeJob = viewModelScope.launch {
            val size = useCase.getStorageSizeBytes(path)
            updateState {
                copy(
                    storageSizeBytes = size,
                )
            }
        }
    }
}
