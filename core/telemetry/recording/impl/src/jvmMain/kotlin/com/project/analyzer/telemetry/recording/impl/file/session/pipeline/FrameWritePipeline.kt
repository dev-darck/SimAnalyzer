package com.project.analyzer.telemetry.recording.impl.file.session.pipeline

import com.project.analyzer.telemetry.recording.api.payload.TelemetryFramePayload
import com.project.analyzer.telemetry.recording.impl.file.model.ActiveSession
import com.project.analyzer.telemetry.recording.impl.file.pipeline.FileTelemetryFrameWriteAdapter
import com.project.analyzer.telemetry.recording.impl.file.session.FileTelemetrySessionStore

internal class FrameWritePipeline(
    store: FileTelemetrySessionStore,
    private val adapters: List<FileTelemetryFrameWriteAdapter>,
) {
    private val stages: List<FrameWriteStage> = listOf(
        FrameAdmissionStage(store),
        FramePayloadPreparationStage(store),
        FrameEncodeAndPersistStage(store, adapters),
    )

    fun write(session: ActiveSession, payload: TelemetryFramePayload): Boolean {
        val context = FrameWriteContext(session = session, payload = payload)
        for (stage in stages) {
            adapters.forEach { it.onStageStart(stage.name, session, payload) }
            val shouldContinue = stage.process(context)
            if (!shouldContinue) {
                adapters.forEach { it.onStageStop(stage.name, session, payload, session.isClosed) }
                break
            }
        }
        return context.shouldEnqueueCompression
    }
}
