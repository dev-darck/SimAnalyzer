package com.project.analyzer.utils.shm

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinNT
import java.io.Closeable

public class WinMappedRegion(private val mappingName: String) : Closeable {
    private val kernel32 = Kernel32.INSTANCE

    private var handle: WinNT.HANDLE? = null
    private var view: Pointer? = null

    public fun openReadOnly(): Pointer? {
        view?.let { return it }

        val h = kernel32.OpenFileMapping(WinNT.FILE_MAP_READ, false, mappingName) ?: return null

        val p = kernel32.MapViewOfFile(h, WinNT.FILE_MAP_READ, 0, 0, 0) ?: run {
            kernel32.CloseHandle(h)
            return null
        }

        handle = h
        view = p
        return p
    }

    override fun close() {
        view?.let { kernel32.UnmapViewOfFile(it) }
        handle?.let { kernel32.CloseHandle(it) }
        view = null
        handle = null
    }
}
