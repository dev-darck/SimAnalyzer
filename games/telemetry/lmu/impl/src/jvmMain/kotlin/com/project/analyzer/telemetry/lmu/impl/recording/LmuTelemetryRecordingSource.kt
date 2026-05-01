package com.project.analyzer.telemetry.lmu.impl.recording

import com.project.analyzer.api.di.IO
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.leak.api.LeakCanaryRuntime
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.telemetry.lmu.api.model.LmuTelemetrySnapshot
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionDefaults
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionSettings
import com.project.analyzer.telemetry.recording.api.recording.TelemetryRecordingFrameSnapshot
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
internal class LmuTelemetryRecordingSource(
    private val settings: TelemetryAcquisitionSettings,
    private val encoder: LmuRawFrameEncoder,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : TelemetryRecordingSource,
    LmuTelemetryRecordingEmitter {

    private val settingsDispatcher: CoroutineDispatcher =
        ioDispatcher.limitedParallelism(1, "LmuTelemetryRecordingSource")
    private val settingsScope = CoroutineScope(SupervisorJob() + settingsDispatcher)
    private val gate = TelemetrySamplingGate(TelemetryAcquisitionDefaults.DEFAULT_SAMPLING_RATE_HZ)
    private var lastSamplingRateHz: Int = -1

    @Volatile
    private var recordingEnabled: Boolean = true

    private val sampleBuffer = TelemetryRecordingSampleBuffer(
        bufferCapacity = SAMPLE_BUFFER_CAPACITY,
        onDrop = { sample ->
            logger.atWarn(RATE_LIMITED) {
                message = "[recording] dropped LMU encoded sample due to recording buffer backpressure " +
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
                    logger.info { "[recording] lmu samplingRateHz=$rate" }
                }
            }
        }
    }

    override suspend fun emitSample(sessionId: Long, snapshot: LmuTelemetrySnapshot, frame: TelemetryFrame) {
        if (sessionId <= 0L) return
        if (!recordingEnabled) return
        if (!sampleBuffer.hasSubscribers()) return
        if (!gate.shouldSample(snapshot.timestampNs)) return

        val encodeStartedNs = System.nanoTime()
        val payload = encoder.encode(snapshot.telemetryVersion, snapshot.scoringVersion) ?: return
        val encodeElapsedNs = (System.nanoTime() - encodeStartedNs).coerceAtLeast(0L)
        encodedCount.incrementAndGet()
        encodeTotalNs.addAndGet(encodeElapsedNs)
        encodeMaxNs.accumulateAndGet(encodeElapsedNs, ::max)
        val sample = TelemetryRecordingSample(
            sessionId = sessionId,
            timestampNs = snapshot.timestampNs,
            frameId = snapshot.frameId,
            gameId = GAME_ID,
            dataSourceId = DATA_SOURCE_ID,
            dataSource = DATA_SOURCE,
            payloadType = encoder.payloadType,
            payload = payload,
            frame = TelemetryRecordingFrameSnapshot.from(frame),
        )
        sampleBuffer.tryOffer(sample)
        maybeLogEmitterStats()
    }

    override fun close() {
        sampleBuffer.close()
        settingsScope.cancel()

        LeakCanaryRuntime.watch(this, "LmuTelemetryRecordingSource")
    }

    private fun maybeLogEmitterStats() {
        val now = System.nanoTime()
        val last = lastStatsLogNs.get()
        if (last != 0L && now - last < EMITTER_STATS_LOG_EVERY_NS) return
        if (!lastStatsLogNs.compareAndSet(last, now)) return

        val encoded = encodedCount.get()
        val accepted = sampleBuffer.acceptedCount
        val dropped = sampleBuffer.dropCount
        if (encoded == 0L && accepted == 0L && dropped == 0L) return

        val avgEncodeUs = if (encoded > 0L) (encodeTotalNs.get() / encoded) / 1_000.0 else 0.0
        val maxEncodeUs = encodeMaxNs.get() / 1_000.0
        logger.atDebug {
            message = "[recording] LMU emitter stats encodedTotal=$encoded acceptedTotal=$accepted " +
                "dropTotal=$dropped " +
                "encodeUs(avg=${"%.1f".format(avgEncodeUs)}, max=${"%.1f".format(maxEncodeUs)})"
        }
    }

    private companion object {

        const val GAME_ID = "lmu"
        const val DATA_SOURCE_ID = 0
        const val DATA_SOURCE = "shm"
        const val SAMPLE_BUFFER_CAPACITY = 256
        const val EMITTER_STATS_LOG_EVERY_NS: Long = 15_000_000_000L
    }
}
