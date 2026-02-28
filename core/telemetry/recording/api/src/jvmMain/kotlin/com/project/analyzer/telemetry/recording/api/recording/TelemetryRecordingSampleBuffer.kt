package com.project.analyzer.telemetry.recording.api.recording

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

public class TelemetryRecordingSampleBuffer(
    scope: CoroutineScope,
    bufferCapacity: Int,
    private val onQueueDrop: ((TelemetryRecordingSample) -> Unit)? = null,
    private val onSinkDrop: ((TelemetryRecordingSample) -> Unit)? = null,
) {

    private val _samples = MutableSharedFlow<TelemetryRecordingSample>(
        replay = 0,
        extraBufferCapacity = bufferCapacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    public val samples: SharedFlow<TelemetryRecordingSample> = _samples.asSharedFlow()

    private val pendingSamples = Channel<TelemetryRecordingSample>(
        capacity = bufferCapacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val _queuedCount = AtomicLong(0)
    private val _queueDropCount = AtomicLong(0)
    private val _sinkEmitCount = AtomicLong(0)
    private val _sinkDropCount = AtomicLong(0)

    init {
        scope.launch {
            for (sample in pendingSamples) {
                if (_samples.tryEmit(sample)) {
                    _sinkEmitCount.incrementAndGet()
                } else {
                    _sinkDropCount.incrementAndGet()
                    onSinkDrop?.invoke(sample)
                }
            }
        }
    }

    public fun hasSubscribers(): Boolean = _samples.subscriptionCount.value > 0

    public fun tryOffer(sample: TelemetryRecordingSample): Boolean {
        val queued = pendingSamples.trySend(sample).isSuccess
        if (queued) {
            _queuedCount.incrementAndGet()
        } else {
            _queueDropCount.incrementAndGet()
            onQueueDrop?.invoke(sample)
        }
        return queued
    }

    public fun close() {
        pendingSamples.close()
    }

    public val queuedCount: Long
        get() = _queuedCount.get()

    public val queueDropCount: Long
        get() = _queueDropCount.get()

    public val sinkEmitCount: Long
        get() = _sinkEmitCount.get()

    public val sinkDropCount: Long
        get() = _sinkDropCount.get()

    /**
     * Approximate number of samples still pending relay from the internal channel to the shared flow.
     * This is a derived metric from counters and may be off by 1 momentarily due to concurrent reads.
     */
    public val relayBacklogApproxCount: Long
        get() = (_queuedCount.get() - _sinkEmitCount.get() - _sinkDropCount.get()).coerceAtLeast(0L)
}
