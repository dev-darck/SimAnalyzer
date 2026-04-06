package com.analyzer.session.data.repository

import com.analyzer.session.data.analysis.IndexAnalysis
import com.analyzer.session.data.model.RecordedSessionSummary
import com.analyzer.session.data.model.SessionBundleLocation
import com.analyzer.session.data.model.SessionLocation
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionBundle
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionLocation
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionStorage
import dev.zacsweers.metro.Inject

@Inject
internal class RecordedSessionBundleStore(
    private val storage: RecordedTelemetrySessionStorage,
    private val indexReader: RecordedSessionIndexReader,
) {

    private var cachedSummaries: List<RecordedSessionSummary>? = null

    suspend fun loadSummaries(forceRefresh: Boolean = false): List<RecordedSessionSummary> {
        if (!forceRefresh) {
            cachedSummaries?.let { return it }
        }
        val summaries = storage
            .loadBundles(forceRefresh = forceRefresh)
            .map(::buildBundleSummary)
        cachedSummaries = summaries
        return summaries
    }

    suspend fun findBundle(sessionId: Long, forceRefresh: Boolean = false): SessionBundleLocation? {
        val bundle = storage.findBundle(
            sessionId = sessionId,
            forceRefresh = forceRefresh,
        ) ?: return null
        return mapBundle(bundle)
    }

    suspend fun saveSession(sessionId: Long): Boolean = storage.saveSession(sessionId)
        .also { saved ->
            if (saved) {
                invalidateCaches()
            }
        }

    suspend fun deleteSession(sessionId: Long): Boolean = storage.deleteSession(sessionId)
        .also { deleted ->
            if (deleted) {
                invalidateCaches()
            }
        }

    fun resolveAnalysis(location: SessionLocation): IndexAnalysis? = location.analysis
        ?: indexReader.readAnalysis(location.source)

    private fun mapBundle(bundle: RecordedTelemetrySessionBundle): SessionBundleLocation {
        val locations = bundle.locations.map(::mapLocation)
        return SessionBundleLocation(
            summary = buildSummary(
                locations = locations,
                persistedSessionId = bundle.sessionId,
            ),
            locations = locations,
        )
    }

    private fun mapLocation(location: RecordedTelemetrySessionLocation): SessionLocation {
        val analysis = indexReader.readAnalysis(location)
        return SessionLocation(
            source = location,
            summary = buildSummary(
                locations = emptyList(),
                persistedSessionId = location.persistedSessionId,
                singleLocation = location,
                analysis = analysis,
            ),
            analysis = analysis,
        )
    }

    private fun buildBundleSummary(bundle: RecordedTelemetrySessionBundle): RecordedSessionSummary = buildSummary(
        locations = bundle.locations.map(::mapLocation),
        persistedSessionId = bundle.sessionId,
    )

    private fun buildSummary(
        locations: List<SessionLocation>,
        persistedSessionId: Long,
        singleLocation: RecordedTelemetrySessionLocation? = null,
        analysis: IndexAnalysis? = null,
    ): RecordedSessionSummary {
        val primaryLocation = singleLocation ?: locations.lastOrNull()?.source ?: return RecordedSessionSummary(
            sessionId = persistedSessionId,
            startedAtMs = 0L,
            endedAtMs = null,
            gameId = "",
            sessionType = null,
            carModel = null,
            carName = null,
            carId = null,
            trackId = null,
            trackName = null,
            layoutId = null,
            lapCount = 0,
            bestLapTimeMs = null,
            totalIncidents = 0,
            distanceKm = 0.0,
            isSaved = false,
            airTempC = null,
            trackTempC = null,
        )
        val metadata = primaryLocation.metadata
        val resolvedAnalysis = analysis ?: locations.lastOrNull()?.analysis
        val laps = resolvedAnalysis?.laps.orEmpty()
        val completedLaps = laps.count { lap -> lap.complete }
        val bestLapMs = laps
            .asSequence()
            .filter { lap -> lap.complete && !lap.invalid && !lap.inPit }
            .mapNotNull { lap -> lap.totalTimeMs }
            .minOrNull()
        val incidents = laps.count { lap -> lap.invalid }
        val distanceKm = if (singleLocation == null) {
            locations.sumOf { it.analysis?.distanceKm ?: 0.0 }
        } else {
            resolvedAnalysis?.distanceKm ?: 0.0
        }
        val lapCount = if (singleLocation == null) {
            locations.sumOf { location -> location.analysis?.laps?.count { lap -> lap.complete } ?: 0 }
        } else {
            completedLaps
        }
        val allLocations = singleLocation?.let { listOf(it) } ?: locations.map(SessionLocation::source)
        val latestMetadata =
            allLocations.maxByOrNull { location -> location.metadata.startedAtMs }?.metadata ?: metadata
        val endedAtMs = allLocations.maxOfOrNull { location -> location.metadata.endedAtMs ?: Long.MIN_VALUE }
            ?.takeIf { it != Long.MIN_VALUE }
        val bestAcrossBundle = if (singleLocation == null) {
            locations.asSequence()
                .flatMap { location -> location.analysis?.laps.orEmpty().asSequence() }
                .filter { lap -> lap.complete && !lap.invalid && !lap.inPit }
                .mapNotNull { lap -> lap.totalTimeMs }
                .minOrNull()
        } else {
            bestLapMs
        }
        val bundleIncidents = if (singleLocation == null) {
            locations.sumOf { location -> location.analysis?.laps?.count { lap -> lap.invalid } ?: 0 }
        } else {
            incidents
        }

        return RecordedSessionSummary(
            sessionId = persistedSessionId,
            startedAtMs = allLocations.minOfOrNull { location -> location.metadata.startedAtMs }
                ?: metadata.startedAtMs,
            endedAtMs = endedAtMs ?: latestMetadata.endedAtMs,
            gameId = latestMetadata.gameId,
            sessionType = latestMetadata.sessionType,
            carModel = latestMetadata.carModel,
            carName = allLocations.asSequence()
                .mapNotNull { location -> location.metadata.carName?.takeIf(String::isNotBlank) }
                .lastOrNull(),
            carId = latestMetadata.carId,
            trackId = latestMetadata.trackId,
            trackName = allLocations.asSequence()
                .mapNotNull { location -> location.metadata.trackName?.takeIf(String::isNotBlank) }
                .lastOrNull(),
            layoutId = latestMetadata.layoutId,
            lapCount = lapCount,
            bestLapTimeMs = bestAcrossBundle,
            totalIncidents = bundleIncidents,
            distanceKm = distanceKm,
            isSaved = allLocations.all { location -> location.metadata.isSaved },
            airTempC = latestMetadata.airTempC,
            trackTempC = latestMetadata.trackTempC,
        )
    }

    private fun invalidateCaches() {
        cachedSummaries = null
    }
}
