package com.project.analyzer.telemetry.recording.impl.file.codec

internal object DefaultFrameStorageCodecFactory : FrameStorageCodecFactory {
    override fun create(semanticPayloadTypeHint: String?): FrameStorageSessionCodec =
        if (shouldUseXorDelta(semanticPayloadTypeHint)) {
            XorDeltaFrameStorageSessionCodec()
        } else {
            RawFrameStorageSessionCodec
        }

    private fun shouldUseXorDelta(semanticPayloadTypeHint: String?): Boolean {
        val type = semanticPayloadTypeHint?.trim().orEmpty()
        return type.startsWith("ac_shm_", ignoreCase = true)
    }
}
