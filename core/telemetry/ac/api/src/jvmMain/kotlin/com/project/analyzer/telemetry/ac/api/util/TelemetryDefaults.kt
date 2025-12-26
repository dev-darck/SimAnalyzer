package com.project.analyzer.telemetry.ac.api.util

public fun clamp01(v: Float): Float = when {
    v < 0f -> 0f
    v > 1f -> 1f
    else -> v
}
