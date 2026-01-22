package com.analyzer.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.analyzer.settings.data.telemetry.SettingsRepository
import com.analyzer.settings.data.telemetry.SettingsRepositoryImpl.StorageValidationResult
import com.analyzer.settings.data.theme.ThemeRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Inject
internal class SettingsViewModel(
    private val themeRepository: ThemeRepository,
    private val telemetrySettingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

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
            telemetrySettingsRepository.observeSettings().collect { settings ->
                _state.update {
                    it.copy(
                        samplingRateHz = settings.samplingRateHz,
                        storageLocation = settings.storageLocation,
                    )
                }
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

    private fun handleChangeStorageLocation(path: String) {
        viewModelScope.launch {
            val validationResult = telemetrySettingsRepository.validateStorageLocation(path)
            val error = mapValidationError(validationResult)

//            _state.update { it.copy(storageLocationError = error) }

            if (validationResult == StorageValidationResult.Valid) {
                telemetrySettingsRepository.updateStorageLocation(path)
            }
        }
    }

    private fun mapValidationError(result: StorageValidationResult): String? {
        return when (result) {
            StorageValidationResult.Valid -> null
            StorageValidationResult.Empty -> "Storage location cannot be empty"
            StorageValidationResult.NotADirectory -> "Selected path is not a directory"
            StorageValidationResult.NotWritable -> "Cannot write to selected directory"
            StorageValidationResult.CannotCreate -> "Cannot create directory at selected path"
        }
    }
}
