package com.project.analyzer.telemetry.recording.impl.file.codec

internal interface FrameStorageSessionCodec {
    val codecId: String

    fun describeStorage(semanticPayloadType: String, semanticPayloadSize: Int): StoragePayloadDescriptor

    fun encode(semanticPayloadType: String, semanticPayload: ByteArray): EncodedFramePayload
}
