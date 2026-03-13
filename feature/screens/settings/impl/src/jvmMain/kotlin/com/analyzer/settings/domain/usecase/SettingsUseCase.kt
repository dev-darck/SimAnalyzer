package com.analyzer.settings.domain.usecase

import com.analyzer.settings.domain.model.TelemetrySettings
import com.analyzer.settings.domain.model.StorageValidationResult
import com.project.analyzer.game.api.GameId
import com.project.analyzer.game.api.GameSelection
import com.project.analyzer.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsUseCase {

    fun observeThemeMode(): Flow<ThemeMode>
    fun observeHudEnabled(): Flow<Boolean>
    fun observeRecordingNoticeShown(): Flow<Boolean>
    fun observeTelemetrySettings(): Flow<Pair<TelemetrySettings, GameId?>>
    suspend fun warmUp()
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun updateSamplingRate(hz: Int)
    suspend fun updateHudEnabled(enabled: Boolean)
    suspend fun updateRecordingEnabled(enabled: Boolean)
    suspend fun markRecordingNoticeShown()
    suspend fun updateMaxRecordedLaps(laps: Int)
    suspend fun updateGameSelection(selection: GameSelection)
    suspend fun updateStorageLocationIfValid(path: String): StorageValidationResult
    suspend fun getStorageSizeBytes(path: String): Long?
}
