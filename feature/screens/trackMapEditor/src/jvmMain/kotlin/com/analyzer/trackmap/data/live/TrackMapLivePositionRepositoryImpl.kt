package com.analyzer.trackmap.data.live

import com.analyzer.trackmap.data.live.engine.TrackMapLivePositionResolver
import com.analyzer.trackmap.domain.model.TrackMapLibraryItem
import com.project.analyzer.api.di.Default
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.api.contract.TelemetryFrameSource
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.sample
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
@SingleIn(ScreenScope::class)
@ContributesBinding(ScreenScope::class, binding = binding<TrackMapLivePositionRepository>())
@Inject
class TrackMapLivePositionRepositoryImpl(
    private val telemetryFrames: TelemetryFrameSource,
    @param:Default
    private val defaultDispatcher: CoroutineDispatcher,
) : TrackMapLivePositionRepository {

    override fun observe(item: TrackMapLibraryItem): Flow<Vec2?> {
        val resolver = TrackMapLivePositionResolver(item)

        return telemetryFrames.frames
            .sample(50.milliseconds)
            .map(resolver::resolve)
            .distinctUntilChanged(::samePosition)
            .flowOn(defaultDispatcher)
            .onStart { emit(null) }
    }
}

private fun samePosition(old: Vec2?, new: Vec2?): Boolean {
    if (old == null || new == null) return old == new
    return abs(old.x - new.x) < POSITION_EPSILON && abs(old.y - new.y) < POSITION_EPSILON
}

private const val POSITION_EPSILON = 0.25f
