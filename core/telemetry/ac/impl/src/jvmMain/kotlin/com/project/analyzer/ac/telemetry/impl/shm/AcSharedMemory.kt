package com.project.analyzer.ac.telemetry.impl.shm

import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileStatic
import java.io.Closeable

interface AcSharedMemory : Closeable {

    val physics: SPageFilePhysics
    val graphics: SPageFileGraphics
    val statics: SPageFileStatic

    fun isAnyAttached(): Boolean

    fun readAll()
}
