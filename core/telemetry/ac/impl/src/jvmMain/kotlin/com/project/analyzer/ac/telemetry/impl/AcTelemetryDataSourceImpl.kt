package com.project.analyzer.ac.telemetry.impl

import com.project.analyzer.ac.telemetry.impl.internal.AcPollLoop
import com.project.analyzer.ac.telemetry.impl.internal.mapper.AcMapper
import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemory
import com.project.analyzer.api.di.IO
import com.project.analyzer.telemetry.ac.api.TelemetryDataSource
import com.project.analyzer.telemetry.ac.api.model.TelemetryFrame
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Inject
class AcTelemetryDataSourceImpl(
    private val pollLoop: AcPollLoop,
    private val mapper: AcMapper,
    private val shm: AcSharedMemory,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : TelemetryDataSource {

    override fun frames(): Flow<TelemetryFrame> = callbackFlow {
        val job = launch(context = ioDispatcher, start = CoroutineStart.UNDISPATCHED) {
            pollLoop.run { snapshot ->
                trySend(mapper.map(snapshot))
            }
        }

        awaitClose {
            job.cancel()
        }
    }
        .buffer(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        .catch { e -> println("Telemetry error: $e") }
        .flowOn(ioDispatcher)

    override suspend fun close() {
        withContext(ioDispatcher) {
            shm.close()
            pollLoop.close()
        }
    }
}
