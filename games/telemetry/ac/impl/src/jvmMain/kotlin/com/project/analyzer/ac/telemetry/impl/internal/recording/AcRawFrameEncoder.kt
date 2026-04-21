package com.project.analyzer.ac.telemetry.impl.internal.recording

import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AcLegacyRawSnapshot
import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AcPollSnapshot
import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AceRawSnapshot
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileStatic
import com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoGraphicsPageView
import com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoStaticPageView
import com.project.analyzer.api.di.SessionScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(SessionScope::class)
class AcRawFrameEncoder {

    private val physicsSize: Int = SPageFilePhysics().size()
    private val legacyGraphicsSize: Int = SPageFileGraphics().size()
    private val legacyStaticsSize: Int = SPageFileStatic().size()
    private val aceGraphicsSize: Int = AcEvoGraphicsPageView.SIZE_BYTES
    private val aceStaticsSize: Int = AcEvoStaticPageView.SIZE_BYTES

    fun encode(snapshot: AcPollSnapshot): EncodedAcRawFrame = when (snapshot) {
        is AcLegacyRawSnapshot -> EncodedAcRawFrame(
            payloadType = LEGACY_PAYLOAD_TYPE,
            payload = encodeLegacy(snapshot),
        )

        is AceRawSnapshot -> EncodedAcRawFrame(
            payloadType = ACE_PAYLOAD_TYPE,
            payload = encodeAce(snapshot),
        )
    }

    private fun encodeLegacy(snapshot: AcLegacyRawSnapshot): ByteArray {
        snapshot.physics.write()
        snapshot.graphics.write()
        snapshot.statics.write()

        val buffer = ByteArray(physicsSize + legacyGraphicsSize + legacyStaticsSize)
        var offset = 0

        snapshot.physics.pointer.read(0, buffer, offset, physicsSize)
        offset += physicsSize

        snapshot.graphics.pointer.read(0, buffer, offset, legacyGraphicsSize)
        offset += legacyGraphicsSize

        snapshot.statics.pointer.read(0, buffer, offset, legacyStaticsSize)

        return buffer
    }

    private fun encodeAce(snapshot: AceRawSnapshot): ByteArray {
        snapshot.physics.write()
        snapshot.graphics.write()
        snapshot.statics.write()

        val buffer = ByteArray(physicsSize + aceGraphicsSize + aceStaticsSize)
        var offset = 0

        snapshot.physics.pointer.read(0, buffer, offset, physicsSize)
        offset += physicsSize

        snapshot.graphics.pointer.read(0, buffer, offset, aceGraphicsSize)
        offset += aceGraphicsSize

        snapshot.statics.pointer.read(0, buffer, offset, aceStaticsSize)

        return buffer
    }

    private companion object {

        const val LEGACY_PAYLOAD_TYPE = "ac_shm_v1"
        const val ACE_PAYLOAD_TYPE = "ace_shm_v1"
    }
}
