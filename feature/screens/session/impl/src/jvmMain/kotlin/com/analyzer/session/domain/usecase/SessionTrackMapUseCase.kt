package com.analyzer.session.domain.usecase

import com.analyzer.session.domain.model.SessionListDomainItem
import com.project.analyzer.ui.components.TrackMapData

internal interface SessionTrackMapUseCase {

    suspend fun loadTrackMaps(items: Collection<SessionListDomainItem>): Map<String, TrackMapData>
    fun resolveTrackMap(
        trackMapsByKey: Map<String, TrackMapData>,
        gameId: String,
        trackId: String,
        layoutId: String? = null,
    ): TrackMapData?
}
