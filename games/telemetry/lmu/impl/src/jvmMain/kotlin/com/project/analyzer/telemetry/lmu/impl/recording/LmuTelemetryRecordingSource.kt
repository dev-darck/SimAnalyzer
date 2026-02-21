package com.project.analyzer.telemetry.lmu.impl.recording

import com.project.analyzer.api.di.IO
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.telemetry.lmu.api.model.LmuTelemetrySnapshot
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionDefaults
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionSettings
import com.project.analyzer.telemetry.recording.api.recording.TelemetryRecordingSample
import com.project.analyzer.telemetry.recording.api.recording.TelemetryRecordingSource
import com.project.analyzer.telemetry.recording.api.recording.TelemetrySamplingGate
import com.project.analyzer.utils.logger
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@Inject
@SingleIn(SessionScope::class)
internal class LmuTelemetryRecordingSource(
    private val settings: TelemetryAcquisitionSettings,
    private val encoder: LmuRawFrameEncoder,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : TelemetryRecordingSource,
    LmuTelemetryRecordingEmitter {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val gate = TelemetrySamplingGate(TelemetryAcquisitionDefaults.DEFAULT_SAMPLING_RATE_HZ)
    private var lastSamplingRateHz: Int = -1

    @Volatile
    private var recordingEnabled: Boolean = true

    private val _samples = MutableSharedFlow<TelemetryRecordingSample>(
        replay = 0,
        extraBufferCapacity = SAMPLE_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.SUSPEND,
    )
    override val samples: SharedFlow<TelemetryRecordingSample> = _samples.asSharedFlow()

    init {
        scope.launch {
            settings.observeConfig().collect { config ->
                val rate = config.samplingRateHz.coerceIn(
                    TelemetryAcquisitionDefaults.MIN_SAMPLING_RATE_HZ,
                    TelemetryAcquisitionDefaults.MAX_SAMPLING_RATE_HZ,
                )
                recordingEnabled = config.recordingEnabled
                if (rate != lastSamplingRateHz) {
                    lastSamplingRateHz = rate
                    gate.updateRate(rate)
                    logger.info { "[recording] lmu samplingRateHz=$rate" }
                }
            }
        }
    }

    override suspend fun emitSample(sessionId: Long, snapshot: LmuTelemetrySnapshot, frame: TelemetryFrame) {
        if (sessionId <= 0L) return
        if (!recordingEnabled) return
        if (_samples.subscriptionCount.value == 0) return
        if (!gate.shouldSample(snapshot.timestampNs)) return

        val payload = encoder.encode(snapshot.telemetryVersion, snapshot.scoringVersion) ?: return
        _samples.emit(
            TelemetryRecordingSample(
                sessionId = sessionId,
                timestampNs = snapshot.timestampNs,
                frameId = snapshot.frameId,
                gameId = GAME_ID,
                dataSourceId = DATA_SOURCE_ID,
                dataSource = DATA_SOURCE,
                payloadType = encoder.payloadType,
                payload = payload,
                frame = frame,
            ),
        )
    }

    override fun close() {
        scope.cancel()
    }

    private companion object {

        const val GAME_ID = "lmu"
        const val DATA_SOURCE_ID = 0
        const val DATA_SOURCE = "shm"
        const val SAMPLE_BUFFER_CAPACITY = 256
    }
}
