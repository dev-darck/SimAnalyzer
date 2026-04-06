package com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner

internal data class CornerWindow(val startIndex: Int, val endIndex: Int, val wrapsAroundLap: Boolean = false) {

    fun sampleIndices(totalSampleCount: Int): List<Int> {
        if (totalSampleCount <= 0) return emptyList()
        if (!wrapsAroundLap) {
            if (startIndex > endIndex) return emptyList()
            return (startIndex..endIndex)
                .filter { index -> index in 0 until totalSampleCount }
        }

        val normalizedStartIndex = startIndex.coerceIn(0, totalSampleCount - 1)
        val normalizedEndIndex = endIndex.coerceIn(0, totalSampleCount - 1)
        return buildList {
            for (index in normalizedStartIndex until totalSampleCount) add(index)
            for (index in 0..normalizedEndIndex) add(index)
        }
    }

    fun expanded(totalSampleCount: Int, leadingPadding: Int, trailingPadding: Int): CornerWindow {
        if (totalSampleCount <= 0) return this
        if (!wrapsAroundLap) {
            return copy(
                startIndex = (startIndex - leadingPadding).coerceAtLeast(0),
                endIndex = (endIndex + trailingPadding).coerceAtMost(totalSampleCount - 1),
            )
        }

        var expandedStart = startIndex - leadingPadding
        var expandedEnd = endIndex + trailingPadding
        var expandedWraps = true

        if (expandedStart < 0) {
            expandedStart = (expandedStart % totalSampleCount + totalSampleCount) % totalSampleCount
        }
        if (expandedEnd >= totalSampleCount) {
            expandedEnd %= totalSampleCount
        }

        return copy(
            startIndex = expandedStart.coerceIn(0, totalSampleCount - 1),
            endIndex = expandedEnd.coerceIn(0, totalSampleCount - 1),
            wrapsAroundLap = expandedWraps,
        )
    }
}
