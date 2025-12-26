package com.project.analyzer.calibration.data

import com.project.analyzer.calibration.data.model.CalibrationSample
import com.project.analyzer.calibration.data.model.ReferencePoint
import com.project.analyzer.calibration.domain.TelemetrySampleProvider
import com.project.analyzer.telemetry.ac.api.TelemetryDataSource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Inject
@SingleIn(AppScope::class)
class AcTelemetrySampleProvider(
    telemetry: TelemetryDataSource,
) : TelemetrySampleProvider {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var extractor = PoseExtractor(ReferencePoint.FRONT_AXLE)

    fun setReferencePoint(rp: ReferencePoint) {
        extractor = PoseExtractor(rp)
    }

    override val sample: StateFlow<CalibrationSample> = telemetry.frames()
        .map { frame ->
            val pose = extractor.extract(frame)
            val speed = frame.car?.speedKmh ?: 0f
            val heading = frame.car?.heading ?: 0f

            val ts = frame.timestampNs.takeIf { it > 0L } ?: System.nanoTime()

            CalibrationSample(
                pose = pose,
                speedKmh = speed,
                headingRad = heading,
                timestampNs = ts
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, CalibrationSample(pose = null, speedKmh = 0f, headingRad = 0f))
}
