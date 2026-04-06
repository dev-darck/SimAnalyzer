package com.analyzer.session.analysis.presentation.pipeline

import kotlin.math.roundToInt

/**
 * Small interpolation helpers keep chart sampling logic out of the higher-level pipelines.
 */
internal fun interpolateFloat(start: Float?, end: Float?, fraction: Float): Float? = when {
    start != null && end != null -> start + (end - start) * fraction
    start != null -> start
    else -> end
}

internal fun interpolateInt(start: Int?, end: Int?, fraction: Float): Int? = when {
    start != null && end != null -> (start + (end - start) * fraction).roundToInt()
    start != null -> start
    else -> end
}
