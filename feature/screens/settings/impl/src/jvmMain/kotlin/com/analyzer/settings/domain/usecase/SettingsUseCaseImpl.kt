package com.analyzer.settings.domain.usecase

import com.analyzer.settings.data.telemetry.SettingsRepository
import com.analyzer.settings.data.theme.ThemeRepository
import com.analyzer.settings.domain.model.StorageValidationResult
import com.analyzer.settings.domain.model.TelemetrySettings
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.game.api.GameId
import com.project.analyzer.game.api.GameSelection
import com.project.analyzer.theme.ThemeMode
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.nio.file.Path

@Inject
@SingleIn(ScreenScope::class)
class SettingsUseCaseImpl(
    private val themeRepository: ThemeRepository,
    private val telemetrySettingsRepository: SettingsRepository,
) : SettingsUseCase {

    override fun observeThemeMode(): Flow<ThemeMode> = themeRepository.observeThemeMode()

    override fun observeHudEnabled(): Flow<Boolean> = telemetrySettingsRepository.observeHudEnabled()

    override fun observeRecordingNoticeShown(): Flow<Boolean> =
        telemetrySettingsRepository.observeRecordingNoticeShown()

    override fun observeTelemetrySettings(): Flow<Pair<TelemetrySettings, GameId?>> = combine(
        telemetrySettingsRepository.observeSettings(),
        telemetrySettingsRepository.observeGameSelectionVariant(),
    ) { settings, variant -> settings to variant }

    override suspend fun warmUp() {
        telemetrySettingsRepository.loadSettings()
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeRepository.setThemeMode(mode)
    }

    override suspend fun updateSamplingRate(hz: Int) {
        telemetrySettingsRepository.updateSamplingRate(hz)
    }

    override suspend fun updateHudEnabled(enabled: Boolean) {
        telemetrySettingsRepository.updateHudEnabled(enabled)
    }

    override suspend fun updateRecordingEnabled(enabled: Boolean) {
        telemetrySettingsRepository.updateRecordingEnabled(enabled)
    }

    override suspend fun markRecordingNoticeShown() {
        telemetrySettingsRepository.markRecordingNoticeShown()
    }

    override suspend fun updateMaxRecordedLaps(laps: Int) {
        telemetrySettingsRepository.updateMaxRecordedLaps(laps)
    }

    override suspend fun updateGameSelection(selection: GameSelection) {
        telemetrySettingsRepository.updateGameSelection(selection)
        val variant = (selection as? GameSelection.Manual)?.game
        telemetrySettingsRepository.updateGameSelectionVariant(variant)
    }

    override suspend fun updateStorageLocationIfValid(path: String): StorageValidationResult {
        if (path.isBlank()) return StorageValidationResult.Empty

        val targetTelemetryPath = resolveTelemetryDirectoryPath(path)
        val currentSettings = telemetrySettingsRepository.loadSettings()
        val currentTelemetryPath = currentSettings.storageLocation
            .takeIf { it.isNotBlank() }
            ?.let(::resolveTelemetryDirectoryPath)

        val validationResult = telemetrySettingsRepository.validateStorageLocation(targetTelemetryPath)
        if (validationResult != StorageValidationResult.Valid) return validationResult

        if (currentTelemetryPath != null) {
            val copied = telemetrySettingsRepository.copyFromOldDir(
                currentTelemetryPath = currentTelemetryPath,
                targetTelemetryPath = targetTelemetryPath,
            )
            if (!copied) return StorageValidationResult.CannotCreate
        }

        telemetrySettingsRepository.updateStorageLocation(targetTelemetryPath)
        return StorageValidationResult.Valid
    }

    override suspend fun getStorageSizeBytes(path: String): Long? =
        telemetrySettingsRepository.getStorageSizeBytes(path)

    private fun resolveTelemetryDirectoryPath(path: String): String {
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
