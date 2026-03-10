package com.analyzer.trackmap.domain.usecase

import com.analyzer.trackmap.domain.model.TrackMapBuilderState
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import kotlinx.coroutines.flow.Flow

interface TrackMapBuilderUseCase {

    fun observeState(): Flow<TrackMapBuilderState>
    fun start()
    fun stop()
    fun reset()
    fun setReferencePoint(point: ReferencePoint)
    fun setFallbackHalfWidthMeters(value: Float)
    fun markPitEntry()
    fun markPitExit()
    suspend fun save()
}
