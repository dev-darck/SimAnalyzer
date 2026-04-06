package com.project.analyzer.telemetry.recording.impl.reader.codec

internal class FrameStoragePayloadReader(storagePayloadType: String) {

    val semanticPayloadType: String = storagePayloadType.removeSuffix(XOR_SUFFIX)

    private val isXorDelta: Boolean = storagePayloadType.endsWith(XOR_SUFFIX)
    private var previousSemanticPayload: ByteArray? = null

    fun decode(storagePayload: ByteArray): ByteArray? {
        if (!isXorDelta) return storagePayload
        if (storagePayload.isEmpty()) return null

        val frameKind = storagePayload[0].toInt()
        val current = ByteArray(storagePayload.size - 1)

        return when (frameKind) {
            XorFrameKindKey -> {
                storagePayload.copyInto(current, destinationOffset = 0, startIndex = 1)
                previousSemanticPayload = current
                current
            }

            XorFrameKindDelta -> {
                val previous = previousSemanticPayload ?: return null
                if (previous.size != current.size) return null

                var index = 0
                while (index < current.size) {
                    current[index] = (storagePayload[index + 1].toInt() xor previous[index].toInt()).toByte()
                    index += 1
                }
                previousSemanticPayload = current
                current
            }

            else -> null
        }
    }
}

private const val XOR_SUFFIX: String = "#xor1"
private const val XorFrameKindKey: Int = 0
private const val XorFrameKindDelta: Int = 1
