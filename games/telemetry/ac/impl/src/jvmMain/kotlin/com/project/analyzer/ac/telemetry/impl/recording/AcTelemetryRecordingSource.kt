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
import com.project.analyzer.telemetry.recording.api.recording.TelemetryRecordingSampleBuffer
import com.project.analyzer.telemetry.recording.api.recording.TelemetryRecordingSource
import com.project.analyzer.telemetry.recording.api.recording.TelemetrySamplingGate
import com.project.analyzer.utils.logger.RATE_LIMITED
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

@Inject
@SingleIn(SessionScope::class)
class AcTelemetryRecordingSource(
    private val settings: TelemetryAcquisitionSettings,
    private val encoder: AcRawFrameEncoder,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : TelemetryRecordingSource,
    AcTelemetryRecordingEmitter {

    private val settingsDispatcher: CoroutineDispatcher =
        ioDispatcher.limitedParallelism(1, "AcTelemetryRecordingSource")
    private val emitDispatcher: CoroutineDispatcher =
        ioDispatcher.limitedParallelism(1, "AcTelemetryRecordingEmitter")
    private val settingsScope = CoroutineScope(SupervisorJob() + settingsDispatcher)
    private val emitScope = CoroutineScope(SupervisorJob() + emitDispatcher)
    private val gate = TelemetrySamplingGate(TelemetryAcquisitionDefaults.DEFAULT_SAMPLING_RATE_HZ)
    private var lastSamplingRateHz: Int = -1

    @Volatile
    private var recordingEnabled: Boolean = true

    private val sampleBuffer = TelemetryRecordingSampleBuffer(
        scope = emitScope,
        bufferCapacity = SAMPLE_BUFFER_CAPACITY,
        onQueueDrop = { sample ->
            logger.atWarn(RATE_LIMITED) {
                message = "[recording] dropped AC encoded sample queue due to backpressure " +
                    "(sessionId=${sample.sessionId})"
            }
        },
        onSinkDrop = { sample ->
            logger.atWarn(RATE_LIMITED) {
                message = "[recording] dropped AC encoded sample due to sink backpressure " +
                    "(sessionId=${sample.sessionId})"
            }
        },
    )
    override val samples: SharedFlow<TelemetryRecordingSample> = sampleBuffer.samples
    private val encodedCount = AtomicLong(0)
    private val encodeTotalNs = AtomicLong(0)
    private val encodeMaxNs = AtomicLong(0)
    private val lastStatsLogNs = AtomicLong(0)

    init {
        settingsScope.launch {
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
        if (!sampleBuffer.hasSubscribers()) return
        if (!gate.shouldSample(snapshot.timestampNs)) return

        val encodeStartedNs = System.nanoTime()
        val payload = encoder.encode(snapshot)
        val encodeElapsedNs = (System.nanoTime() - encodeStartedNs).coerceAtLeast(0L)
        encodedCount.incrementAndGet()
        encodeTotalNs.addAndGet(encodeElapsedNs)
        encodeMaxNs.accumulateAndGet(encodeElapsedNs, ::max)
        val sample = TelemetryRecordingSample(
            sessionId = sessionId,
            timestampNs = snapshot.timestampNs,
            frameId = snapshot.frameId,
            gameId = gameIdForSource(dataSource),
            dataSourceId = dataSourceId(dataSource),
            dataSource = dataSource.name,
            payloadType = encoder.payloadType,
            payload = payload,
            frame = frame,
        )
        sampleBuffer.tryOffer(sample)
        maybeLogEmitterStats()
    }

    override fun close() {
        sampleBuffer.close()
        settingsScope.cancel()
        emitScope.cancel()

        LeakCanaryRuntime.watch(this, "AcTelemetryRecordingSource")
    }

    private fun dataSourceId(source: DataSourceType): Int = if (source == DataSourceType.FALLBACK) 1 else 0

    private fun gameIdForSource(source: DataSourceType): String = if (source == DataSourceType.FALLBACK) "ace" else "ac"

    private fun maybeLogEmitterStats() {
        val now = System.nanoTime()
        val last = lastStatsLogNs.get()
        if (last != 0L && now - last < EMITTER_STATS_LOG_EVERY_NS) return
        if (!lastStatsLogNs.compareAndSet(last, now)) return

        val encoded = encodedCount.get()
        val queued = sampleBuffer.queuedCount
        val queueDropped = sampleBuffer.queueDropCount
        val sinkEmitted = sampleBuffer.sinkEmitCount
        val sinkDropped = sampleBuffer.sinkDropCount
        val backlogApprox = sampleBuffer.relayBacklogApproxCount
        if (encoded == 0L && queued == 0L && queueDropped == 0L && sinkDropped == 0L) return

        val avgEncodeUs = if (encoded > 0L) (encodeTotalNs.get() / encoded) / 1_000.0 else 0.0
        val maxEncodeUs = encodeMaxNs.get() / 1_000.0
        logger.atDebug {
            message = "[recording] AC emitter stats encodedTotal=$encoded queuedTotal=$queued " +
                "queueDropTotal=$queueDropped sinkEmitTotal=$sinkEmitted sinkDropTotal=$sinkDropped " +
                "backlogApprox=$backlogApprox " +
                "encodeUs(avg=${"%.1f".format(avgEncodeUs)}, max=${"%.1f".format(maxEncodeUs)})"
        }
    }

    private companion object {

        const val SAMPLE_BUFFER_CAPACITY = 256
        const val EMITTER_STATS_LOG_EVERY_NS: Long = 15_000_000_000L
    }
}
