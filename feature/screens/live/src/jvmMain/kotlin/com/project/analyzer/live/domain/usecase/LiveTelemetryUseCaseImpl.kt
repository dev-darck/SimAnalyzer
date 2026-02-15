package com.project.analyzer.live.domain.usecase

import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.live.domain.mapper.LiveScreenStateMapper
import com.project.analyzer.live.domain.model.LiveTelemetryResult
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan

@Inject
@SingleIn(ScreenScope::class)
internal class LiveTelemetryUseCaseImpl(
    private val telemetry: TelemetryLifecycle,
    private val mapper: LiveScreenStateMapper,
) : LiveTelemetryUseCase {

    private var currentCarModel: String? = null
    private var currentTrackId: String? = null

    private val isSessionActive: Flow<Boolean>
        get() = telemetry.events
            .scan(false) { active, ev ->
                when (ev) {
                    is TelemetryLifecycleEvent.SessionStarted -> true
                    is TelemetryLifecycleEvent.SessionResumed -> true
                    is TelemetryLifecycleEvent.SessionPaused -> false
                    is TelemetryLifecycleEvent.SessionEnded,
                    is TelemetryLifecycleEvent.SimDisconnected -> false

                    else -> active
                }
            }
            .distinctUntilChanged()

    private val lifecycleResults: Flow<LiveTelemetryResult>
        get() = telemetry.events
            .mapNotNull { ev -> ev.toLiveLifecycleResultOrNull() }
            .distinctUntilChanged()

    private val frameResults: Flow<LiveTelemetryResult>
        get() = combine(telemetry.frames, isSessionActive) { frame, active ->
            if (!active) return@combine null

            val carModel = frame.session?.car?.carModel?.takeIf { it.isNotBlank() }
            val trackId = frame.session?.track?.trackId?.takeIf { it.isNotBlank() }

            if (currentCarModel == null && carModel != null) currentCarModel = carModel
            if (currentTrackId == null && trackId != null) currentTrackId = trackId

            val identityChanged =
                (carModel != null && currentCarModel != null && carModel != currentCarModel) ||
                    (trackId != null && currentTrackId != null && trackId != currentTrackId)

            if (identityChanged) {
                currentCarModel = carModel ?: currentCarModel
                currentTrackId = trackId ?: currentTrackId
                return@combine LiveTelemetryResult.SessionReset
            }

            mapper.map(frame)?.let { LiveTelemetryResult.Data(it) }
        }.filterNotNull()

    override val telemetryFlow: Flow<LiveTelemetryResult> =
        merge(lifecycleResults, frameResults)

    private fun TelemetryLifecycleEvent.toLiveLifecycleResultOrNull(): LiveTelemetryResult? = when (this) {
        is TelemetryLifecycleEvent.SessionEnded -> {
            currentCarModel = null
            currentTrackId = null
            LiveTelemetryResult.SessionEnded(sessionId)
        }

        is TelemetryLifecycleEvent.SimDisconnected -> {
            currentCarModel = null
            currentTrackId = null
            LiveTelemetryResult.SessionEnded(sessionId = -1L)
        }

        else -> null
    }
}
