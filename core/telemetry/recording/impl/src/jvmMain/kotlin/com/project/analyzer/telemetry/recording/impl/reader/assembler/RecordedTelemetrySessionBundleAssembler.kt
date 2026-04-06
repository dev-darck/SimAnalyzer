package com.project.analyzer.telemetry.recording.impl.reader.assembler

import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionBundle
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionLocation
import dev.zacsweers.metro.Inject

@Inject
internal class RecordedTelemetrySessionBundleAssembler {

    fun build(locations: List<RecordedTelemetrySessionLocation>): List<RecordedTelemetrySessionBundle> {
        if (locations.isEmpty()) return emptyList()

        val sorted = locations.sortedBy { it.metadata.startedAtMs }
        val explicitByGroupId = linkedMapOf<String, MutableList<RecordedTelemetrySessionLocation>>()
        val legacyLocations = mutableListOf<RecordedTelemetrySessionLocation>()

        sorted.forEach { location ->
            val groupId = location.metadata.sessionGroupId
                ?.trim()
                ?.takeIf(String::isNotBlank)
            if (groupId == null) {
                legacyLocations += location
            } else {
                explicitByGroupId.getOrPut(groupId) { mutableListOf() } += location
            }
        }

        val explicitBundles = explicitByGroupId.values.flatMap(::buildExplicitBundles)
        val legacyBundles = buildLegacyBundles(legacyLocations)
        return mergeSplitWeekendBundles(explicitBundles + legacyBundles)
            .sortedByDescending { it.metadata.startedAtMs }
    }

    private fun buildExplicitBundles(
        locations: List<RecordedTelemetrySessionLocation>,
    ): List<RecordedTelemetrySessionBundle> {
        if (locations.isEmpty()) return emptyList()

        val grouped = mutableListOf<MutableList<RecordedTelemetrySessionLocation>>()
        locations.sortedBy { it.metadata.startedAtMs }.forEach { location ->
            val current = grouped.lastOrNull()
            if (current == null || !sameBundleIdentity(current.last(), location)) {
                grouped += mutableListOf(location)
            } else {
                current += location
            }
        }
        return grouped.map(::buildBundle)
    }

    private fun buildLegacyBundles(
        locations: List<RecordedTelemetrySessionLocation>,
    ): List<RecordedTelemetrySessionBundle> {
        if (locations.isEmpty()) return emptyList()

        val grouped = mutableListOf<MutableList<RecordedTelemetrySessionLocation>>()
        locations.sortedBy { it.metadata.startedAtMs }.forEach { location ->
            val current = grouped.lastOrNull()
            if (current == null || !canMergeIntoSameBundle(current.last(), location)) {
                grouped += mutableListOf(location)
            } else {
                current += location
            }
        }
        return grouped.map(::buildBundle)
    }

    private fun mergeSplitWeekendBundles(
        bundles: List<RecordedTelemetrySessionBundle>,
    ): List<RecordedTelemetrySessionBundle> {
        if (bundles.isEmpty()) return emptyList()

        val merged = mutableListOf<MutableList<RecordedTelemetrySessionLocation>>()
        bundles.sortedBy { it.metadata.startedAtMs }.forEach { bundle ->
            val current = merged.lastOrNull()
            if (current == null || !shouldMergeSplitWeekend(current.last(), bundle)) {
                merged += bundle.locations.toMutableList()
            } else {
                current += bundle.locations
            }
        }
        return merged.map(::buildBundle)
    }

    private fun buildBundle(locations: List<RecordedTelemetrySessionLocation>): RecordedTelemetrySessionBundle {
        val ordered = locations.sortedBy { it.metadata.startedAtMs }
        return RecordedTelemetrySessionBundle(
            sessionId = stableBundleSessionId(ordered),
            metadata = ordered.last().metadata,
            locations = ordered,
        )
    }

    private fun canMergeIntoSameBundle(
        previous: RecordedTelemetrySessionLocation,
        next: RecordedTelemetrySessionLocation,
    ): Boolean {
        if (!sameBundleIdentity(previous, next)) return false
        val previousEnd = previous.metadata.endedAtMs ?: previous.metadata.startedAtMs
        val gapMs = next.metadata.startedAtMs - previousEnd
        return gapMs in 0..SESSION_BUNDLE_GAP_MAX_MS
    }

    private fun shouldMergeSplitWeekend(
        previous: RecordedTelemetrySessionLocation,
        nextBundle: RecordedTelemetrySessionBundle,
    ): Boolean {
        val next = nextBundle.locations.firstOrNull() ?: return false
        if (!sameBundleIdentity(previous, next)) return false

        val previousEnd = previous.metadata.endedAtMs ?: previous.metadata.startedAtMs
        val gapMs = next.metadata.startedAtMs - previousEnd
        if (gapMs !in 0..SESSION_BUNDLE_GAP_MAX_MS) return false

        val previousType = normalizeSessionType(previous.metadata.sessionType)
        val nextType = normalizeSessionType(next.metadata.sessionType)
        if (previousType.isBlank() || previousType != nextType) return false

        return next.metadata.frameCount <= DETAIL_PLACEHOLDER_MAX_FRAMES
    }

    private fun sameBundleIdentity(
        first: RecordedTelemetrySessionLocation,
        second: RecordedTelemetrySessionLocation,
    ): Boolean = normalizeGameId(first.metadata.gameId) == normalizeGameId(second.metadata.gameId) &&
        normalizeLabel(first.metadata.trackId) == normalizeLabel(second.metadata.trackId) &&
        normalizeLabel(first.metadata.layoutId) == normalizeLabel(second.metadata.layoutId) &&
        normalizeCarId(first.metadata.carId, first.metadata.carModel) ==
        normalizeCarId(second.metadata.carId, second.metadata.carModel)

    private fun stableBundleSessionId(locations: List<RecordedTelemetrySessionLocation>): Long {
        val source = buildString(locations.size * 64) {
            locations.forEach { location ->
                append(location.persistedSessionId)
                append('|')
                append(location.metadata.startedAtMs)
                append('|')
                append(location.dir.absolutePath)
                append('\n')
            }
        }
        val hash = fnv1a64(source)
        return (hash and Long.MAX_VALUE).let { if (it == 0L) 1L else it }
    }

    private fun normalizeGameId(gameId: String?): String = gameId.orEmpty().trim().lowercase()

    private fun normalizeLabel(value: String?): String = value.orEmpty().trim().lowercase()

    private fun normalizeSessionType(value: String?): String = value.orEmpty().trim().uppercase()

    private fun normalizeCarId(carId: Int?, carModel: String?): String = carId
        ?.takeIf { it > 0 }
        ?.toString()
        ?: normalizeLabel(carModel)

    private fun fnv1a64(value: String): Long {
        var hash = FNV1A_64_OFFSET
        value.forEach { ch ->
            hash = hash xor ch.code.toLong()
            hash *= FNV1A_64_PRIME
        }
        return hash
    }

    private companion object {

        private const val DETAIL_PLACEHOLDER_MAX_FRAMES: Long = 5L
        private const val SESSION_BUNDLE_GAP_MAX_MS: Long = 20 * 60 * 1000L
        private const val FNV1A_64_OFFSET: Long = -0x340d631b7bdddcdbL
        private const val FNV1A_64_PRIME: Long = 0x100000001b3L
    }
}
