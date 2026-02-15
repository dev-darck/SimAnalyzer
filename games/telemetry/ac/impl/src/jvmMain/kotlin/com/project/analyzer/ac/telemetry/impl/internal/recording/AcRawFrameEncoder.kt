package com.project.analyzer.ac.telemetry.impl.internal.recording

import com.project.analyzer.ac.telemetry.impl.internal.AcRawSnapshot
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileStatic
import com.project.analyzer.api.di.SessionScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(SessionScope::class)
class AcRawFrameEncoder {

    val payloadType: String = "ac_shm_v1"

    private val physicsSize: Int = SPageFilePhysics().size()
    private val graphicsSize: Int = SPageFileGraphics().size()
    private val staticsSize: Int = SPageFileStatic().size()

    val payloadSize: Int = physicsSize + graphicsSize + staticsSize

    fun encode(snapshot: AcRawSnapshot): ByteArray {
        val buffer = ByteArray(payloadSize)
        var offset = 0

        snapshot.physics.pointer.read(0, buffer, offset, physicsSize)
        offset += physicsSize

        snapshot.graphics.pointer.read(0, buffer, offset, graphicsSize)
        offset += graphicsSize

        snapshot.statics.pointer.read(0, buffer, offset, staticsSize)

        return buffer
    }
}
