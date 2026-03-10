package com.analyzer.trackmap.domain.usecase.live

import com.analyzer.trackmap.data.live.TrackMapLivePositionRepository
import com.analyzer.trackmap.domain.model.TrackMapLibraryItem
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.math.Vec2
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.Flow

@SingleIn(ScreenScope::class)
@ContributesBinding(ScreenScope::class, binding = binding<ObserveTrackMapLivePositionUseCase>())
@Inject
class ObserveTrackMapLivePositionUseCaseImpl(private val repository: TrackMapLivePositionRepository) :
    ObserveTrackMapLivePositionUseCase {

    override fun observe(item: TrackMapLibraryItem): Flow<Vec2?> = repository.observe(item)
}
