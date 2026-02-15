package com.project.analyzer.inputs.domain.usecase

import com.project.analyzer.hud.api.HudScope
import com.project.analyzer.inputs.domain.model.InputsResult
import com.project.analyzer.inputs.settings.InputHudSettings
import com.project.analyzer.inputs.settings.repository.InputHudSettingsRepository
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.utils.ext.orZero
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge

@Inject
@SingleIn(HudScope::class)
class InputsUseCaseImpl(
    private val telemetry: TelemetryLifecycle,
    private val settingsRepo: InputHudSettingsRepository
) : InputsUseCase {

    override val settings: Flow<InputHudSettings> = settingsRepo.data

    private val lifecycleResults: Flow<InputsResult>
        get() = telemetry.events
            .mapNotNull { it.toInputsLifecycleResultOrNull() }
            .distinctUntilChanged()

    private val samples: Flow<InputsResult>
        get() = telemetry.frames.map { frame ->
            val controls = frame.car?.controls
            InputsResult.Sample(
                throttle = controls?.throttle.orZero(),
                brake = controls?.brake.orZero(),
                clutch = controls?.clutch.orZero(),
                steerRadians = controls?.steerAngle.orZero(),
                timestampNs = frame.timestampNs
            )
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
}
