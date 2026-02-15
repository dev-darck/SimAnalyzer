package com.analyzer.settings.data.telemetry

import com.analyzer.settings.data.telemetry.SettingsRepositoryImpl.StorageValidationResult
import com.analyzer.settings.domain.model.TelemetrySettings
import com.project.analyzer.game.api.GameId
import com.project.analyzer.game.api.GameSelection
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    fun observeSettings(): Flow<TelemetrySettings>
    fun observeGameSelectionVariant(): Flow<GameId?>
    fun observeHudEnabled(): Flow<Boolean>
    suspend fun loadSettings(): TelemetrySettings
    suspend fun updateSamplingRate(hz: Int)
    suspend fun updateHudEnabled(enabled: Boolean)
    suspend fun updateStorageLocation(path: String)
    suspend fun updateRecordingEnabled(enabled: Boolean)
    suspend fun updateMaxRecordedLaps(laps: Int)
    suspend fun updateGameSelection(selection: GameSelection)
    suspend fun updateGameSelectionVariant(gameId: GameId?)
    suspend fun getStorageSizeBytes(path: String): Long?
    fun getDefaultStorageLocation(): String
    fun validateStorageLocation(path: String): StorageValidationResult
}
