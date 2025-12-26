package com.project.analyzer.calibration.data

import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.calibration.data.model.CalibrationSample
import com.project.analyzer.calibration.data.model.WheelDebug
import com.project.analyzer.calibration.domain.TelemetrySampleProvider
import com.project.analyzer.telemetry.ac.api.TelemetryDataSource
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.telemetry.ac.api.model.math.MinLen
import com.project.analyzer.telemetry.ac.api.model.math.Vec2
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Inject
@SingleIn(ScreenScope::class)
@ContributesBinding(ScreenScope::class, binding = binding<TelemetrySampleProvider>())
class AcTelemetrySampleProvider(
    telemetry: TelemetryDataSource,
) : TelemetrySampleProvider {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var extractor = PoseExtractor(ReferencePoint.CAR_CENTER)

    fun setReferencePoint(rp: ReferencePoint) {
        extractor = PoseExtractor(rp)
        println("AcTelemetrySampleProvider: setReferencePoint($rp)")
    }

    override val sample: StateFlow<CalibrationSample> = telemetry.frames()
        .map { frame ->
            val pose = extractor.extract(frame)
            val speed = frame.car?.speedKmh ?: 0f
            val heading = frame.car?.heading ?: 0f
            val ts = frame.timestampNs.takeIf { it > 0L } ?: System.nanoTime()

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
                axleForward = axleForward
            )
        }
        .stateIn(
            scope,
            SharingStarted.Eagerly,
            CalibrationSample(pose = null, speedKmh = 0f, headingRad = 0f)
        )

    private fun computeAxleForward(wheels: WheelDebug?): Vec2? {
        if (wheels == null) return null

        val front = avg(wheels.fl, wheels.fr)
        val rear = avg(wheels.rl, wheels.rr)
        if (front == null || rear == null) return null

        val d = front - rear
        return if (d.len() > MinLen) d.safeNormalized() else null
    }

    private fun avg(a: Vec2?, b: Vec2?): Vec2? = when {
        a != null && b != null -> (a + b).half()
        a != null -> a
        b != null -> b
        else -> null
    }
}
