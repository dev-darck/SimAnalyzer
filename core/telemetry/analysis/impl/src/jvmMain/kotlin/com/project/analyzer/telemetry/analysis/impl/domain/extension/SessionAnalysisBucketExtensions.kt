package com.project.analyzer.telemetry.analysis.impl.domain.extension

import kotlin.math.roundToInt

/**
 * Buckets continuous telemetry into coarse ranges that are easier to summarize in diagnostics and highlights.
 */
internal fun List<Int?>.bucketAt(trackPosition: Float): Int? {
    if (isEmpty()) return null
    val index = ((trackPosition.coerceIn(0f, 1f)) * (size - 1)).roundToInt()
    return getOrNull(index)
}
