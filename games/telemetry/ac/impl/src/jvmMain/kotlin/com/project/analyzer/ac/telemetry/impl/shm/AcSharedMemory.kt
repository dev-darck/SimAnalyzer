package com.project.analyzer.ac.telemetry.impl.shm

import java.io.Closeable

internal interface AcSharedMemory : Closeable {

    val layout: AcSharedMemoryLayout

    val view: AcSharedMemoryView

    fun isAnyAttached(): Boolean

    fun readAll()
}
