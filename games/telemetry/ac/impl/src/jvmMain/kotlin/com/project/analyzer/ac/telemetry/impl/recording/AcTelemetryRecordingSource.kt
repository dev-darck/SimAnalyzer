package com.project.analyzer.ac.telemetry.impl.recording

import com.project.analyzer.ac.telemetry.impl.internal.AcRawSnapshot
import com.project.analyzer.ac.telemetry.impl.internal.DataSourceType
import com.project.analyzer.ac.telemetry.impl.internal.recording.AcRawFrameEncoder
import com.project.analyzer.api.di.IO
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.leak.api.LeakCanaryRuntime
import com.project.analyzer.telemetry.api.model.TelemetryFrame
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
class AcTelemetryRecordingSource(
    private val settings: TelemetryAcquisitionSettings,
    private val encoder: AcRawFrameEncoder,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : TelemetryRecordingSource, AcTelemetryRecordingEmitter {

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
                    logger.info { "[recording] samplingRateHz=$rate" }
                }
            }
        }
    }

    override suspend fun emitSample(
        sessionId: Long,
        snapshot: AcRawSnapshot,
        frame: TelemetryFrame,
        dataSource: DataSourceType,
    ) {
        if (sessionId <= 0L) return
        if (!recordingEnabled) return
        if (_samples.subscriptionCount.value == 0) return
        if (!gate.shouldSample(snapshot.timestampNs)) return

        val payload = encoder.encode(snapshot)
        _samples.emit(
            TelemetryRecordingSample(
                sessionId = sessionId,
                timestampNs = snapshot.timestampNs,
                frameId = snapshot.frameId,
                gameId = gameIdForSource(dataSource),
                dataSourceId = dataSourceId(dataSource),
                dataSource = dataSource.name,
                payloadType = encoder.payloadType,
                payload = payload,
                frame = frame,
            ),
        )
    }

    override fun close() {
        scope.cancel()

        LeakCanaryRuntime.watch(this, "AcTelemetryRecordingSource")
    }

    private fun dataSourceId(source: DataSourceType): Int = if (source == DataSourceType.FALLBACK) 1 else 0

    private fun gameIdForSource(source: DataSourceType): String = if (source == DataSourceType.FALLBACK) "ace" else "ac"

    private companion object {

        const val SAMPLE_BUFFER_CAPACITY = 256
    }
}
