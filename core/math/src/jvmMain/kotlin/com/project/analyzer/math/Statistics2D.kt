package com.project.analyzer.math

import kotlin.math.sqrt

public object Statistics2D {

    public fun mean(points: List<Vec2>): Vec2 {
        var sx = 0.0
        var sy = 0.0
        for (p in points) {
            sx += p.x
            sy += p.y
        }
        val n = points.size.toDouble().coerceAtLeast(1.0)
        return Vec2((sx / n).toFloat(), (sy / n).toFloat())
    }

    /** Population standard deviation in 2D: sqrt(E[(x-mx)^2 + (y-my)^2]). */
    public fun std(points: List<Vec2>, mean: Vec2 = mean(points)): Float {
        var acc = 0.0
        for (p in points) {
            val dx = (p.x - mean.x).toDouble()
            val dy = (p.y - mean.y).toDouble()
            acc += dx * dx + dy * dy
        }
        val n = points.size.toDouble().coerceAtLeast(1.0)
        return sqrt(acc / n).toFloat()
    }
}
