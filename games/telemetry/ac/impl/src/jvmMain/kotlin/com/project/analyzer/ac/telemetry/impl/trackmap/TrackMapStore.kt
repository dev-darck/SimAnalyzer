package com.project.analyzer.ac.telemetry.impl.trackmap

import com.project.analyzer.annotations.DataStoreSerializer
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMap
import kotlinx.serialization.Serializable

@Serializable
@DataStoreSerializer
internal data class TrackMapStore(val maps: List<TrackMap> = emptyList())
