package com.project.analyzer.telemetry.recording.impl.controller

import com.project.analyzer.api.di.IO
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.api.contract.SessionEndReason
import com.project.analyzer.telemetry.api.contract.SessionInfo
import com.project.analyzer.telemetry.api.contract.SessionType
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionConfig
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionSettings
import com.project.analyzer.telemetry.recording.api.payload.TelemetryFramePayload
import com.project.analyzer.telemetry.recording.api.recorder.TelemetryRecorder
import com.project.analyzer.telemetry.recording.api.recording.TelemetryRecordingController
import com.project.analyzer.telemetry.recording.api.recording.TelemetryRecordingSample
import com.project.analyzer.telemetry.recording.api.recording.TelemetryRecordingSource
import com.project.analyzer.telemetry.recording.api.session.TelemetrySessionDescriptor
import com.project.analyzer.telemetry.recording.api.session.TelemetrySessionUpdate
import com.project.analyzer.telemetry.recording.impl.controller.TelemetryRecordingInput.TelemetryRecordingConfigInput
import com.project.analyzer.telemetry.recording.impl.controller.TelemetryRecordingInput.TelemetryRecordingEventInput
import com.project.analyzer.telemetry.recording.impl.controller.TelemetryRecordingInput.TelemetryRecordingSampleInput
import com.project.analyzer.telemetry.recording.impl.file.META_FILE_NAME
import com.project.analyzer.telemetry.recording.impl.file.model.SessionMetadata
import com.project.analyzer.telemetry.recording.impl.index.TelemetryFrameIndexBuilder
import com.project.analyzer.utils.logger
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.abs

