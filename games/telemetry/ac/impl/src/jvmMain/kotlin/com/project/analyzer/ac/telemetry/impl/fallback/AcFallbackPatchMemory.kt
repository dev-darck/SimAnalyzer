package com.project.analyzer.ac.telemetry.impl.fallback

import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileStatic

internal interface AcFallbackPatchMemory {

    val physics: SPageFilePhysics

    val graphics: SPageFileGraphics

    val statics: SPageFileStatic
}
