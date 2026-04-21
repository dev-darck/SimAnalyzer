package com.project.analyzer.utils.shm

import com.sun.jna.Pointer
import com.sun.jna.Structure

/**
 * JNA structure with MSVC pack(4) alignment.
 */
public abstract class Pack4Structure(p: Pointer? = null) : Structure(p) {

    public fun attachMemory(pointer: Pointer?) {
        if (pointer == null) return
        useMemory(pointer)
        read()
    }

    override fun getNativeAlignment(type: Class<*>, value: Any?, isFirstElement: Boolean): Int {
        val alignment = super.getNativeAlignment(type, value, isFirstElement)
        return if (alignment > PACK_ALIGNMENT) PACK_ALIGNMENT else alignment
    }

    private companion object {

        private const val PACK_ALIGNMENT = 4
    }
}
