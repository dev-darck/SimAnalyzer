package com.project.analyzer.calibration.trackmap

import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapBounds

class TrackMapRecorderRuntime {

    val mapPoints: MutableList<Vec2> = mutableListOf()
    val centerlinePoints: MutableList<Vec2> = mutableListOf()
    val lapPoints: MutableList<Vec2> = mutableListOf()
    val pitPoints: MutableList<Vec2> = mutableListOf()
    val leftEdgeWidthMeters: MutableList<Float> = mutableListOf()
    val rightEdgeWidthMeters: MutableList<Float> = mutableListOf()

    var lastLapAccepted: Vec2? = null
    var lastLapDir: Vec2? = null
    var lastPitAccepted: Vec2? = null
    var previousInPitLane: Boolean = false
    var lastGuidanceIndex: Int = 0

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
        leftCoverageRatio = 0f
        rightCoverageRatio = 0f
        lastLapIndex = null
        lastReportedLapIndex = null
        lastReportedPitLane = false
        lastReportedLapsRecorded = 0
        lapsRecorded = 0
        sawPitInLap = false
        manualPitLane = null
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
            leftCoverageRatio = 0f
            rightCoverageRatio = 0f
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
}
