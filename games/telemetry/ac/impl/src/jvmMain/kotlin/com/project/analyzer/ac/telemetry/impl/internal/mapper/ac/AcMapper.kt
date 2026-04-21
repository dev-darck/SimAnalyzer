package com.project.analyzer.ac.telemetry.impl.internal.mapper.ac

import com.project.analyzer.ac.telemetry.impl.internal.mapper.common.AcBaseFrameMapper
import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AcLegacyRawSnapshot
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(SessionScope::class)
internal class AcMapper(private val baseFrameMapper: AcBaseFrameMapper) {

    suspend fun map(snapshot: AcLegacyRawSnapshot): TelemetryFrame = baseFrameMapper.map(
        frameId = snapshot.frameId,
        timestampNs = snapshot.timestampNs,
        physics = snapshot.physics,
        graphics = snapshot.graphics,
        statics = snapshot.statics,
    )
}
