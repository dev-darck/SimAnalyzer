package com.analyzer.trackmap.domain

import com.analyzer.trackmap.domain.model.TrackMapBuilderState
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import kotlinx.coroutines.flow.StateFlow

interface TrackMapCaptureController {

    val state: StateFlow<TrackMapBuilderState>

    fun start()
    fun stop()
    fun reset()
    fun setReferencePoint(point: ReferencePoint)
    fun setFallbackHalfWidthMeters(value: Float)
    fun markPitEntry()
    fun markPitExit()
    suspend fun save()
}
