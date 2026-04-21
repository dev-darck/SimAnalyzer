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
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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

    override val results: Flow<InputsResult>
        get() = channelFlow {
            var mode = Mode.NONE
            var activeSessionId = 0L
            var lastEmittedTsNs = 0L
            var lastThrottle = 0f
            var lastBrake = 0f
            var lastClutch = 0f
            var lastSteer = 0f

            merge(
                telemetry.events.distinctUntilChanged().map { Input.Event(it) },
                telemetry.frames.map { Input.Frame(it) },
            ).collect { input ->
                when (input) {
                    is Input.Event -> {
                        when (val event = input.event) {
                            is TelemetryLifecycleEvent.SessionStarted -> {
                                activeSessionId = event.session.sessionId
                                mode = Mode.RUNNING
                                lastEmittedTsNs = 0L
                                send(InputsResult.SessionStarted(activeSessionId))
                            }

                            is TelemetryLifecycleEvent.SessionResumed -> {
                                if (activeSessionId != 0L && event.sessionId != activeSessionId) return@collect
                                if (activeSessionId == 0L) activeSessionId = event.sessionId
                                mode = Mode.RUNNING
                                lastEmittedTsNs = 0L
                                send(InputsResult.SessionResumed(event.sessionId))
                            }

                            is TelemetryLifecycleEvent.SessionPaused -> {
                                if (activeSessionId != 0L && event.sessionId != activeSessionId) return@collect
                                if (activeSessionId == 0L) activeSessionId = event.sessionId
                                mode = Mode.PAUSED
                                lastEmittedTsNs = 0L
                                send(InputsResult.SessionPaused(event.sessionId))
                            }

                            is TelemetryLifecycleEvent.SessionEnded -> {
                                if (activeSessionId != 0L && event.sessionId != activeSessionId) return@collect
                                mode = Mode.NONE
                                activeSessionId = 0L
                                lastEmittedTsNs = 0L
                                send(InputsResult.SessionEnded(event.sessionId))
                            }

                            is TelemetryLifecycleEvent.SimDisconnected -> {
                                if (activeSessionId == 0L) return@collect
                                val endedSessionId = activeSessionId
                                mode = Mode.NONE
                                activeSessionId = 0L
                                lastEmittedTsNs = 0L
                                send(InputsResult.SessionEnded(endedSessionId))
                            }

                            else -> Unit
                        }
                    }

                    is Input.Frame -> {
                        if (mode != Mode.RUNNING || activeSessionId == 0L) return@collect

                        val frame = input.frame
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

                        send(
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
            }
        }

    override suspend fun updateSettings(inputHudSettings: InputHudSettings) {
        settingsRepo.update(inputHudSettings)
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

    private sealed interface Input {
        data class Event(val event: TelemetryLifecycleEvent) : Input
        data class Frame(val frame: TelemetryFrame) : Input
    }

    private enum class Mode {
        NONE,
        RUNNING,
        PAUSED,
    }
}
