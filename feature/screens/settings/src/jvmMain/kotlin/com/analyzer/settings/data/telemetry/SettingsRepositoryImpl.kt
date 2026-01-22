package com.analyzer.settings.data.telemetry

import com.analyzer.settings.domain.model.TelemetrySettings
import com.project.analyzer.preference.api.Preference
import com.project.analyzer.preference.api.UserPref
import com.project.analyzer.preference.api.bool
import com.project.analyzer.preference.api.int
import com.project.analyzer.preference.api.str
import com.project.analyzer.utils.AppDirectories
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

@Inject
class SettingsRepositoryImpl(
    @param:UserPref
    private val userPreferences: Preference,
    private val appDirectories: AppDirectories,
) : SettingsRepository {

    private val _settings = MutableStateFlow(TelemetrySettings())

    override fun observeSettings(): Flow<TelemetrySettings> = _settings.asStateFlow()
    override fun observeHudEnabled(): Flow<Boolean> = userPreferences.observe(TELEMETRY_HUD_ENABLED.bool, true)

    override fun getSettings(): TelemetrySettings = _settings.value

    override suspend fun loadSettings() {
        val location = userPreferences.get(TELEMETRY_SETTINGS_LOCATION.str, getDefaultStorageLocation())
        val rate = userPreferences.get(TELEMETRY_SETTINGS_RATE.int, 50)
        _settings.value = TelemetrySettings(samplingRateHz = rate, storageLocation = location)
    }

    override suspend fun updateSamplingRate(hz: Int) {
        val clamped = hz.coerceIn(
            TelemetrySettings.MIN_SAMPLING_RATE_HZ,
            TelemetrySettings.MAX_SAMPLING_RATE_HZ
        )
        userPreferences.put(TELEMETRY_SETTINGS_RATE.int to clamped)
    }

    override suspend fun updateHudEnabled(enabled: Boolean) {
        userPreferences.put(TELEMETRY_HUD_ENABLED.bool to enabled)
    }

    override suspend fun updateStorageLocation(path: String) {
        userPreferences.put(TELEMETRY_SETTINGS_LOCATION.str to path)
    }

    override fun getDefaultStorageLocation(): String {
        return appDirectories.cacheDir.absolutePath
    }

    override fun validateStorageLocation(path: String): StorageValidationResult {
        if (path.isBlank()) {
            return StorageValidationResult.Empty
        }

        val file = File(path)

        return when {
            !file.exists() -> {
                val created = runCatching { file.mkdirs() }.getOrDefault(false)
                if (created) StorageValidationResult.Valid else StorageValidationResult.CannotCreate
            }

            !file.isDirectory -> StorageValidationResult.NotADirectory
            !file.canWrite() -> StorageValidationResult.NotWritable
            else -> StorageValidationResult.Valid
        }
    }

    sealed interface StorageValidationResult {
        data object Valid : StorageValidationResult
        data object Empty : StorageValidationResult
        data object NotADirectory : StorageValidationResult
        data object NotWritable : StorageValidationResult
        data object CannotCreate : StorageValidationResult
    }

    private companion object {

        const val TELEMETRY_HUD_ENABLED = "telemetry_hud_enabled"
        const val TELEMETRY_SETTINGS_RATE = "telemetry_settings_rate"
        const val TELEMETRY_SETTINGS_LOCATION = "telemetry_settings_location"
    }
}
