package com.project.analyzer.ac.telemetry.impl.shm

import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileStatic
import com.project.analyzer.api.di.SessionScope
import com.sun.jna.Pointer
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

@Inject
@SingleIn(SessionScope::class)
@ContributesBinding(SessionScope::class, binding = binding<AcSharedMemory>())
class DefaultAcSharedMemory(
    names: AcShmNames
) : AcSharedMemory {

    private val physicsRegion = WinMappedRegion(names.physics)
    private val graphicsRegion = WinMappedRegion(names.graphics)
    private val staticsRegion = WinMappedRegion(names.statics)

    override val physics: SPageFilePhysics = SPageFilePhysics(Pointer.NULL)
    override val graphics: SPageFileGraphics = SPageFileGraphics(Pointer.NULL)
    override val statics: SPageFileStatic = SPageFileStatic(Pointer.NULL)

    private var physicsAttached = false
    private var graphicsAttached = false
    private var staticsAttached = false

    override fun isAnyAttached(): Boolean = physicsAttached || graphicsAttached || staticsAttached

    override fun readAll() {
        if (!physicsAttached) {
            val p = physicsRegion.openReadOnly()
            if (p != null) {
                physics.attach(p)
                physicsAttached = true
            }
        }
        if (!graphicsAttached) {
            val p = graphicsRegion.openReadOnly()
            if (p != null) {
                graphics.attach(p)
                graphicsAttached = true
            }
        }
        if (!staticsAttached) {
            val p = staticsRegion.openReadOnly()
            if (p != null) {
                statics.attach(p)
                staticsAttached = true
            }
        }

        if (physicsAttached) physics.read()
        if (graphicsAttached) graphics.read()
        if (staticsAttached) statics.read()
    }

    override fun close() {
        physicsRegion.close()
        graphicsRegion.close()
        staticsRegion.close()

        physicsAttached = false
        graphicsAttached = false
        staticsAttached = false
    }
}
