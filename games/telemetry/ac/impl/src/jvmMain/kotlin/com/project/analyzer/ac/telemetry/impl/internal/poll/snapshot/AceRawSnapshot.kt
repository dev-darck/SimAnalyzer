package com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot

import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemoryLayout
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoGraphicsPageView
import com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoStaticPageView
import com.sun.jna.Memory

internal class AceRawSnapshot(override var timestampNs: Long = 0L) : AcPollSnapshot {

    private val physicsMemory: Memory = Memory(SPageFilePhysics().size().toLong())
    private val graphicsMemory: Memory = Memory(AcEvoGraphicsPageView.SIZE_BYTES.toLong())
    private val staticsMemory: Memory = Memory(AcEvoStaticPageView.SIZE_BYTES.toLong())

    val physics: SPageFilePhysics = SPageFilePhysics().apply { attach(physicsMemory) }
    val graphics: AcEvoGraphicsPageView = AcEvoGraphicsPageView().apply { attachMemory(graphicsMemory) }
    val statics: AcEvoStaticPageView = AcEvoStaticPageView().apply { attachMemory(staticsMemory) }
    val fallback: AceFallbackSnapshotPages = AceFallbackSnapshotPages()

    override val layout: AcSharedMemoryLayout = AcSharedMemoryLayout.ACEVO

    override var frameId: Long = 0L

    override var sessionRestartHint: AcSessionRestartHint = AcSessionRestartHint.NONE

    var needsFallbackPatch: Boolean = false
}
