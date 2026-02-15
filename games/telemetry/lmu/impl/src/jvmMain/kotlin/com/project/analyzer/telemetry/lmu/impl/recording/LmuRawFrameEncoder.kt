package com.project.analyzer.telemetry.lmu.impl.recording

import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.lmu.impl.shm.LmuSharedMemory
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(SessionScope::class)
internal class LmuRawFrameEncoder(
    private val shm: LmuSharedMemory,
) {

    val payloadType: String = "lmu_shm_v1"
    val payloadSize: Int = LmuSharedMemory.TELEMETRY_BUFFER_SIZE + LmuSharedMemory.SCORING_BUFFER_SIZE

    fun encode(telemetryVersion: Int, scoringVersion: Int?): ByteArray? {
        if (!shm.isAttached()) return null
        val buffer = ByteArray(payloadSize)
        if (!shm.copyTelemetryBytes(buffer, 0, telemetryVersion)) return null
        if (scoringVersion != null) {
            if (!shm.copyScoringBytes(buffer, LmuSharedMemory.TELEMETRY_BUFFER_SIZE, scoringVersion)) {
                return null
            }
        }
        return buffer
    }
}
