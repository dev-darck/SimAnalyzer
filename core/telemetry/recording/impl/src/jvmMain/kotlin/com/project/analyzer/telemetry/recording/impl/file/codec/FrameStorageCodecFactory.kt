package com.project.analyzer.telemetry.recording.impl.file.codec

internal interface FrameStorageCodecFactory {
    fun create(semanticPayloadTypeHint: String? = null): FrameStorageSessionCodec
}
