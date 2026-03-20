package com.analyzer.session.data.repository

import com.analyzer.session.data.model.RecordedSessionSummary
import com.analyzer.session.data.model.SessionBundleLocation
import com.analyzer.session.data.model.SessionLocation
import dev.zacsweers.metro.Inject

@Inject
internal class RecordedSessionBundleAssembler {

    fun build(locations: List<SessionLocation>): List<SessionBundleLocation> {
        if (locations.isEmpty()) return emptyList()

        val sorted = locations.sortedBy { it.summary.startedAtMs }
        val explicitByGroupId = linkedMapOf<String, MutableList<SessionLocation>>()
        val legacyLocations = mutableListOf<SessionLocation>()

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

        val explicitBundles = explicitByGroupId.values.flatMap(::buildExplicitSessionBundles)
        val legacyBundles = buildLegacySessionBundles(legacyLocations)
        return mergeSplitWeekendBundles(explicitBundles + legacyBundles)
            .sortedByDescending { it.summary.startedAtMs }
    }

    private fun buildExplicitSessionBundles(locations: List<SessionLocation>): List<SessionBundleLocation> {
        if (locations.isEmpty()) return emptyList()

        val grouped = mutableListOf<MutableList<SessionLocation>>()
        locations.sortedBy { it.summary.startedAtMs }.forEach { location ->
            val current = grouped.lastOrNull()
            if (current == null || !sameBundleIdentity(current.last(), location)) {
                grouped += mutableListOf(location)
            } else {
                current += location
            }
        }
        return grouped.map(::buildSessionBundle)
    }

    private fun mergeSplitWeekendBundles(bundles: List<SessionBundleLocation>): List<SessionBundleLocation> {
        if (bundles.isEmpty()) return emptyList()

        val merged = mutableListOf<MutableList<SessionLocation>>()
        bundles.sortedBy { it.summary.startedAtMs }.forEach { bundle ->
            val current = merged.lastOrNull()
            if (current == null || !shouldMergeSplitWeekend(current.last(), bundle)) {
                merged += bundle.locations.toMutableList()
            } else {
                current += bundle.locations
            }
        }
        return merged.map(::buildSessionBundle)
    }

    private fun buildLegacySessionBundles(locations: List<SessionLocation>): List<SessionBundleLocation> {
        if (locations.isEmpty()) return emptyList()

        val grouped = mutableListOf<MutableList<SessionLocation>>()
        locations.sortedBy { it.summary.startedAtMs }.forEach { location ->
            val current = grouped.lastOrNull()
            if (current == null || !canMergeIntoSameBundle(current.last(), location)) {
                grouped += mutableListOf(location)
            } else {
                current += location
            }
        }
        return grouped.map(::buildSessionBundle)
    }

    private fun buildSessionBundle(locations: List<SessionLocation>): SessionBundleLocation {
        val ordered = locations.sortedBy { it.summary.startedAtMs }
        return SessionBundleLocation(
            summary = buildBundleSummary(ordered),
            locations = ordered,
        )
    }

    private fun canMergeIntoSameBundle(previous: SessionLocation, next: SessionLocation): Boolean {
        if (!sameBundleIdentity(previous, next)) return false

        val previousEnd = previous.summary.endedAtMs ?: previous.summary.startedAtMs
        val gapMs = next.summary.startedAtMs - previousEnd
        return gapMs in 0..SESSION_BUNDLE_GAP_MAX_MS
    }

    private fun shouldMergeSplitWeekend(previous: SessionLocation, nextBundle: SessionBundleLocation): Boolean {
        val next = nextBundle.locations.firstOrNull() ?: return false
        if (!sameBundleIdentity(previous, next)) return false

        val previousEnd = previous.summary.endedAtMs ?: previous.summary.startedAtMs
        val gapMs = next.summary.startedAtMs - previousEnd
        if (gapMs !in 0..SESSION_BUNDLE_GAP_MAX_MS) return false

        val previousType = normalizeSessionType(previous.metadata.sessionType)
        val nextType = normalizeSessionType(next.metadata.sessionType)
        if (previousType.isBlank() || previousType != nextType) return false

        return isBoundaryPlaceholder(next, next.analysis)
    }

    private fun sameBundleIdentity(a: SessionLocation, b: SessionLocation): Boolean =
        normalizeGameId(a.summary.gameId) == normalizeGameId(b.summary.gameId) &&
            normalizeBundleLabel(a.summary.trackId) == normalizeBundleLabel(b.summary.trackId) &&
            normalizeBundleLabel(a.summary.layoutId) == normalizeBundleLabel(b.summary.layoutId) &&
            normalizeBundleCarId(a.summary.carId, a.summary.carModel) ==
            normalizeBundleCarId(b.summary.carId, b.summary.carModel)

    private fun buildBundleSummary(locations: List<SessionLocation>): RecordedSessionSummary {
        val first = locations.first()
        val latest = locations.maxByOrNull { it.summary.startedAtMs } ?: first
        val bundleId = stableBundleSessionId(locations)
        val bestLap = locations.asSequence().mapNotNull { it.summary.bestLapTimeMs }.minOrNull()
        val endedAt = locations.asSequence().mapNotNull { it.summary.endedAtMs }.maxOrNull()
        val carName = locations.asSequence()
            .mapNotNull { it.summary.carName?.takeIf(String::isNotBlank) }
            .lastOrNull()
        val trackName = locations.asSequence()
            .mapNotNull { it.summary.trackName?.takeIf(String::isNotBlank) }
            .lastOrNull()

        return RecordedSessionSummary(
            sessionId = bundleId,
            startedAtMs = first.summary.startedAtMs,
            endedAtMs = endedAt ?: latest.summary.endedAtMs,
            gameId = latest.summary.gameId,
            sessionType = latest.summary.sessionType,
            carModel = latest.summary.carModel,
            carName = carName,
            carId = latest.summary.carId,
            trackId = latest.summary.trackId,
            trackName = trackName,
            layoutId = latest.summary.layoutId,
            lapCount = locations.sumOf { it.summary.lapCount },
            bestLapTimeMs = bestLap,
            totalIncidents = locations.sumOf { it.summary.totalIncidents },
            distanceKm = locations.sumOf { it.summary.distanceKm },
            isSaved = locations.all { it.summary.isSaved },
            airTempC = latest.summary.airTempC,
            trackTempC = latest.summary.trackTempC,
        )
    }

    private fun stableBundleSessionId(locations: List<SessionLocation>): Long {
        val source = buildString(locations.size * 64) {
            locations.forEach { location ->
                append(location.summary.sessionId)
                append('|')
                append(location.summary.startedAtMs)
                append('|')
                append(location.dir.absolutePath)
                append('\n')
            }
        }
        val hash = fnv1a64(source)
        return (hash and Long.MAX_VALUE).let { if (it == 0L) 1L else it }
    }

    private fun normalizeGameId(gameId: String?): String = gameId.orEmpty().trim().lowercase()

    private fun normalizeBundleLabel(value: String?): String = value.orEmpty().trim().lowercase()

    private fun normalizeSessionType(value: String?): String = value.orEmpty().trim().uppercase()

    private fun normalizeBundleCarId(carId: Int?, carModel: String?): String = carId
        ?.takeIf { it > 0 }
        ?.toString()
        ?: normalizeBundleLabel(carModel)

    private fun fnv1a64(value: String): Long {
        var hash = -0x340d631b7bdddcdbL
        value.forEach { ch ->
            hash = hash xor ch.code.toLong()
            hash *= 0x100000001b3L
        }
        return hash
    }
}
