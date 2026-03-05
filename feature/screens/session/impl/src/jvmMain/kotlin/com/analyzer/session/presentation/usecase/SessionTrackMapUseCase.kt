package com.analyzer.session.presentation.usecase

import com.project.analyzer.ui.components.TrackMapData

internal data class SessionTrackMapRequest(val gameId: String, val trackId: String)

internal interface SessionTrackMapUseCase {

    fun request(gameId: String, trackId: String): SessionTrackMapRequest?
    suspend fun loadTrackMapsByRequest(requests: Set<SessionTrackMapRequest>): Map<String, TrackMapData>
    fun resolveTrackMap(
        trackMapsByKey: Map<String, TrackMapData>,
        gameId: String,
        trackId: String,
    ): TrackMapData?
}
