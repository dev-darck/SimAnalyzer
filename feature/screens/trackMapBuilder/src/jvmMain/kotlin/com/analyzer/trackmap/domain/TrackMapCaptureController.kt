package com.analyzer.trackmap.domain

import com.analyzer.trackmap.domain.model.TrackMapBuilderState
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import kotlinx.coroutines.flow.StateFlow

interface TrackMapCaptureController {

    val state: StateFlow<TrackMapBuilderState>

    suspend fun start()
    suspend fun stop()
    suspend fun reset()
    suspend fun setReferencePoint(point: ReferencePoint)
    suspend fun setFallbackHalfWidthMeters(value: Float)
    suspend fun markPitEntry()
    suspend fun markPitExit()
    suspend fun save()
}
