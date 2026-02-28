package com.project.analyzer.telemetry.recording.impl.file.codec

internal class XorDeltaFrameStorageSessionCodec(private val keyFrameInterval: Int = DEFAULT_KEYFRAME_INTERVAL) :
    FrameStorageSessionCodec {

    override val codecId: String = "xor_delta_v1"

    private var previousSemanticPayload: ByteArray? = null
    private var framesSinceKeyFrame: Int = keyFrameInterval
    private var lastSemanticPayloadType: String? = null

    override fun describeStorage(semanticPayloadType: String, semanticPayloadSize: Int): StoragePayloadDescriptor =
        StoragePayloadDescriptor(
            storagePayloadType = "$semanticPayloadType#xor1",
            storagePayloadSize = semanticPayloadSize + 1,
        )

    override fun encode(semanticPayloadType: String, semanticPayload: ByteArray): EncodedFramePayload {
        val descriptor = describeStorage(
            semanticPayloadType = semanticPayloadType,
            semanticPayloadSize = semanticPayload.size,
        )
        val previous = previousSemanticPayload
        val mustWriteKeyFrame = previous == null ||
            previous.size != semanticPayload.size ||
            lastSemanticPayloadType != semanticPayloadType ||
            framesSinceKeyFrame >= keyFrameInterval

        val encoded = ByteArray(semanticPayload.size + 1)
        if (mustWriteKeyFrame) {
            encoded[0] = FRAME_KIND_KEY
            semanticPayload.copyInto(encoded, destinationOffset = 1)
            framesSinceKeyFrame = 0
        } else {
            encoded[0] = FRAME_KIND_XOR_DELTA
            var i = 0
            while (i < semanticPayload.size) {
                encoded[i + 1] = (semanticPayload[i].toInt() xor previous[i].toInt()).toByte()
                i += 1
            }
            framesSinceKeyFrame += 1
        }

        previousSemanticPayload = semanticPayload.copyOf()
        lastSemanticPayloadType = semanticPayloadType

        return EncodedFramePayload(
            storagePayloadType = descriptor.storagePayloadType,
            storagePayload = encoded,
        )
    }

    private companion object {
        // Larger interval improves compression ratio for long sessions while staying lossless.
        const val DEFAULT_KEYFRAME_INTERVAL = 1000
        const val FRAME_KIND_KEY: Byte = 0
        const val FRAME_KIND_XOR_DELTA: Byte = 1
    }
}
