package com.analyzer.trackmap.domain.usecase.live

import com.analyzer.trackmap.domain.model.TrackMapLibraryItem
import com.project.analyzer.math.Vec2
import kotlinx.coroutines.flow.Flow

interface ObserveTrackMapLivePositionUseCase {

    fun observe(item: TrackMapLibraryItem): Flow<Vec2?>
}
