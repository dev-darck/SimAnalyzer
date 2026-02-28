package com.project.analyzer.telemetry.recording.impl.file.codec

internal object RawFrameStorageSessionCodec : FrameStorageSessionCodec {
    override val codecId: String = "raw"

    override fun describeStorage(semanticPayloadType: String, semanticPayloadSize: Int): StoragePayloadDescriptor =
        StoragePayloadDescriptor(
            storagePayloadType = semanticPayloadType,
            storagePayloadSize = semanticPayloadSize,
        )

    override fun encode(semanticPayloadType: String, semanticPayload: ByteArray): EncodedFramePayload =
        EncodedFramePayload(
            storagePayloadType = semanticPayloadType,
            storagePayload = semanticPayload,
        )
}
