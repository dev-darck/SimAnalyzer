package com.analyzer.settings.domain.interactor

import com.analyzer.settings.data.telemetry.SettingsRepository
import com.analyzer.settings.data.telemetry.StorageValidationResult
import com.analyzer.settings.data.theme.ThemeRepository
import com.analyzer.settings.domain.model.TelemetrySettings
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.game.api.GameId
import com.project.analyzer.game.api.GameSelection
import com.project.analyzer.theme.ThemeMode
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Inject
@SingleIn(ScreenScope::class)
class SettingsUseCase(
    private val themeRepository: ThemeRepository,
    private val telemetrySettingsRepository: SettingsRepository,
) {

    fun observeThemeMode(): Flow<ThemeMode> = themeRepository.observeThemeMode()

    fun observeHudEnabled(): Flow<Boolean> = telemetrySettingsRepository.observeHudEnabled()

    fun observeRecordingNoticeShown(): Flow<Boolean> = telemetrySettingsRepository.observeRecordingNoticeShown()

    fun observeTelemetrySettings(): Flow<Pair<TelemetrySettings, GameId?>> = combine(
        telemetrySettingsRepository.observeSettings(),
        telemetrySettingsRepository.observeGameSelectionVariant(),
    ) { settings, variant -> settings to variant }

    suspend fun warmUp() {
        telemetrySettingsRepository.loadSettings()
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        themeRepository.setThemeMode(mode)
    }

    suspend fun updateSamplingRate(hz: Int) {
        telemetrySettingsRepository.updateSamplingRate(hz)
    }

    suspend fun updateHudEnabled(enabled: Boolean) {
        telemetrySettingsRepository.updateHudEnabled(enabled)
    }

    suspend fun updateRecordingEnabled(enabled: Boolean) {
        telemetrySettingsRepository.updateRecordingEnabled(enabled)
    }

    suspend fun markRecordingNoticeShown() {
        telemetrySettingsRepository.markRecordingNoticeShown()
    }

    suspend fun updateMaxRecordedLaps(laps: Int) {
        telemetrySettingsRepository.updateMaxRecordedLaps(laps)
    }

    suspend fun updateGameSelection(selection: GameSelection) {
        telemetrySettingsRepository.updateGameSelection(selection)
        val variant = (selection as? GameSelection.Manual)?.game
        telemetrySettingsRepository.updateGameSelectionVariant(variant)
    }

    suspend fun updateStorageLocationIfValid(path: String): Boolean {
        val validationResult = telemetrySettingsRepository.validateStorageLocation(path)
        if (validationResult != StorageValidationResult.Valid) return false
        telemetrySettingsRepository.updateStorageLocation(path)
        return true
    }

    suspend fun getStorageSizeBytes(path: String): Long? = telemetrySettingsRepository.getStorageSizeBytes(path)
}
