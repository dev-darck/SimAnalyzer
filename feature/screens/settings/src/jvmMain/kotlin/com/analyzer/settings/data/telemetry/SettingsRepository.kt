package com.analyzer.settings.data.telemetry

import com.analyzer.settings.data.telemetry.SettingsRepositoryImpl.StorageValidationResult
import com.analyzer.settings.domain.model.TelemetrySettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    fun observeSettings(): Flow<TelemetrySettings>
    fun observeHudEnabled(): Flow<Boolean>
    fun getSettings(): TelemetrySettings
    suspend fun loadSettings()
    suspend fun updateSamplingRate(hz: Int)
    suspend fun updateHudEnabled(enabled: Boolean)
    suspend fun updateStorageLocation(path: String)
    fun getDefaultStorageLocation(): String
    fun validateStorageLocation(path: String): StorageValidationResult
}
