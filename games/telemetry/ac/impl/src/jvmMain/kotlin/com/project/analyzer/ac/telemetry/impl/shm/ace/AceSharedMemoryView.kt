package com.project.analyzer.ac.telemetry.impl.shm.ace

import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemoryLayout
import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemoryView
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoGraphicsPageView
import com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoStaticPageView

public class AceSharedMemoryView(
    val physics: SPageFilePhysics,
    val graphics: AcEvoGraphicsPageView,
    val statics: AcEvoStaticPageView,
) : AcSharedMemoryView {

    override val layout: AcSharedMemoryLayout = AcSharedMemoryLayout.ACEVO
}
