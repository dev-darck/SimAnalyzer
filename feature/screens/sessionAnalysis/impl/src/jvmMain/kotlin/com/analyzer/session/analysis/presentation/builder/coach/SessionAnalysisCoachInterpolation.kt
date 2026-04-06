package com.analyzer.session.analysis.presentation.builder.coach

internal fun interpolateCoachValue(
    startPos: Float,
    endPos: Float,
    startValue: Float?,
    endValue: Float?,
    valuePos: Float,
): Float? {
    val start = startValue ?: return endValue
    val end = endValue ?: return startValue
    if (startPos == endPos) return start
    val t = ((valuePos - startPos) / (endPos - startPos)).coerceIn(0f, 1f)
    return start + (end - start) * t
}