@Inject
@SingleIn(SessionScope::class)
class TelemetryRecordingControllerImpl(
    private val telemetry: TelemetryLifecycle,
    private val sources: Set<@JvmSuppressWildcards TelemetryRecordingSource>,
    private val recorder: TelemetryRecorder,
    private val frameIndexBuilder: TelemetryFrameIndexBuilder,
    private val settings: TelemetryAcquisitionSettings,
    private val json: Json,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : TelemetryRecordingController {
    private val scope = CoroutineScope(
        SupervisorJob() + ioDispatcher + CoroutineExceptionHandler { _, e ->
            logger.error(e) { "[recording] recording controller failed" }
        },
    )

    private var job: Job? = null
    private var startupCleanupJob: Job? = null
    private var state: ControllerState = ControllerState()

    override suspend fun start() {
        if (job?.isActive == true) return
        startupCleanupJob?.cancelAndJoin()

        job = scope.launch {
            inputFlow().collect(::handleInput)
        }
        startupCleanupJob = scope.launch {
            cleanupUnsavedSessions(reason = "app_start")
        }
    }

    override suspend fun stop() {
        startupCleanupJob?.cancelAndJoin()
        startupCleanupJob = null
        job?.cancelAndJoin()
        job = null

        val activeGameId = state.currentGameId
        val activeSessionId = state.startedSessionId
        if (activeGameId != null && activeSessionId != null) {
            recorder.endSession(activeGameId, activeSessionId, "shutdown")
        }

        recorder.close()
        sources.forEach { it.close() }
        state = ControllerState()

        cleanupUnsavedSessions(reason = "app_stop")
        scope.cancel()
    }

    private fun inputFlow(): Flow<TelemetryRecordingInput> {
        val inputs = mutableListOf<Flow<TelemetryRecordingInput>>()
        inputs += settings.observeConfig().map { TelemetryRecordingConfigInput(it) }
        inputs += telemetry.events.map { TelemetryRecordingEventInput(it) }
        sources.forEach { source ->
            inputs += source.samples.map { TelemetryRecordingSampleInput(it) }
        }
        return inputs.merge()
    }

    private suspend fun handleInput(input: TelemetryRecordingInput) {
        when (input) {
            is TelemetryRecordingConfigInput -> handleConfig(input.config)
            is TelemetryRecordingEventInput -> handleEvent(input.event)
            is TelemetryRecordingSampleInput -> handleSample(input.sample)
        }
    }

    private suspend fun handleConfig(config: TelemetryAcquisitionConfig) {
        state = state.copy(
            recordingEnabled = config.recordingEnabled,
            maxRecordedLaps = config.maxRecordedLaps,
        )

        if (!config.recordingEnabled) {
            val activeGameId = state.currentGameId
            val activeSessionId = state.startedSessionId
            if (activeGameId != null && activeSessionId != null) {
                recorder.endSession(activeGameId, activeSessionId, "disabled")
            }
            state = state.clearSessionState()
        }
    }

    private suspend fun handleEvent(event: TelemetryLifecycleEvent) {
        when (event) {
            is TelemetryLifecycleEvent.SessionStarted -> {
                state = state.copy(
                    sessionInfo = event.session,
                    startedSessionId = null,
                    currentGameId = null,
                    currentDataSource = null,
                    baseCompletedLaps = null,
                    blockedSessionId = state.blockedSessionId.takeIf { it == event.session.sessionId },
                )
            }

            is TelemetryLifecycleEvent.SessionUpdated -> {
                val updated = event.session
                if (state.sessionInfo?.sessionId == updated.sessionId) {
                    state = state.copy(sessionInfo = updated)
                }
                if (state.startedSessionId == updated.sessionId) {
                    val gameId = state.currentGameId ?: return
                    recorder.updateSession(
                        TelemetrySessionUpdate(
                            sessionId = updated.sessionId,
                            gameId = gameId,
                            sessionType = updated.sessionType.asSessionTypeString(),
                            carModel = updated.carModel.ifBlank { null },
                            trackId = updated.trackId.ifBlank { null },
                            airTempC = state.lastAirTempC,
                            trackTempC = state.lastTrackTempC,
                            dataSource = state.currentDataSource,
                        ),
                    )
                }
            }

            is TelemetryLifecycleEvent.SessionPaused -> {
                val gameId = state.currentGameId
                if (state.startedSessionId == event.sessionId && gameId != null) {
                    recorder.pauseSession(gameId = gameId, sessionId = event.sessionId, reason = event.reason.name)
                }
            }

            is TelemetryLifecycleEvent.SessionResumed -> {
                val gameId = state.currentGameId
                if (state.startedSessionId == event.sessionId && gameId != null) {
                    recorder.resumeSession(gameId = gameId, sessionId = event.sessionId)
                }
            }

            is TelemetryLifecycleEvent.SessionEnded -> {
                val gameId = state.currentGameId
                if (state.startedSessionId == event.sessionId && gameId != null) {
                    recorder.endSession(gameId = gameId, sessionId = event.sessionId, reason = event.reason.name)
                }
                state = state.clearSessionState()
            }

            is TelemetryLifecycleEvent.SimDisconnected -> {
                val gameId = state.currentGameId
                val sessionId = state.startedSessionId
                if (gameId != null && sessionId != null) {
                    recorder.endSession(
                        gameId = gameId,
                        sessionId = sessionId,
                        reason = SessionEndReason.SIM_DISCONNECTED.name,
                    )
                }
                state = state.clearSessionState()
            }

            else -> Unit
        }
    }

    private suspend fun handleSample(sample: TelemetryRecordingSample) {
        val snapshot = state
        if (!snapshot.recordingEnabled) return

        val sessionInfo = snapshot.sessionInfo ?: return
        if (sample.sessionId != sessionInfo.sessionId) return
        if (snapshot.blockedSessionId == sessionInfo.sessionId) return

        if (snapshot.startedSessionId == null) {
            startSession(sessionInfo, sample)
        } else if (snapshot.currentDataSource != sample.dataSource) {
            state = state.copy(currentDataSource = sample.dataSource)
            recorder.updateSession(
                TelemetrySessionUpdate(
                    sessionId = sessionInfo.sessionId,
                    gameId = snapshot.currentGameId ?: sample.gameId,
                    airTempC = state.lastAirTempC,
                    trackTempC = state.lastTrackTempC,
                    dataSource = sample.dataSource,
                ),
            )
        }

        if (state.startedSessionId != sample.sessionId) return

        maybeUpdateTemperatures(sample)

        val index = frameIndexBuilder.build(sample.frame)
        recorder.recordFrame(
            TelemetryFramePayload(
                sessionId = sample.sessionId,
                gameId = sample.gameId,
                timestampNs = sample.timestampNs,
                frameId = sample.frameId,
                payloadType = sample.payloadType,
                dataSourceId = sample.dataSourceId,
                payload = sample.payload,
                index = index,
            ),
        )

        if (shouldStopOnLapLimit(sample.frame)) {
            recorder.endSession(sample.gameId, sample.sessionId, "lap_limit")
            state = state.copy(
                startedSessionId = null,
                currentGameId = null,
                currentDataSource = null,
                baseCompletedLaps = null,
                blockedSessionId = sample.sessionId,
            )
        }
    }

    private suspend fun startSession(session: SessionInfo, sample: TelemetryRecordingSample) {
        val baseCompletedLaps = completedLaps(sample.frame)
        val airTempC = normalizeTemperature(sample.frame.environment?.airTempC)
        val trackTempC = normalizeTemperature(sample.frame.environment?.roadTempC)
        recorder.startSession(
            TelemetrySessionDescriptor(
                sessionId = session.sessionId,
                gameId = sample.gameId,
                sessionType = session.sessionType.asSessionTypeString(),
                carModel = session.carModel.ifBlank { null },
                trackId = session.trackId.ifBlank { null },
                airTempC = airTempC,
                trackTempC = trackTempC,
                startedAtMs = System.currentTimeMillis(),
                dataSource = sample.dataSource,
                payloadType = sample.payloadType,
                payloadSize = sample.payload.size,
            ),
        )

        state = state.copy(
            startedSessionId = session.sessionId,
            currentGameId = sample.gameId,
            currentDataSource = sample.dataSource,
            baseCompletedLaps = baseCompletedLaps,
            lastAirTempC = airTempC,
            lastTrackTempC = trackTempC,
        )
    }

    private suspend fun maybeUpdateTemperatures(sample: TelemetryRecordingSample) {
        val sessionId = state.startedSessionId ?: return
        if (sessionId != sample.sessionId) return

        val airTempC = normalizeTemperature(sample.frame.environment?.airTempC)
        val trackTempC = normalizeTemperature(sample.frame.environment?.roadTempC)

        val prevAir = state.lastAirTempC
        val prevTrack = state.lastTrackTempC
        val airChanged = hasMeaningfulTempChange(prevAir, airTempC)
        val trackChanged = hasMeaningfulTempChange(prevTrack, trackTempC)
        if (!airChanged && !trackChanged) return

        val gameId = state.currentGameId ?: sample.gameId
        recorder.updateSession(
            TelemetrySessionUpdate(
                sessionId = sessionId,
                gameId = gameId,
                airTempC = airTempC,
                trackTempC = trackTempC,
            ),
        )

        state = state.copy(
            lastAirTempC = airTempC ?: prevAir,
            lastTrackTempC = trackTempC ?: prevTrack,
        )
    }

    private fun shouldStopOnLapLimit(frame: com.project.analyzer.telemetry.api.model.TelemetryFrame): Boolean {
        val limit = state.maxRecordedLaps
        if (limit <= 0) return false

        val currentCompleted = completedLaps(frame) ?: return false
        val baseline = state.baseCompletedLaps ?: currentCompleted.also {
            state = state.copy(baseCompletedLaps = it)
        }
        return currentCompleted - baseline >= limit
    }

    private fun completedLaps(frame: com.project.analyzer.telemetry.api.model.TelemetryFrame): Int? =
        frame.session?.completedLaps ?: frame.lap?.completedLaps

    private fun SessionType.asSessionTypeString(): String? = if (this == SessionType.UNKNOWN) null else name

    private fun normalizeTemperature(value: Float?): Float? {
        if (value == null || !value.isFinite()) return null
        return value.coerceIn(MIN_REASONABLE_TEMP_C, MAX_REASONABLE_TEMP_C)
    }

    private fun hasMeaningfulTempChange(previous: Float?, current: Float?): Boolean {
        if (current == null) return false
        if (previous == null) return true
        return abs(previous - current) >= TEMP_UPDATE_EPSILON_C
    }

    private suspend fun cleanupUnsavedSessions(reason: String) {
        withContext(ioDispatcher) {
            val root = resolveStorageRoot() ?: return@withContext
            val dirs = root.listFiles()?.filter { it.isDirectory }.orEmpty()
            var removed = 0

            dirs.forEach { dir ->
                val metaFile = File(dir, META_FILE_NAME)
                if (!metaFile.exists()) return@forEach

                val metadata = runCatching {
                    json.decodeFromString(SessionMetadata.serializer(), metaFile.readText())
                }.getOrNull() ?: return@forEach

                if (metadata.isSaved) return@forEach
                if (dir.deleteRecursively()) {
                    removed += 1
                }
            }

            if (removed > 0) {
                logger.info { "[recording] removed $removed temp sessions ($reason)" }
            }
        }
    }

    private suspend fun resolveStorageRoot(): File? {
        val path = runCatching { settings.currentConfig().storageLocation }
            .getOrNull()
            .orEmpty()
            .trim()
        if (path.isBlank()) return null

        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return null
        if (!dir.canWrite()) return null
        return dir
    }

    private data class ControllerState(
        val sessionInfo: SessionInfo? = null,
        val startedSessionId: Long? = null,
        val currentGameId: String? = null,
        val currentDataSource: String? = null,
        val lastAirTempC: Float? = null,
        val lastTrackTempC: Float? = null,
        val baseCompletedLaps: Int? = null,
        val blockedSessionId: Long? = null,
        val recordingEnabled: Boolean = true,
        val maxRecordedLaps: Int = 0,
    ) {
        fun clearSessionState(): ControllerState = copy(
            sessionInfo = null,
            startedSessionId = null,
            currentGameId = null,
            currentDataSource = null,
            lastAirTempC = null,
            lastTrackTempC = null,
            baseCompletedLaps = null,
            blockedSessionId = null,
        )
    }

    private companion object {
        const val TEMP_UPDATE_EPSILON_C = 0.25f
        const val MIN_REASONABLE_TEMP_C = -80f
        const val MAX_REASONABLE_TEMP_C = 120f
    }
}
