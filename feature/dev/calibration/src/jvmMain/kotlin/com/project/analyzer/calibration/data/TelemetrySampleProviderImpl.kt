package com.project.analyzer.calibration.data

import com.project.analyzer.api.di.Default
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.calibration.domain.TelemetrySampleProvider
import com.project.analyzer.calibration.domain.model.CalibrationSample
import com.project.analyzer.calibration.domain.model.WheelDebug
import com.project.analyzer.math.MIN_LEN
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.calibration.ReferencePointPoseExtractor
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycle
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@SingleIn(ScreenScope::class)
@Inject
internal class TelemetrySampleProviderImpl(
    telemetry: TelemetryLifecycle,
    @Default
    defaultDispatcher: CoroutineDispatcher,
) : TelemetrySampleProvider {

    private val scope = CoroutineScope(SupervisorJob() + defaultDispatcher)

    @Volatile
    private var extractor = ReferencePointPoseExtractor(ReferencePoint.CAR_CENTER)

    override fun setReferencePoint(point: ReferencePoint) {
        extractor = ReferencePointPoseExtractor(point)
    }

    override val sample: StateFlow<CalibrationSample> = telemetry.frames
        .map { frame ->
            val pose = extractor.extract(frame)
            val speed = frame.car?.speedKmh ?: 0f
            val heading = frame.car?.heading ?: 0f
            val ts = frame.timestampNs.takeIf { it > 0L } ?: System.nanoTime()
            val session = frame.session
            val track = session?.track
            val car = session?.car

            val wheelDebug = frame.wheels?.let { w ->
                val fl = w.fl?.contactPoint
                val fr = w.fr?.contactPoint
                val rl = w.rl?.contactPoint
                val rr = w.rr?.contactPoint

                WheelDebug(
                    fl = fl?.let { Vec2(it.x, it.z) },
                    fr = fr?.let { Vec2(it.x, it.z) },
                    rl = rl?.let { Vec2(it.x, it.z) },
                    rr = rr?.let { Vec2(it.x, it.z) },
                )
            }

            val axleForward = computeAxleForward(wheelDebug)

            CalibrationSample(
                pose = pose,
                speedKmh = speed,
                headingRad = heading,
                timestampNs = ts,
                wheels = wheelDebug,
                axleForward = axleForward,
                trackId = track?.trackId,
                trackName = track?.trackName,
                carModel = car?.carName ?: car?.carModel,
            )
        }
        .flowOn(defaultDispatcher)
        .stateIn(
            scope,
            SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            CalibrationSample(pose = null, speedKmh = 0f, headingRad = 0f),
        )

    private fun computeAxleForward(wheels: WheelDebug?): Vec2? {
        if (wheels == null) return null

        val front = wheels.fl?.avg(wheels.fr)
        val rear = wheels.rl?.avg(wheels.rr)
        if (front == null || rear == null) return null

        val d = front - rear
        return if (d.len() > MIN_LEN) d.safeNormalized() else null
    }
}
