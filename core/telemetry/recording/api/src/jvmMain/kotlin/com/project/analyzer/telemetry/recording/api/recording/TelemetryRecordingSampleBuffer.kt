package com.project.analyzer.telemetry.recording.api.recording

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicLong

public class TelemetryRecordingSampleBuffer(
    bufferCapacity: Int,
    private val onDrop: ((TelemetryRecordingSample) -> Unit)? = null,
) {

    private val _samples = MutableSharedFlow<TelemetryRecordingSample>(
        replay = 0,
        extraBufferCapacity = bufferCapacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    public val samples: SharedFlow<TelemetryRecordingSample> = _samples.asSharedFlow()

    private val _acceptedCount = AtomicLong(0)
    private val _dropCount = AtomicLong(0)

    public fun hasSubscribers(): Boolean = _samples.subscriptionCount.value > 0

    public fun tryOffer(sample: TelemetryRecordingSample): Boolean {
        val accepted = _samples.tryEmit(sample)
        if (accepted) {
            _acceptedCount.incrementAndGet()
        } else {
            _dropCount.incrementAndGet()
            onDrop?.invoke(sample)
        }
        return accepted
    }

    public fun close(): Unit = Unit

    public val acceptedCount: Long
        get() = _acceptedCount.get()

    public val dropCount: Long
        get() = _dropCount.get()

    /**
     * Compatibility counters kept for existing diagnostics.
     * The buffer now emits directly into a single shared flow, so queue/relay metrics collapse
     * into accepted/drop totals.
     */
    public val queuedCount: Long
        get() = _acceptedCount.get()

    public val sinkEmitCount: Long
        get() = _acceptedCount.get()

    public val sinkDropCount: Long
        get() = _dropCount.get()

    public val queueDropCount: Long
        get() = 0L

    public val relayBacklogApproxCount: Long
        get() = 0L
}
