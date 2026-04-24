package com.analyzer.session.domain.usecase

import com.project.analyzer.ui.components.TrackMapData

internal interface SessionTrackMapUseCase {

    suspend fun loadTrackMaps(items: Collection<SessionTrackMapIdentity>): Map<String, TrackMapData>
    fun resolveTrackMap(
        trackMapsByKey: Map<String, TrackMapData>,
        gameId: String,
        trackId: String,
        layoutId: String? = null,
    ): TrackMapData?
}

internal data class SessionTrackMapIdentity(
    val sessionId: Long,
    val gameId: String,
    val trackId: String,
    val layoutId: String? = null,
)
