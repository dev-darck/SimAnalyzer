package com.project.analyzer.ac.telemetry.impl.shm.ace

import com.project.analyzer.ac.telemetry.impl.shm.AcShmLayoutNames
import com.project.analyzer.ac.telemetry.impl.shm.StableMappedPage
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoGraphicsPageView
import com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoStaticPageView
import java.io.Closeable

internal class AcEvoSharedMemoryReader(names: AcShmLayoutNames) : Closeable {

    private val physicsPage = StableMappedPage(
        mappingName = names.physics,
        sizeBytes = SPageFilePhysics().size(),
        packetIdOffsetBytes = 0L,
    )
    private val graphicsPage = StableMappedPage(
        mappingName = names.graphics,
        sizeBytes = AcEvoGraphicsPageView.SIZE_BYTES,
        packetIdOffsetBytes = 0L,
    )
    private val staticsPage = StableMappedPage(
        mappingName = names.statics,
        sizeBytes = AcEvoStaticPageView.SIZE_BYTES,
    )

    private val physics = SPageFilePhysics().apply { attach(physicsPage.memory) }
    private val graphics = AcEvoGraphicsPageView().apply { attachMemory(graphicsPage.memory) }
    private val statics = AcEvoStaticPageView().apply { attachMemory(staticsPage.memory) }

    val view: AceSharedMemoryView = AceSharedMemoryView(
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
