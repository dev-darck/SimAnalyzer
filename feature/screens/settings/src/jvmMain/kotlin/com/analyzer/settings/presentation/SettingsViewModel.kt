package com.analyzer.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.analyzer.settings.data.telemetry.SettingsRepository
import com.analyzer.settings.data.telemetry.SettingsRepositoryImpl.StorageValidationResult
import com.analyzer.settings.data.theme.ThemeRepository
import com.project.analyzer.game.api.GameSelection
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Inject
internal class SettingsViewModel(
    private val themeRepository: ThemeRepository,
    private val telemetrySettingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()
    private var storageSizeJob: Job? = null
    private var lastStorageLocation: String? = null

    init {
        observeTheme()
        observeTelemetrySettings()
        observeHudEnabled()
        viewModelScope.launch {
            telemetrySettingsRepository.loadSettings()
        }
    }

    fun dispatch(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.ChangeTheme -> handleChangeTheme(intent.mode)
            is SettingsIntent.ChangeSamplingRate -> handleChangeSamplingRate(intent.hz)
            is SettingsIntent.ChangeStorageLocation -> handleChangeStorageLocation(intent.path)
            is SettingsIntent.ChangeHudEnabled -> handleChangeHudEnabled(intent.enabled)
            is SettingsIntent.ChangeRecordingEnabled -> handleChangeRecordingEnabled(intent.enabled)
            is SettingsIntent.ChangeMaxRecordedLaps -> handleChangeMaxRecordedLaps(intent.laps)
            is SettingsIntent.ChangeGameSelection -> handleChangeGameSelection(intent.selection)
        }
    }

    private fun handleChangeHudEnabled(enabled: Boolean) {
        viewModelScope.launch {
            telemetrySettingsRepository.updateHudEnabled(enabled)
        }
    }

    private fun observeTheme() {
        viewModelScope.launch {
            themeRepository.observeThemeMode().collect { mode ->
                _state.update { it.copy(themeMode = mode) }
            }
        }
    }

    private fun observeHudEnabled() {
        viewModelScope.launch {
            telemetrySettingsRepository.observeHudEnabled().collect { enabled ->
                _state.update { it.copy(hudEnabled = enabled) }
            }
        }
    }

    private fun observeTelemetrySettings() {
        viewModelScope.launch {
            combine(
                telemetrySettingsRepository.observeSettings(),
                telemetrySettingsRepository.observeGameSelectionVariant(),
            ) { settings, variant -> settings to variant }
                .collect { (settings, variant) ->
                    val warning = buildRecordingWarning(
                        recordingEnabled = settings.recordingEnabled,
                        samplingRateHz = settings.samplingRateHz,
                        maxRecordedLaps = settings.maxRecordedLaps,
                    )
                    val selectionUi = buildGameSelectionUi(settings.gameSelection, variant)
                    _state.update {
                        it.copy(
                            samplingRateHz = settings.samplingRateHz,
                            storageLocation = settings.storageLocation,
                            recordingEnabled = settings.recordingEnabled,
                            maxRecordedLaps = settings.maxRecordedLaps,
                            gameSelection = settings.gameSelection,
                            gameSelectionUi = selectionUi,
                            recordingWarning = warning,
                        )
                    }
                    updateStorageSize(settings.storageLocation)
                }
        }
    }

    private fun handleChangeTheme(mode: com.project.analyzer.theme.ThemeMode) {
        viewModelScope.launch {
            themeRepository.setThemeMode(mode)
        }
    }

    private fun handleChangeSamplingRate(hz: Int) {
        viewModelScope.launch {
            telemetrySettingsRepository.updateSamplingRate(hz)
        }
    }

    private fun handleChangeRecordingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            telemetrySettingsRepository.updateRecordingEnabled(enabled)
        }
    }

    private fun handleChangeMaxRecordedLaps(laps: Int) {
        viewModelScope.launch {
            telemetrySettingsRepository.updateMaxRecordedLaps(laps)
        }
    }

    private fun handleChangeGameSelection(selection: GameSelection) {
        viewModelScope.launch {
            telemetrySettingsRepository.updateGameSelection(selection)
            val variant = (selection as? GameSelection.Manual)?.game
            telemetrySettingsRepository.updateGameSelectionVariant(variant)
        }
    }

    private fun handleChangeStorageLocation(path: String) {
        viewModelScope.launch {
            val validationResult = telemetrySettingsRepository.validateStorageLocation(path)

            if (validationResult == StorageValidationResult.Valid) {
                telemetrySettingsRepository.updateStorageLocation(path)
            }
        }
    }

    private fun updateStorageSize(path: String) {
        if (path == lastStorageLocation) return
        lastStorageLocation = path
        if (path.isBlank()) {
            _state.update {
                it.copy(
                    storageSizeBytes = null,
                    storageSizeLabel = formatStorageSizeLabel(null),
                )
            }
            return
        }

        storageSizeJob?.cancel()
        storageSizeJob = viewModelScope.launch {
            val size = telemetrySettingsRepository.getStorageSizeBytes(path)
            _state.update {
                it.copy(
                    storageSizeBytes = size,
                    storageSizeLabel = formatStorageSizeLabel(size),
                )
            }
        }
    }
}
