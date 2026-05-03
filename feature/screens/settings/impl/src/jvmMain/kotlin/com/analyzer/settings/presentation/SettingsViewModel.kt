package com.analyzer.settings.presentation

import androidx.lifecycle.viewModelScope
import com.analyzer.settings.api.AppCloseBehavior
import com.analyzer.settings.domain.usecase.SettingsUseCase
import com.project.analyzer.game.api.GameId
import com.project.analyzer.game.api.GameSelection
import com.project.analyzer.leak.api.LeakAwareMviViewModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.nio.file.Path

@Inject
internal class SettingsViewModel(private val useCase: SettingsUseCase) :
    LeakAwareMviViewModel<SettingsIntent, SettingsState>(SettingsState()) {

    private var storageSizeJob: Job? = null
    private var lastStorageLocation: String? = null
    private var storageLocationValidationRequestId: Long = 0
    private var storageLocationInputDirty: Boolean = false

    init {
        observeTheme()
        observeAppCloseBehavior()
        observeTelemetrySettings()
        observeHudEnabled()
        viewModelScope.launch {
            useCase.warmUp()
        }
    }

    override suspend fun handleIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.ChangeTheme -> handleChangeTheme(intent.mode)
            is SettingsIntent.ChangeAppCloseBehavior -> handleChangeAppCloseBehavior(intent.behavior)
            is SettingsIntent.ChangeSamplingRate -> handleChangeSamplingRate(intent.hz)
            is SettingsIntent.ChangeStorageLocation -> handleChangeStorageLocation(intent.path)
            is SettingsIntent.ChangeStorageLocationInput -> handleChangeStorageLocationInput(intent.path)
            SettingsIntent.CommitStorageLocationInput -> commitStorageLocationInput()
            is SettingsIntent.ChangeHudEnabled -> handleChangeHudEnabled(intent.enabled)
            is SettingsIntent.ChangeRecordingEnabled -> handleChangeRecordingEnabled(intent.enabled)
            SettingsIntent.DismissRecordingEnabledNotice -> handleDismissRecordingEnabledNotice()
            is SettingsIntent.ChangeMaxRecordedLaps -> handleChangeMaxRecordedLaps(intent.laps)
            is SettingsIntent.ChangeGameSelection -> handleChangeGameSelection(intent.selection)
            SettingsIntent.ConfirmLmuPluginInstall -> handleConfirmLmuPluginInstall()
            SettingsIntent.DismissLmuPluginDialog -> handleDismissLmuPluginDialog()
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

    private fun observeAppCloseBehavior() {
        viewModelScope.launch {
            useCase.observeAppCloseBehavior().collect { behavior ->
                updateState { copy(appCloseBehavior = behavior) }
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
                        storageLocation = if (storageLocationInputDirty) storageLocation else settings.storageLocation,
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

    private fun handleChangeAppCloseBehavior(behavior: AppCloseBehavior) {
        viewModelScope.launch {
            useCase.updateAppCloseBehavior(behavior)
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
        if (selection == state.value.gameSelection) return
        if (selection is GameSelection.Manual && selection.game == GameId.LMU) {
            handleLmuSelectionRequested()
            return
        }
        viewModelScope.launch {
            useCase.updateGameSelection(selection)
        }
    }

    private fun handleLmuSelectionRequested() {
        viewModelScope.launch {
            when (val result = useCase.inspectLmuPluginSetup().toUiResult()) {
                LmuPluginSetupCheckUiResult.Ready -> {
                    useCase.updateGameSelection(GameSelection.Manual(GameId.LMU))
                }

                is LmuPluginSetupCheckUiResult.ShowDialog -> {
                    updateState { copy(lmuPluginDialog = result.dialogState) }
                }
            }
        }
    }

    private fun handleConfirmLmuPluginInstall() {
        val dialogState = state.value.lmuPluginDialog ?: return
        if (
            dialogState.phase == LmuPluginDialogPhase.Installing ||
            dialogState.phase == LmuPluginDialogPhase.MissingGame
        ) {
            return
        }

        viewModelScope.launch {
            updateState {
                copy(
                    lmuPluginDialog = dialogState.copy(
                        phase = LmuPluginDialogPhase.Installing,
                        progressStep = LmuPluginInstallStepUi.ResolvingSource,
                        detailMessage = null,
                    ),
                )
            }

            when (
                val result = useCase.installLmuPlugin(dialogState.details.toDomain()) { step ->
                    updateState {
                        val currentDialog = lmuPluginDialog ?: return@updateState this
                        copy(
                            lmuPluginDialog = currentDialog.copy(
                                phase = LmuPluginDialogPhase.Installing,
                                progressStep = step.toUi(),
                                detailMessage = null,
                            ),
                        )
                    }
                }.toUiResult()
            ) {
                is LmuPluginInstallUiResult.Success -> {
                    useCase.updateGameSelection(GameSelection.Manual(GameId.LMU))
                    updateState { copy(lmuPluginDialog = result.dialogState) }
                }

                is LmuPluginInstallUiResult.Failure -> {
                    updateState { copy(lmuPluginDialog = result.dialogState) }
                }
            }
        }
    }

    private fun handleDismissLmuPluginDialog() {
        updateState { copy(lmuPluginDialog = null) }
    }

    private fun handleChangeStorageLocation(path: String) {
        val normalized = path.trim()
        storageLocationInputDirty = true
        updateState {
            copy(
                storageLocation = normalized,
                storageLocationError = null,
                isStorageLocationValid = true,
            )
        }
        validateStorageLocation(normalized)
    }

    private fun handleChangeStorageLocationInput(path: String) {
        storageLocationInputDirty = true
        updateState {
            copy(
                storageLocation = path,
                storageLocationError = null,
                isStorageLocationValid = true,
            )
        }
    }

    private fun commitStorageLocationInput() {
        val normalized = state.value.storageLocation.trim()
        updateState { copy(storageLocation = normalized) }
        validateStorageLocation(normalized)
    }

    private fun validateStorageLocation(path: String) {
        val normalizedPath = path.trim()
        val requestId = ++storageLocationValidationRequestId
        viewModelScope.launch {
            val result = useCase.updateStorageLocationIfValid(normalizedPath).toUi()
            if (requestId != storageLocationValidationRequestId) return@launch

            if (result == null) {
                val resolvedPath = resolveTelemetryStoragePath(normalizedPath)
                storageLocationInputDirty = false
                updateState {
                    copy(
                        storageLocation = resolvedPath,
                        isStorageLocationValid = true,
                        storageLocationError = null,
                    )
                }
                updateStorageSize(resolvedPath)
            } else {
                updateState {
                    copy(
                        isStorageLocationValid = false,
                        storageLocationError = result,
                    )
                }
            }
        }
    }

    private fun updateStorageSize(path: String) {
        if (path == lastStorageLocation) return
        lastStorageLocation = path
        if (path.isBlank()) {
            updateState {
                copy(
                    storageSizeInfo = StorageSizeInfo.Unknown,
                )
            }
            return
        }

        storageSizeJob?.cancel()
        storageSizeJob = viewModelScope.launch {
            val size = useCase.getStorageSizeBytes(path)
            updateState {
                copy(
                    storageSizeInfo = size.toStorageSizeInfo(),
                )
            }
        }
    }

    private fun Long?.toStorageSizeInfo(): StorageSizeInfo {
        val value = this ?: return StorageSizeInfo.Unknown
        if (value <= 0L) return StorageSizeInfo.Zero

        var size = value.toDouble()
        var unitIndex = 0
        while (size >= 1024.0 && unitIndex < 4) {
            size /= 1024.0
            unitIndex += 1
        }

        val fractionDigits = when {
            size >= 100 -> 0
            size >= 10 -> 1
            else -> 2
        }

        val unit = when (unitIndex) {
            0 -> StorageSizeUnit.B
            1 -> StorageSizeUnit.KB
            2 -> StorageSizeUnit.MB
            3 -> StorageSizeUnit.GB
            else -> StorageSizeUnit.TB
        }

        return StorageSizeInfo.Value(
            size = size,
            fractionDigits = fractionDigits,
            unit = unit,
        )
    }

    private fun resolveTelemetryStoragePath(path: String): String {
        val basePath = Path.of(path.trim()).normalize()
        val lastSegment = basePath.fileName?.toString()
        return if (lastSegment != null && lastSegment.equals(TELEMETRY_DIRECTORY_NAME, ignoreCase = true)) {
            basePath.toString()
        } else {
            basePath.resolve(TELEMETRY_DIRECTORY_NAME).toString()
        }
    }

    private companion object {

        const val TELEMETRY_DIRECTORY_NAME = "telemetry"
    }
}
