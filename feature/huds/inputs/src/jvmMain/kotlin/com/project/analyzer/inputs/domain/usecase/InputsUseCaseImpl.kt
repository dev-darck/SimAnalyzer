package com.project.analyzer.inputs.domain.usecase

import com.project.analyzer.hud.api.HudScope
import com.project.analyzer.inputs.data.repository.InputHudSettingsRepository
import com.project.analyzer.inputs.domain.model.InputsResult
import com.project.analyzer.inputs.settings.InputHudSettings
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.utils.ext.orZero
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlin.math.abs
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds

@Inject
@SingleIn(HudScope::class)
internal class InputsUseCaseImpl(
    private val telemetry: TelemetryLifecycle,
    private val settingsRepo: InputHudSettingsRepository,
) : InputsUseCase {
    override val settings: Flow<InputHudSettings> = settingsRepo.data

    private val lifecycleResults: Flow<InputsResult>
        get() = telemetry.events
            .mapNotNull { it.toInputsLifecycleResultOrNull() }
            .distinctUntilChanged()

    private val samples: Flow<InputsResult>
        get() = flow {
            var lastEmittedTsNs = 0L
            var lastThrottle = 0f
            var lastBrake = 0f
            var lastClutch = 0f
            var lastSteer = 0f

            telemetry.frames.collect { frame ->
                val controls = frame.car?.controls
                val throttle = controls?.throttle.orZero()
                val brake = controls?.brake.orZero()
                val clutch = controls?.clutch.orZero()
                val steer = controls?.steerAngle.orZero()

                if (!shouldEmitSample(
                        frame = frame,
                        lastEmittedTsNs = lastEmittedTsNs,
                        throttle = throttle,
                        brake = brake,
                        clutch = clutch,
                        steer = steer,
                        prevThrottle = lastThrottle,
                        prevBrake = lastBrake,
                        prevClutch = lastClutch,
                        prevSteer = lastSteer,
                    )
                ) {
                    return@collect
                }

                lastEmittedTsNs = frame.timestampNs.takeIf { it > 0L } ?: System.nanoTime()
                lastThrottle = throttle
                lastBrake = brake
                lastClutch = clutch
                lastSteer = steer

                emit(
                    InputsResult.Sample(
                        throttle = throttle,
                        brake = brake,
                        clutch = clutch,
                        steerRadians = steer,
                        timestampNs = frame.timestampNs,
                    ),
                )
            }
        }

    override val results: Flow<InputsResult>
        get() = merge(lifecycleResults, samples)

    override suspend fun updateSettings(inputHudSettings: InputHudSettings) {
        settingsRepo.update(inputHudSettings)
    }

    private fun TelemetryLifecycleEvent.toInputsLifecycleResultOrNull(): InputsResult? = when (this) {
        is TelemetryLifecycleEvent.SessionStarted -> InputsResult.SessionStarted(session.sessionId)
        is TelemetryLifecycleEvent.SessionResumed -> InputsResult.SessionResumed(sessionId)
        is TelemetryLifecycleEvent.SessionPaused -> InputsResult.SessionPaused(sessionId)
        is TelemetryLifecycleEvent.SessionEnded -> InputsResult.SessionEnded(sessionId)
        else -> null
    }

    private fun shouldEmitSample(
        frame: TelemetryFrame,
        lastEmittedTsNs: Long,
        throttle: Float,
        brake: Float,
        clutch: Float,
        steer: Float,
        prevThrottle: Float,
        prevBrake: Float,
        prevClutch: Float,
        prevSteer: Float,
    ): Boolean {
        if (lastEmittedTsNs == 0L) return true

        val tsNs = frame.timestampNs.takeIf { it > 0L } ?: System.nanoTime()
        val dtNs = tsNs - lastEmittedTsNs
        if (dtNs <= 0L) return false

        val maxDelta = max(
            max(abs(throttle - prevThrottle), abs(brake - prevBrake)),
            max(abs(clutch - prevClutch), abs(steer - prevSteer)),
        )

        if (maxDelta >= RAPID_CHANGE_THRESHOLD) {
            return dtNs >= FAST_SAMPLE_WINDOW_NS
        }

        return dtNs >= BASE_SAMPLE_WINDOW_NS
    }

    private companion object {
        val BASE_SAMPLE_WINDOW_NS: Long = 6.milliseconds.inWholeNanoseconds // ~166 Hz in stable phases
        val FAST_SAMPLE_WINDOW_NS: Long = 3.milliseconds.inWholeNanoseconds // up to source rate on sharp changes
        const val RAPID_CHANGE_THRESHOLD: Float = 0.06f
    }
}
