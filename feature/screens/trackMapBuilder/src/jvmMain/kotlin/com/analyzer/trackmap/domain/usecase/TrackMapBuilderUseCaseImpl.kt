package com.analyzer.trackmap.domain.usecase

import com.analyzer.trackmap.domain.TrackMapCaptureController
import com.analyzer.trackmap.domain.model.TrackMapBuilderState
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.Flow

@SingleIn(ScreenScope::class)
@ContributesBinding(ScreenScope::class, binding = binding<TrackMapBuilderUseCase>())
@Inject
class TrackMapBuilderUseCaseImpl(private val controller: TrackMapCaptureController) : TrackMapBuilderUseCase {

    override fun observeState(): Flow<TrackMapBuilderState> = controller.state

    override fun start() {
        controller.start()
    }

    override fun stop() {
        controller.stop()
    }

    override fun reset() {
        controller.reset()
    }

    override fun setReferencePoint(point: ReferencePoint) {
        controller.setReferencePoint(point)
    }

    override fun setFallbackHalfWidthMeters(value: Float) {
        controller.setFallbackHalfWidthMeters(value)
    }

    override fun markPitEntry() {
        controller.markPitEntry()
    }

    override fun markPitExit() {
        controller.markPitExit()
    }

    override suspend fun save() {
        controller.save()
    }
}
