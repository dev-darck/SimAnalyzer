package com.analyzer.settings.data.telemetry

import com.analyzer.settings.api.AppCloseBehavior
import com.analyzer.settings.api.AppCloseBehaviorRepository
import com.analyzer.settings.domain.model.StorageValidationResult
import com.analyzer.settings.domain.model.TelemetrySettings
import com.project.analyzer.api.di.IO
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.game.api.GameId
import com.project.analyzer.game.api.GameSelection
import com.project.analyzer.game.api.toPreferenceValue
import com.project.analyzer.preference.api.Preference
import com.project.analyzer.preference.api.StringPrefKey
import com.project.analyzer.preference.api.UserPref
import com.project.analyzer.preference.api.bool
import com.project.analyzer.preference.api.str
import com.project.analyzer.telemetry.api.contract.TelemetryGameDefaults
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionDefaults
import com.project.analyzer.utils.AppDirectories
import com.project.analyzer.utils.file.copyDirectoryWithRollback
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

@Inject
@SingleIn(ScreenScope::class)
internal class SettingsRepositoryImpl(
    @param:UserPref
    private val userPreferences: Preference,
    private val appCloseBehaviorRepository: AppCloseBehaviorRepository,
    private val appDirectories: AppDirectories,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : SettingsRepository {

    override fun observeSettings(): Flow<TelemetrySettings> = combine(
        userPreferences.observe(
            TelemetryAcquisitionDefaults.KEY_SAMPLING_RATE,
            TelemetryAcquisitionDefaults.DEFAULT_SAMPLING_RATE_HZ,
        ),
        userPreferences.observe(
            TelemetryAcquisitionDefaults.KEY_STORAGE_LOCATION,
            getDefaultStorageLocation(),
        ),
        userPreferences.observe(
            TelemetryAcquisitionDefaults.KEY_RECORDING_ENABLED,
            TelemetryAcquisitionDefaults.DEFAULT_RECORDING_ENABLED,
        ),
        userPreferences.observe(
            TelemetryAcquisitionDefaults.KEY_MAX_RECORDED_LAPS,
            TelemetryAcquisitionDefaults.DEFAULT_MAX_RECORDED_LAPS,
        ),
        userPreferences.observe(
            TelemetryGameDefaults.KEY_GAME_SELECTION,
            TelemetryGameDefaults.DEFAULT_GAME_SELECTION,
        ),
    ) { rate, location, recordingEnabled, maxRecordedLaps, gameSelectionRaw ->
        val clampedRate = rate.coerceIn(
            TelemetrySettings.MIN_SAMPLING_RATE_HZ,
            TelemetrySettings.MAX_SAMPLING_RATE_HZ,
        )
        val clampedLaps = maxRecordedLaps.coerceIn(
            TelemetrySettings.MIN_MAX_RECORDED_LAPS,
            TelemetrySettings.MAX_MAX_RECORDED_LAPS,
        )
        TelemetrySettings(
            samplingRateHz = clampedRate,
            storageLocation = location,
            recordingEnabled = recordingEnabled,
            maxRecordedLaps = clampedLaps,
            gameSelection = GameSelection.fromPreference(gameSelectionRaw),
        )
    }

    override fun observeHudEnabled(): Flow<Boolean> = userPreferences.observe(TELEMETRY_HUD_ENABLED.bool, true)

    override fun observeAppCloseBehavior(): Flow<AppCloseBehavior> = appCloseBehaviorRepository.observeCloseBehavior()

    override fun observeRecordingNoticeShown(): Flow<Boolean> =
        userPreferences.observe(KEY_RECORDING_NOTICE_SHOWN.bool, false)

    override fun observeGameSelectionVariant(): Flow<GameId?> = userPreferences.observe(KEY_GAME_VARIANT, "")
        .map { raw -> raw.takeIf { it.isNotBlank() }?.let { toGameId(it) } }

    override suspend fun loadSettings(): TelemetrySettings {
        val location =
            userPreferences.get(TelemetryAcquisitionDefaults.KEY_STORAGE_LOCATION, getDefaultStorageLocation())
        val rate = userPreferences.get(
            TelemetryAcquisitionDefaults.KEY_SAMPLING_RATE,
            TelemetryAcquisitionDefaults.DEFAULT_SAMPLING_RATE_HZ,
        )
        val recordingEnabled = userPreferences.get(
            TelemetryAcquisitionDefaults.KEY_RECORDING_ENABLED,
            TelemetryAcquisitionDefaults.DEFAULT_RECORDING_ENABLED,
        )
        val maxRecordedLaps = userPreferences.get(
            TelemetryAcquisitionDefaults.KEY_MAX_RECORDED_LAPS,
            TelemetryAcquisitionDefaults.DEFAULT_MAX_RECORDED_LAPS,
        )
        val gameSelectionRaw = userPreferences.get(
            TelemetryGameDefaults.KEY_GAME_SELECTION,
            TelemetryGameDefaults.DEFAULT_GAME_SELECTION,
        )
        val clampedRate = rate.coerceIn(
            TelemetrySettings.MIN_SAMPLING_RATE_HZ,
            TelemetrySettings.MAX_SAMPLING_RATE_HZ,
        )
        val clampedLaps = maxRecordedLaps.coerceIn(
            TelemetrySettings.MIN_MAX_RECORDED_LAPS,
            TelemetrySettings.MAX_MAX_RECORDED_LAPS,
        )
        return TelemetrySettings(
            samplingRateHz = clampedRate,
            storageLocation = location,
            recordingEnabled = recordingEnabled,
            maxRecordedLaps = clampedLaps,
            gameSelection = GameSelection.fromPreference(gameSelectionRaw),
        )
    }

    override suspend fun updateSamplingRate(hz: Int) {
        val clamped = hz.coerceIn(
            TelemetrySettings.MIN_SAMPLING_RATE_HZ,
            TelemetrySettings.MAX_SAMPLING_RATE_HZ,
        )
        userPreferences.put(TelemetryAcquisitionDefaults.KEY_SAMPLING_RATE to clamped)
    }

    override suspend fun updateAppCloseBehavior(behavior: AppCloseBehavior) {
        appCloseBehaviorRepository.setCloseBehavior(behavior)
    }

    override suspend fun updateHudEnabled(enabled: Boolean) {
        userPreferences.put(TELEMETRY_HUD_ENABLED.bool to enabled)
    }

    override suspend fun copyFromOldDir(currentTelemetryPath: String, targetTelemetryPath: String): Boolean =
        withContext(ioDispatcher) {
            val currentTelemetryDir = File(currentTelemetryPath)
            val targetTelemetryDir = File(targetTelemetryPath)
            currentTelemetryDir.copyDirectoryWithRollback(targetTelemetryDir)
        }

    override suspend fun updateStorageLocation(path: String) {
        val target = withContext(ioDispatcher) {
            val baseDirectory = File(path)
            val telemetryDirectory = if (baseDirectory.name.equals(TELEMETRY_TARGET, ignoreCase = true)) {
                baseDirectory
            } else {
                File(baseDirectory, TELEMETRY_TARGET)
            }
            runCatching {
                if (!telemetryDirectory.exists()) telemetryDirectory.mkdirs()
                telemetryDirectory.path
            }.getOrNull() ?: path
        }
        userPreferences.put(TelemetryAcquisitionDefaults.KEY_STORAGE_LOCATION to target)
    }

    override suspend fun updateRecordingEnabled(enabled: Boolean) {
        userPreferences.put(TelemetryAcquisitionDefaults.KEY_RECORDING_ENABLED to enabled)
    }

    override suspend fun markRecordingNoticeShown() {
        userPreferences.put(KEY_RECORDING_NOTICE_SHOWN.bool to true)
    }

    override suspend fun updateMaxRecordedLaps(laps: Int) {
        val clamped = laps.coerceIn(
            TelemetrySettings.MIN_MAX_RECORDED_LAPS,
            TelemetrySettings.MAX_MAX_RECORDED_LAPS,
        )
        userPreferences.put(TelemetryAcquisitionDefaults.KEY_MAX_RECORDED_LAPS to clamped)
    }

    override suspend fun updateGameSelection(selection: GameSelection) {
        userPreferences.put(TelemetryGameDefaults.KEY_GAME_SELECTION to selection.toPreferenceValue())
    }

    override suspend fun updateGameSelectionVariant(gameId: GameId?) {
        userPreferences.put(KEY_GAME_VARIANT to (gameId?.name ?: ""))
    }

    override suspend fun getStorageSizeBytes(path: String): Long? = withContext(ioDispatcher) {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return@withContext null

        runCatching {
            dir.walkTopDown()
                .filter { it.isFile }
                .fold(0L) { acc, file -> acc + file.length() }
        }.getOrNull()
    }

    override fun getDefaultStorageLocation(): String = appDirectories.cacheDir.absolutePath

    override suspend fun validateStorageLocation(path: String): StorageValidationResult = withContext(ioDispatcher) {
        if (path.isBlank()) return@withContext StorageValidationResult.Empty

        val file = File(path)
        if (!file.isAbsolute) return@withContext StorageValidationResult.NotAbsolutePath

        return@withContext when {
            !file.exists() -> {
                val created = runCatching { file.mkdirs() }.getOrDefault(false)
                if (created) StorageValidationResult.Valid else StorageValidationResult.CannotCreate
            }

            !file.isDirectory -> StorageValidationResult.NotADirectory

            !file.canWrite() -> StorageValidationResult.NotWritable

            else -> StorageValidationResult.Valid
        }
    }

    private fun toGameId(raw: String): GameId? = GameId.fromName(raw)

    private companion object {

        const val TELEMETRY_HUD_ENABLED = "telemetry_hud_enabled"
        const val TELEMETRY_TARGET = "telemetry"
        const val KEY_RECORDING_NOTICE_SHOWN = "telemetry_recording_notice_shown"
        val KEY_GAME_VARIANT: StringPrefKey = "telemetry_settings_game_variant".str
    }
}
