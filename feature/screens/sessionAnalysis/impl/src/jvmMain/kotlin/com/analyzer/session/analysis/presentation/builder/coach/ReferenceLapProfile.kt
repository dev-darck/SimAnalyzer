package com.analyzer.session.analysis.presentation.builder.coach

import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample

internal class ReferenceLapProfile(samples: List<SessionAnalysisSample>) {

    private val entries: List<ReferenceEntry> = samples.asSequence()
        .mapNotNull { sample ->
            val position = sample.trackPosition ?: return@mapNotNull null
            ReferenceEntry(
                trackPosition = position.coerceIn(0f, 1f),
                x = sample.trackX,
                y = sample.trackY,
                speedKmh = sample.speedKmh,
            )
        }
        .sortedBy(ReferenceEntry::trackPosition)
        .toList()

    fun pointAt(trackPosition: Float?): Pair<Float, Float>? {
        val position = trackPosition ?: return null
        val pair = boundsAt(position.coerceIn(0f, 1f)) ?: return null
        val start = pair.first
        val end = pair.second
        val x = interpolateCoachValue(start.trackPosition, end.trackPosition, start.x, end.x, position)
            ?: return null
        val y = interpolateCoachValue(start.trackPosition, end.trackPosition, start.y, end.y, position)
            ?: return null
        return x to y
    }

    fun speedAt(trackPosition: Float?): Float? {
        val position = trackPosition ?: return null
        val pair = boundsAt(position.coerceIn(0f, 1f)) ?: return null
        return interpolateCoachValue(
            startPos = pair.first.trackPosition,
            endPos = pair.second.trackPosition,
            startValue = pair.first.speedKmh,
            endValue = pair.second.speedKmh,
            valuePos = position,
        )
    }

    private fun boundsAt(trackPosition: Float): Pair<ReferenceEntry, ReferenceEntry>? {
        if (entries.isEmpty()) return null
        if (entries.size == 1) return entries.first() to entries.first()

        val exactIndex = entries.indexOfFirst { entry -> entry.trackPosition >= trackPosition }
        if (exactIndex == -1) {
            return entries[entries.lastIndex - 1] to entries.last()
        }
        if (exactIndex == 0) {
            return entries.first() to entries.getOrElse(1) { entries.first() }
        }
        return entries[exactIndex - 1] to entries[exactIndex]
    }
}
