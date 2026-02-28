package com.project.analyzer.telemetry.recording.impl.file.session.pipeline

internal interface FrameWriteStage {
    val name: String
    fun process(context: FrameWriteContext): Boolean
}
