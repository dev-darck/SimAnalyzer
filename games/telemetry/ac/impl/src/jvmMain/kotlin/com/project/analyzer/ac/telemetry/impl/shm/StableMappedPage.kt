package com.project.analyzer.ac.telemetry.impl.shm

import com.project.analyzer.utils.shm.WinMappedRegion
import com.sun.jna.Memory
import com.sun.jna.Pointer
import java.io.Closeable

internal class StableMappedPage(mappingName: String, sizeBytes: Int, private val packetIdOffsetBytes: Long? = null) :
    Closeable {

    private val region = WinMappedRegion(mappingName)
    private val buffer = ByteArray(sizeBytes)
    private var sourcePointer: Pointer? = null

    val memory: Memory = Memory(sizeBytes.toLong())

    val isAttached: Boolean
        get() = sourcePointer != null

    fun refresh(): Boolean {
        val source = sourcePointer ?: region.openReadOnly()?.also { sourcePointer = it } ?: return false
        copyIntoStaging(source)
        return true
    }

    override fun close() {
        region.close()
        sourcePointer = null
    }

    private fun copyIntoStaging(source: Pointer) {
        if (packetIdOffsetBytes == null) {
            source.read(0, buffer, 0, buffer.size)
            memory.write(0, buffer, 0, buffer.size)
            return
        }

        repeat(MAX_STABLE_COPY_ATTEMPTS) {
            val packetBefore = source.getInt(packetIdOffsetBytes)
            source.read(0, buffer, 0, buffer.size)
            val packetAfter = source.getInt(packetIdOffsetBytes)

            if (packetBefore == packetAfter) {
                memory.write(0, buffer, 0, buffer.size)
                return
            }

            Thread.onSpinWait()
        }

        memory.write(0, buffer, 0, buffer.size)
    }

    private companion object {
        private const val MAX_STABLE_COPY_ATTEMPTS = 8
    }
}
