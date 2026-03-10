package com.project.analyzer.ac.telemetry.impl.trackmap

import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMap

public interface TrackMapGameProvider {

    public suspend fun load(gameId: String, trackId: String, layoutId: String?): TrackMap?

    public suspend fun loadAll(gameId: String?): List<TrackMap>
}
