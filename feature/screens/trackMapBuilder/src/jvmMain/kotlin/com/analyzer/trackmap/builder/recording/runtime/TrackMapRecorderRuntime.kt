package com.analyzer.trackmap.builder.recording.runtime

import com.analyzer.trackmap.data.library.TrackMapStatsCalculator
import com.analyzer.trackmap.domain.model.TrackMapSectorMarker
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapBounds

internal class TrackMapRecorderRuntime {

    val mapPoints: MutableList<Vec2> = mutableListOf()
    val centerlinePoints: MutableList<Vec2> = mutableListOf()
    val lapPoints: MutableList<Vec2> = mutableListOf()
    val pitPoints: MutableList<Vec2> = mutableListOf()
    val leftEdgeWidthMeters: MutableList<Float> = mutableListOf()
    val rightEdgeWidthMeters: MutableList<Float> = mutableListOf()
    val sectorStartSamples: MutableMap<Int, MutableList<Vec2>> = linkedMapOf()
    val lastCapturedLapBySectorStart: MutableMap<Int, Int> = mutableMapOf()

    var lastLapAccepted: Vec2? = null
    var lastLapDir: Vec2? = null
    var lastPitAccepted: Vec2? = null
    var previousInPitLane: Boolean = false
    var lastGuidanceIndex: Int = 0
    var lastSectorIndex: Int? = null
    var sectorCount: Int = 0

    var lapDistanceMeters: Float = 0f
    var trackDistanceMeters: Float = 0f
    var bounds: TrackMapBounds? = null

    var lastUiUpdateNs: Long = 0L
    var lastInfoUpdateNs: Long = 0L

    var lastLapIndex: Int? = null
    var lastReportedLapIndex: Int? = null
    var lastReportedPitLane: Boolean = false
    var lastReportedLapsRecorded: Int = 0

    var lapsRecorded: Int = 0
    var sawPitInLap: Boolean = false
    var manualPitLane: Boolean? = null
    var pitEntryPoint: Vec2? = null
    var pitExitPoint: Vec2? = null
    var leftCoverageRatio: Float = 0f
    var rightCoverageRatio: Float = 0f

    fun resetAll() {
        mapPoints.clear()
        centerlinePoints.clear()
        lapPoints.clear()
        pitPoints.clear()
        leftEdgeWidthMeters.clear()
        rightEdgeWidthMeters.clear()
        lastLapAccepted = null
        lastLapDir = null
        lastPitAccepted = null
        previousInPitLane = false
        lastGuidanceIndex = 0
        lastSectorIndex = null
        sectorCount = 0
        leftCoverageRatio = 0f
        rightCoverageRatio = 0f
        lastLapIndex = null
        lastReportedLapIndex = null
        lastReportedPitLane = false
        lastReportedLapsRecorded = 0
        lapsRecorded = 0
        sawPitInLap = false
        manualPitLane = null
        sectorStartSamples.clear()
        lastCapturedLapBySectorStart.clear()
        pitEntryPoint = null
        pitExitPoint = null
        lapDistanceMeters = 0f
        trackDistanceMeters = 0f
        bounds = null
        lastUiUpdateNs = 0L
        lastInfoUpdateNs = 0L
    }

    fun resetLap(clearMap: Boolean) {
        lapPoints.clear()
        lapDistanceMeters = 0f
        lastLapAccepted = null
        lastLapDir = null
        sawPitInLap = false

        if (clearMap) {
            mapPoints.clear()
            centerlinePoints.clear()
            leftEdgeWidthMeters.clear()
            rightEdgeWidthMeters.clear()
            trackDistanceMeters = 0f
            bounds = null
            lapsRecorded = 0
            pitEntryPoint = null
            pitExitPoint = null
            pitPoints.clear()
            lastPitAccepted = null
            previousInPitLane = false
            lastGuidanceIndex = 0
            lastSectorIndex = null
            sectorCount = 0
            leftCoverageRatio = 0f
            rightCoverageRatio = 0f
            sectorStartSamples.clear()
            lastCapturedLapBySectorStart.clear()
        } else if (mapPoints.isEmpty()) {
            bounds = null
        }
    }

    fun recordLapPoint(point: Vec2, distanceMeters: Float, stats: TrackMapStatsCalculator) {
        lapPoints.add(point)
        lapDistanceMeters += distanceMeters
        lastLapAccepted = point
        if (mapPoints.isEmpty()) {
            bounds = stats.updateBounds(bounds, point)
        }
    }

    fun recordPitPoint(point: Vec2) {
        pitPoints.add(point)
        lastPitAccepted = point
    }

    fun ensureEdgeCapacity(size: Int) {
        while (leftEdgeWidthMeters.size < size) leftEdgeWidthMeters.add(0f)
        while (rightEdgeWidthMeters.size < size) rightEdgeWidthMeters.add(0f)
    }

    fun recordSectorSample(sectorStartIndex: Int, lapIndex: Int?, point: Vec2): Boolean {
        if (sectorStartIndex <= 0) return false
        if (lapIndex != null && lastCapturedLapBySectorStart[sectorStartIndex] == lapIndex) {
            return false
        }

        val samples = sectorStartSamples.getOrPut(sectorStartIndex) { mutableListOf() }
        samples.add(point)
        if (lapIndex != null) {
            lastCapturedLapBySectorStart[sectorStartIndex] = lapIndex
        }
        return samples.size == 1
    }

    fun sectorMarkers(): List<TrackMapSectorMarker> = sectorStartSamples.entries
        .sortedBy { it.key }
        .mapNotNull { (index, samples) ->
            if (samples.isEmpty()) return@mapNotNull null

            var sumX = 0f
            var sumY = 0f
            samples.forEach { sample ->
                sumX += sample.x
                sumY += sample.y
            }
            val count = samples.size
            TrackMapSectorMarker(
                index = index,
                position = Vec2(sumX / count, sumY / count),
                sampleCount = count,
            )
        }
}
