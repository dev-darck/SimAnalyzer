package com.analyzer.trackmap.builder.recording.geometry

import com.project.analyzer.math.Vec2

internal fun List<Vec2>.findClosestIndex(target: Vec2): Int {
    if (isEmpty()) return -1

    var bestIndex = 0
    var bestDistanceSquared = (this[0] - target).len2()
    for (index in 1 until size) {
        val distanceSquared = (this[index] - target).len2()
        if (distanceSquared < bestDistanceSquared) {
            bestDistanceSquared = distanceSquared
            bestIndex = index
        }
    }
    return bestIndex
}
