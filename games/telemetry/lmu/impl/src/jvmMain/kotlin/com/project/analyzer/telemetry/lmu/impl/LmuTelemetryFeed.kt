package com.project.analyzer.telemetry.lmu.impl

import com.project.analyzer.telemetry.lmu.api.model.LmuTelemetrySnapshot

internal interface LmuTelemetryFeed : AutoCloseable {

    suspend fun collectFrames(onSnapshot: suspend (LmuTelemetrySnapshot) -> Unit)
    override fun close()
}
