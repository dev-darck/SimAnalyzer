package com.project.analyzer.ac.telemetry.impl

import com.project.analyzer.ac.telemetry.impl.internal.AcPollLoop
import com.project.analyzer.ac.telemetry.impl.internal.mapper.AcMapper
import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemory
import com.project.analyzer.api.di.IO
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.ac.api.TelemetryDataSource
import com.project.analyzer.telemetry.ac.api.model.TelemetryFrame
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@Inject
@SingleIn(SessionScope::class)
@ContributesBinding(SessionScope::class, binding = binding<TelemetryDataSource>())
class AcTelemetryDataSourceImpl(
    private val pollLoop: AcPollLoop,
    private val mapper: AcMapper,
    private val shm: AcSharedMemory,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : TelemetryDataSource {

    override fun frames(): Flow<TelemetryFrame> = callbackFlow {
        pollLoop.run { snapshot ->
            trySend(mapper.map(snapshot))
        }

        awaitClose {
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
