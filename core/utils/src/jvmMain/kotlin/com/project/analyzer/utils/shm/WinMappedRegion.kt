package com.project.analyzer.utils.shm

import com.project.analyzer.leak.api.LeakCanaryRuntime
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinNT
import java.io.Closeable

public class WinMappedRegion(private val mappingNames: List<String>) : Closeable {
    private val kernel32 = Kernel32.INSTANCE

    private var handle: WinNT.HANDLE? = null
    private var view: Pointer? = null
    private var openedMappingName: String? = null

    public val activeMappingName: String?
        get() = openedMappingName

    public constructor(mappingName: String) : this(listOf(mappingName))

    public fun openReadOnly(): Pointer? {
        view?.let { return it }

        for (mappingName in mappingNames) {
            val h = kernel32.OpenFileMapping(WinNT.FILE_MAP_READ, false, mappingName) ?: continue
            val p = kernel32.MapViewOfFile(h, WinNT.FILE_MAP_READ, 0, 0, 0) ?: run {
                kernel32.CloseHandle(h)
                continue
            }

            handle = h
            view = p
            openedMappingName = mappingName
            return p
        }

        return null
    }

    override fun close() {
        val closingView = view
        val closingHandle = handle
        val mappingName = openedMappingName ?: mappingNames.firstOrNull() ?: "<unknown>"

        closingView?.let { kernel32.UnmapViewOfFile(it) }
        closingHandle?.let { kernel32.CloseHandle(it) }

        view = null
        handle = null
        openedMappingName = null

        if (closingView != null) {
            LeakCanaryRuntime.watch(closingView, "WinMappedRegion.view($mappingName)")
        }
        if (closingHandle != null) {
            LeakCanaryRuntime.watch(closingHandle, "WinMappedRegion.handle($mappingName)")
        }
    }
}
