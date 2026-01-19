package com.project.analyzer.live.domain.usecase

import com.project.analyzer.live.domain.mapper.LiveScreenStateMapper
import com.project.analyzer.live.domain.model.LiveTelemetryResult
import com.project.analyzer.telemetry.ac.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.ac.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.telemetry.ac.api.model.TelemetryFrame
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Inject
internal class LiveTelemetryUseCaseImpl(
    telemetry: TelemetryLifecycle,
    private val mapper: LiveScreenStateMapper,
) : LiveTelemetryUseCase {

    private var currentCarModel: String? = null
    private var currentTrackId: String? = null

    override val telemetryFlow: Flow<LiveTelemetryResult> = combine(
        telemetry.frames,
        telemetry.events.map { it.toSimplifiedEvent() }.distinctUntilChanged()
    ) { frame, event ->
        processFrame(frame, event)
    }

    private fun processFrame(frame: TelemetryFrame, event: SimplifiedEvent): LiveTelemetryResult {
        if (event == SimplifiedEvent.SESSION_ENDED) {
            return LiveTelemetryResult.SessionEnded
        }

        val carModel = frame.session?.car?.carModel
        val trackId = frame.session?.track?.trackId

        if (carModel != currentCarModel || trackId != currentTrackId) {
            currentCarModel = carModel
            currentTrackId = trackId
            return LiveTelemetryResult.SessionReset
        }

        val state = mapper.map(frame) ?: return LiveTelemetryResult.NoData
        return LiveTelemetryResult.Data(state)
    }

    private fun TelemetryLifecycleEvent.toSimplifiedEvent(): SimplifiedEvent = when (this) {
        is TelemetryLifecycleEvent.SessionStarted -> SimplifiedEvent.SESSION_ACTIVE
        is TelemetryLifecycleEvent.SessionEnded,
        is TelemetryLifecycleEvent.SimDisconnected -> SimplifiedEvent.SESSION_ENDED

        else -> SimplifiedEvent.SESSION_ACTIVE
    }

    private enum class SimplifiedEvent {
        SESSION_ACTIVE,
        SESSION_ENDED
    }
}
