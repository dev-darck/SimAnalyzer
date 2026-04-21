package com.analyzer.settings.domain.usecase

import com.analyzer.settings.api.AppCloseBehavior
import com.analyzer.settings.domain.model.LmuPluginInstallResult
import com.analyzer.settings.domain.model.LmuPluginInstallStep
import com.analyzer.settings.domain.model.LmuPluginSetupCheckResult
import com.analyzer.settings.domain.model.LmuPluginSetupDetails
import com.analyzer.settings.domain.model.StorageValidationResult
import com.analyzer.settings.domain.model.TelemetrySettings
import com.project.analyzer.game.api.GameId
import com.project.analyzer.game.api.GameSelection
import com.project.analyzer.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

internal interface SettingsUseCase {

    fun observeAppCloseBehavior(): Flow<AppCloseBehavior>
    fun observeThemeMode(): Flow<ThemeMode>
    fun observeHudEnabled(): Flow<Boolean>
    fun observeRecordingNoticeShown(): Flow<Boolean>
    fun observeTelemetrySettings(): Flow<Pair<TelemetrySettings, GameId?>>
    suspend fun warmUp()
    suspend fun updateAppCloseBehavior(behavior: AppCloseBehavior)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun updateSamplingRate(hz: Int)
    suspend fun updateHudEnabled(enabled: Boolean)
    suspend fun updateRecordingEnabled(enabled: Boolean)
    suspend fun markRecordingNoticeShown()
    suspend fun updateMaxRecordedLaps(laps: Int)
    suspend fun updateGameSelection(selection: GameSelection)
    suspend fun inspectLmuPluginSetup(): LmuPluginSetupCheckResult
    suspend fun installLmuPlugin(
        details: LmuPluginSetupDetails,
        onProgress: (LmuPluginInstallStep) -> Unit = {},
    ): LmuPluginInstallResult

    suspend fun updateStorageLocationIfValid(path: String): StorageValidationResult
    suspend fun getStorageSizeBytes(path: String): Long?
}
