package com.analyzer.session.analysis.domain.usecase

import com.analyzer.session.analysis.domain.model.SessionAnalysisWorkspaceRequest
import com.analyzer.session.analysis.domain.repository.SessionAnalysisRepository
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibrationSource
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMap
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisCornerZone
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisReport
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionMetadata
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionAnalysisUseCaseImplTest {

    @Test
    fun `loadWorkspaceShell keeps telemetry map as source and merges imported widths into display`() = runBlocking {
        val reportTrackMap = sessionTrackMap(
            points = listOf(
                sessionTrackPoint(x = 0f, y = 0f),
                sessionTrackPoint(x = 10f, y = 10f),
            ),
        )
        val repository = FakeSessionAnalysisRepository(
            shellReport = sessionReport(trackMap = reportTrackMap),
            metadata = sessionMetadata(trackId = "suzuka_gp"),
            importedTrackMap = rawTrackMap(
                points = listOf(
                    rawTrackPoint(x = 0f, y = 0f, leftWidth = 6f, rightWidth = 7f),
                    rawTrackPoint(x = 10f, y = 10f, leftWidth = 5f, rightWidth = 8f),
                ),
            ),
        )

        val workspace = buildUseCase(repository).loadWorkspaceShell(
            request = SessionAnalysisWorkspaceRequest(sessionId = 7L),
        )

        assertNotNull(workspace)
        assertEquals(reportTrackMap, workspace.sourceTrackMap)
        assertNotNull(workspace.displayTrackMap)
        assertEquals(6f, workspace.displayTrackMap.points.first().leftWidthMeters)
        assertEquals(7f, workspace.displayTrackMap.points.first().rightWidthMeters)
        assertEquals(5f, workspace.displayTrackMap.points.last().leftWidthMeters)
        assertEquals(8f, workspace.displayTrackMap.points.last().rightWidthMeters)
    }

    @Test
    fun `loadWorkspaceShell falls back to generic calibration when game calibration is missing`() = runBlocking {
        val genericCalibration = trackCalibration(source = TrackCalibrationSource.USER)
        val repository = FakeSessionAnalysisRepository(
            shellReport = sessionReport(trackMap = null),
            metadata = sessionMetadata(trackId = "mount_panorama"),
            gameCalibration = null,
            calibration = genericCalibration,
        )

        val workspace = buildUseCase(repository).loadWorkspaceShell(
            request = SessionAnalysisWorkspaceRequest(sessionId = 11L),
        )

        assertNotNull(workspace)
        assertEquals(genericCalibration, workspace.calibration)
        assertTrue(repository.gameCalibrationRequested)
        assertTrue(repository.calibrationRequested)
        assertNull(workspace.sourceTrackMap)
        assertNull(workspace.displayTrackMap)
    }

    @Test
    fun `loadWorkspaceShell prefers imported corner zones when they are more detailed`() = runBlocking {
        val reportTrackMap = sessionTrackMap(
            points = listOf(
                sessionTrackPoint(x = 0f, y = 0f),
                sessionTrackPoint(x = 10f, y = 0f),
            ),
        )
        val reportCornerZones = (1..4).map(::cornerZone)
        val importedCornerZones = (1..7).map(::cornerZone)
        val repository = FakeSessionAnalysisRepository(
            shellReport = sessionReport(
                trackMap = reportTrackMap,
                cornerZonesBySegmentId = mapOf(1L to reportCornerZones),
            ),
            metadata = sessionMetadata(trackId = "brands_hatch_indy"),
            importedTrackMap = rawTrackMap(
                points = listOf(
                    rawTrackPoint(x = 0f, y = 0f, leftWidth = 7f, rightWidth = 7f),
                    rawTrackPoint(x = 10f, y = 1f, leftWidth = 7f, rightWidth = 7f),
                ),
            ),
            detectedCornerZones = importedCornerZones,
        )

        val workspace = buildUseCase(repository).loadWorkspaceShell(
            request = SessionAnalysisWorkspaceRequest(sessionId = 1L),
        )

        assertNotNull(workspace)
        assertEquals(importedCornerZones, workspace.report.cornerZonesBySegmentId[1L])
        assertEquals(reportTrackMap, workspace.sourceTrackMap)
    }

    @Test
    fun `loadWorkspaceShell ignores imported corner zones when detection is suspiciously inflated`() = runBlocking {
        val reportCornerZones = cornerZones(count = 12)
        val importedCornerZones = cornerZones(count = 100)
        val repository = FakeSessionAnalysisRepository(
            shellReport = sessionReport(
                trackMap = twoPointSessionTrackMap(),
                cornerZonesBySegmentId = mapOf(1L to reportCornerZones),
            ),
            metadata = sessionMetadata(trackId = "road_atlanta_gp"),
            importedTrackMap = twoPointRawTrackMap(),
            detectedCornerZones = importedCornerZones,
        )

        val workspace = buildUseCase(repository).loadWorkspaceShell(
            request = SessionAnalysisWorkspaceRequest(sessionId = 1L),
        )

        assertNotNull(workspace)
        assertEquals(reportCornerZones, workspace.report.cornerZonesBySegmentId[1L])
    }

    @Test
    fun `loadWorkspaceShell replaces inflated telemetry corner zones with plausible imported zones`() = runBlocking {
        val reportCornerZones = cornerZones(count = 100)
        val importedCornerZones = cornerZones(count = 12)
        val repository = FakeSessionAnalysisRepository(
            shellReport = sessionReport(
                trackMap = twoPointSessionTrackMap(),
                cornerZonesBySegmentId = mapOf(1L to reportCornerZones),
            ),
            metadata = sessionMetadata(trackId = "road_atlanta_gp"),
            importedTrackMap = twoPointRawTrackMap(),
            detectedCornerZones = importedCornerZones,
        )

        val workspace = buildUseCase(repository).loadWorkspaceShell(
            request = SessionAnalysisWorkspaceRequest(sessionId = 1L),
        )

        assertNotNull(workspace)
        assertEquals(importedCornerZones, workspace.report.cornerZonesBySegmentId[1L])
    }

    @Test
    fun `loadWorkspaceShell keeps shell data when track map loading fails`() = runBlocking {
        val reportTrackMap = twoPointSessionTrackMap()
        val repository = FakeSessionAnalysisRepository(
            shellReport = sessionReport(trackMap = reportTrackMap),
            metadata = sessionMetadata(trackId = "spa"),
            trackMapError = IllegalStateException("track map unavailable"),
        )

        val workspace = buildUseCase(repository).loadWorkspaceShell(
            request = SessionAnalysisWorkspaceRequest(sessionId = 42L),
        )

        assertNotNull(workspace)
        assertNull(workspace.authoredTrackMap)
        assertEquals(reportTrackMap, workspace.sourceTrackMap)
        assertEquals(reportTrackMap, workspace.displayTrackMap)
    }

    @Test
    fun `loadWorkspaceShell keeps shell data when calibration loading fails`() = runBlocking {
        val repository = FakeSessionAnalysisRepository(
            shellReport = sessionReport(trackMap = twoPointSessionTrackMap()),
            metadata = sessionMetadata(trackId = "monza"),
            calibrationError = IllegalStateException("calibration unavailable"),
        )

        val workspace = buildUseCase(repository).loadWorkspaceShell(
            request = SessionAnalysisWorkspaceRequest(sessionId = 43L),
        )

        assertNotNull(workspace)
        assertNull(workspace.calibration)
        assertTrue(repository.gameCalibrationRequested)
    }

    @Test
    fun `loadWorkspaceShell includes external reference report when requested`() = runBlocking {
        val referenceReport = sessionReport(sessionId = 99L, trackMap = twoPointSessionTrackMap())
        val repository = FakeSessionAnalysisRepository(
            shellReport = sessionReport(sessionId = 42L, trackMap = twoPointSessionTrackMap()),
            metadata = sessionMetadata(trackId = "spa"),
            shellReports = mapOf(99L to referenceReport),
        )

        val workspace = buildUseCase(repository).loadWorkspaceShell(
            request = SessionAnalysisWorkspaceRequest(
                sessionId = 42L,
                referenceSessionId = 99L,
            ),
        )

        assertNotNull(workspace)
        assertEquals(referenceReport, workspace.referenceReport)
    }

    private class FakeSessionAnalysisRepository(
        private val shellReport: SessionAnalysisReport?,
        private val metadata: RecordedTelemetrySessionMetadata?,
        private val shellReports: Map<Long, SessionAnalysisReport?> = emptyMap(),
        private val importedTrackMap: TrackMap? = null,
        private val gameCalibration: TrackCalibration? = null,
        private val calibration: TrackCalibration? = null,
        private val detectedCornerZones: List<SessionAnalysisCornerZone> = emptyList(),
        private val trackMapError: Throwable? = null,
        private val calibrationError: Throwable? = null,
    ) : SessionAnalysisRepository {

        var gameCalibrationRequested: Boolean = false
            private set

        var calibrationRequested: Boolean = false
            private set

        override suspend fun loadSessionShellReport(sessionId: Long, forceRefresh: Boolean): SessionAnalysisReport? =
            shellReports[sessionId] ?: shellReport

        override suspend fun enrichSessionReport(report: SessionAnalysisReport): SessionAnalysisReport = report

        override suspend fun detectCornerZones(trackMap: SessionAnalysisTrackMap): List<SessionAnalysisCornerZone> =
            detectedCornerZones

        override suspend fun loadSessionMetadata(
            sessionId: Long,
            forceRefresh: Boolean,
        ): RecordedTelemetrySessionMetadata? = metadata

        override suspend fun loadTrackMap(gameId: String, trackId: String, layoutId: String?): TrackMap? {
            trackMapError?.let { throw it }
            return importedTrackMap
        }

        override suspend fun loadGameCalibration(trackId: String, layoutId: String?): TrackCalibration? {
            gameCalibrationRequested = true
            calibrationError?.let { throw it }
            return gameCalibration
        }

        override suspend fun loadCalibration(trackId: String, layoutId: String?): TrackCalibration? {
            calibrationRequested = true
            return calibration
        }
    }
}

