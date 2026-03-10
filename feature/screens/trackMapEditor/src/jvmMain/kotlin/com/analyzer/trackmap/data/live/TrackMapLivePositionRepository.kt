package com.analyzer.trackmap.data.live

import com.analyzer.trackmap.domain.model.TrackMapLibraryItem
import com.project.analyzer.math.Vec2
import kotlinx.coroutines.flow.Flow

interface TrackMapLivePositionRepository {
    fun observe(item: TrackMapLibraryItem): Flow<Vec2?>
}
