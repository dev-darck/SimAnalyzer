package com.project.analyzer.telemetry.ac.api.trackmap

import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMap

public interface TrackMapRepository {

    public suspend fun save(trackMap: TrackMap)

    public suspend fun load(gameId: String, trackId: String): TrackMap?

    public suspend fun loadAll(gameId: String? = null): List<TrackMap>
}
