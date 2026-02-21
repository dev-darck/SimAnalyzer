package com.project.analyzer.calibration.trackmap

class TrackMapLapTracker(
    private val merger: TrackMapMerger,
    private val stats: TrackMapStatsCalculator,
    private val minPointsToSave: Int,
) {

    fun handleLapTransition(lapIndex: Int?, runtime: TrackMapRecorderRuntime): TrackMapLapTransitionResult {
        if (lapIndex == null) return TrackMapLapTransitionResult()

        val previous = runtime.lastLapIndex
        if (previous == null) {
            runtime.lastLapIndex = lapIndex
            return TrackMapLapTransitionResult()
        }

        if (lapIndex < previous) {
            runtime.resetLap(clearMap = true)
            runtime.lastLapIndex = lapIndex
            return TrackMapLapTransitionResult(message = "Lap index reset, starting over", forcePublish = true)
        }

        if (lapIndex == previous) {
            return TrackMapLapTransitionResult()
        }

        if (runtime.sawPitInLap) {
            runtime.resetLap(clearMap = false)
            runtime.sawPitInLap = false
            runtime.lastLapIndex = lapIndex
            return TrackMapLapTransitionResult(
                message = "Lap with pit lane ignored",
                forcePublish = true,
            )
        }

        val result = finalizeLap(runtime)
        runtime.sawPitInLap = false
        runtime.lastLapIndex = lapIndex
        return result
    }

    private fun finalizeLap(runtime: TrackMapRecorderRuntime): TrackMapLapTransitionResult {
        val completedLap = runtime.lapPoints.toList()
        if (runtime.lapPoints.size < minPointsToSave) {
            runtime.resetLap(clearMap = false)
            return TrackMapLapTransitionResult(
                message = "Lap too short, keep recording",
                forcePublish = true,
                completedLapPoints = completedLap,
                lapAccepted = false,
            )
        }

        if (runtime.mapPoints.isEmpty()) {
            runtime.mapPoints.addAll(runtime.lapPoints)
        } else {
            merger.merge(runtime.mapPoints, runtime.lapPoints)
        }

        runtime.trackDistanceMeters =
            updateTrackDistance(runtime.trackDistanceMeters, runtime.lapDistanceMeters, runtime.lapsRecorded)
        runtime.bounds = stats.computeBounds(runtime.mapPoints)
        runtime.lapsRecorded += 1

        runtime.resetLap(clearMap = false)
        return TrackMapLapTransitionResult(
            forcePublish = true,
            completedLapPoints = completedLap,
            lapAccepted = true,
        )
    }

    private fun updateTrackDistance(current: Float, lapDistance: Float, lapsRecorded: Int): Float =
        if (lapsRecorded <= 0) {
            lapDistance
        } else {
            ((current * lapsRecorded) + lapDistance) / (lapsRecorded + 1)
        }
}
