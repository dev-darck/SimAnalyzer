package com.project.analyzer.telemetry.analysis.impl.domain.extension

import kotlin.math.roundToInt

/**
 * Provides predictable down-sampling so long telemetry traces stay cheap without losing key endpoints.
 */
internal fun <T> List<T>.sampleEvenly(maxSize: Int): List<T> {
    if (isEmpty() || maxSize <= 0) return emptyList()
    if (size <= maxSize) return this
    if (maxSize == 1) return listOf(first())

    val lastSourceIndex = lastIndex
    return buildList(maxSize) {
        repeat(maxSize) { sampleIndex ->
            val fraction = sampleIndex.toFloat() / (maxSize - 1).toFloat()
            val sourceIndex = (fraction * lastSourceIndex.toFloat()).roundToInt().coerceIn(0, lastSourceIndex)
            add(this@sampleEvenly[sourceIndex])
        }
    }
}
