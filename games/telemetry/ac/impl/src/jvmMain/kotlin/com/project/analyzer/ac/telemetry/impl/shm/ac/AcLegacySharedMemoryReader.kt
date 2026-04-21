package com.project.analyzer.ac.telemetry.impl.shm.ac

import com.project.analyzer.ac.telemetry.impl.shm.AcShmLayoutNames
import com.project.analyzer.ac.telemetry.impl.shm.StableMappedPage
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileStatic
import com.sun.jna.Pointer
import java.io.Closeable

internal class AcLegacySharedMemoryReader(names: AcShmLayoutNames) : Closeable {

    private val physicsPage = StableMappedPage(
        mappingName = names.physics,
        sizeBytes = SPageFilePhysics().size(),
        packetIdOffsetBytes = 0L,
    )
    private val graphicsPage = StableMappedPage(
        mappingName = names.graphics,
        sizeBytes = SPageFileGraphics().size(),
        packetIdOffsetBytes = 0L,
    )
    private val staticsPage = StableMappedPage(
        mappingName = names.statics,
        sizeBytes = SPageFileStatic().size(),
    )

    val physics: SPageFilePhysics = SPageFilePhysics(Pointer.NULL).apply { attach(physicsPage.memory) }
    val graphics: SPageFileGraphics = SPageFileGraphics(Pointer.NULL).apply { attach(graphicsPage.memory) }
    val statics: SPageFileStatic = SPageFileStatic(Pointer.NULL).apply { attach(staticsPage.memory) }
    val view: AcLegacySharedMemoryView = AcLegacySharedMemoryView(
        physics = physics,
        graphics = graphics,
        statics = statics,
    )

    fun isAnyAttached(): Boolean = physicsPage.isAttached || graphicsPage.isAttached || staticsPage.isAttached

    fun readAll() {
        if (physicsPage.refresh()) physics.read()
        if (graphicsPage.refresh()) graphics.read()
        if (staticsPage.refresh()) statics.read()
    }

    override fun close() {
        physicsPage.close()
        graphicsPage.close()
        staticsPage.close()
    }
}
