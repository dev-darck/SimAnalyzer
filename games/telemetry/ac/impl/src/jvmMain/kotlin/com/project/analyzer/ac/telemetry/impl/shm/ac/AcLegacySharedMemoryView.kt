package com.project.analyzer.ac.telemetry.impl.shm.ac

import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemoryLayout
import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemoryView
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileStatic

public class AcLegacySharedMemoryView(
    val physics: SPageFilePhysics,
    val graphics: SPageFileGraphics,
    val statics: SPageFileStatic,
) : AcSharedMemoryView {

    override val layout: AcSharedMemoryLayout = AcSharedMemoryLayout.LEGACY
}
