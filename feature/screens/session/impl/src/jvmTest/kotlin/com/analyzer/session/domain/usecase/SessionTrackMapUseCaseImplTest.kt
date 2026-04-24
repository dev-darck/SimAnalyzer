package com.analyzer.session.domain.usecase

import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMap
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapPoint
import com.project.analyzer.telemetry.ac.api.trackmap.TrackMapRepository
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisCornerZone
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisReport
import com.project.analyzer.telemetry.analysis.api.model.header.SessionAnalysisHeader
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import com.project.analyzer.telemetry.analysis.api.model.vehicle.SessionAnalysisVehicleClass
import com.project.analyzer.telemetry.analysis.api.service.RecordedTelemetryAnalysisService
import com.project.analyzer.utils.trackmap.TrackMapPreparationUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SessionTrackMapUseCaseImplTest {

    private val trackMapPreparationUtil = TrackMapPreparationUtil()

    @Test
    fun `loadTrackMaps falls back to recorded session track map when repository misses`() = runBlocking {
        val analysisService = FakeRecordedTelemetryAnalysisService(
            reportsBySessionId = mapOf(
                42L to sessionReport(
                    sessionId = 42L,
                    trackMap = sessionTrackMap(),
                ),
            ),
        )
        val useCase = SessionTrackMapUseCaseImpl(
            trackMapRepository = FakeTrackMapRepository(),
            analysisService = analysisService,
            trackMapPreparationUtil = trackMapPreparationUtil,
            ioDispatcher = Dispatchers.Unconfined,
        )

        val result = useCase.loadTrackMaps(
            items = listOf(
                SessionTrackMapIdentity(
                    sessionId = 42L,
                    gameId = "LMU",
                    trackId = "Circuit de la Sarthe",
                ),
            ),
        )

        val trackMap = result["lmu|circuit de la sarthe"]
        assertNotNull(trackMap)
        assertEquals(3, trackMap.points.size)
        assertEquals(listOf(42L), analysisService.requestedSessionIds)
    }

    @Test
    fun `loadTrackMaps tries next session when first matching session has no report map`() = runBlocking {
        val analysisService = FakeRecordedTelemetryAnalysisService(
            reportsBySessionId = mapOf(
                100L to sessionReport(sessionId = 100L, trackMap = null),
                101L to sessionReport(sessionId = 101L, trackMap = sessionTrackMap()),
            ),
        )
        val useCase = SessionTrackMapUseCaseImpl(
            trackMapRepository = FakeTrackMapRepository(),
            analysisService = analysisService,
            trackMapPreparationUtil = trackMapPreparationUtil,
            ioDispatcher = Dispatchers.Unconfined,
        )

        val result = useCase.loadTrackMaps(
            items = listOf(
                SessionTrackMapIdentity(
                    sessionId = 100L,
                    gameId = "ace",
                    trackId = "sebring",
                ),
                SessionTrackMapIdentity(
                    sessionId = 101L,
                    gameId = "ace",
                    trackId = "sebring",
                ),
            ),
        )

        val trackMap = result["ace|sebring"]
        assertNotNull(trackMap)
        assertEquals(listOf(100L, 101L), analysisService.requestedSessionIds)
    }

    @Test
    fun `loadTrackMaps keeps repository map as preferred source`() = runBlocking {
        val repositoryTrackMap = TrackMap(
            gameId = "acc",
            trackId = "monza",
            trackName = "Monza",
            layoutId = null,
            createdAtEpochMs = 1L,
            points = listOf(
                TrackMapPoint(x = 0f, y = 0f),
                TrackMapPoint(x = 10f, y = 0f),
                TrackMapPoint(x = 10f, y = 10f),
            ),
        )
        val analysisService = FakeRecordedTelemetryAnalysisService(
            reportsBySessionId = mapOf(
                7L to sessionReport(
                    sessionId = 7L,
                    trackMap = sessionTrackMap(),
                ),
            ),
        )
        val useCase = SessionTrackMapUseCaseImpl(
            trackMapRepository = FakeTrackMapRepository(
                trackMaps = mapOf("acc|monza" to repositoryTrackMap),
            ),
            analysisService = analysisService,
            trackMapPreparationUtil = trackMapPreparationUtil,
            ioDispatcher = Dispatchers.Unconfined,
        )

        val result = useCase.loadTrackMaps(
            items = listOf(
                SessionTrackMapIdentity(
                    sessionId = 7L,
                    gameId = "acc",
                    trackId = "monza",
                ),
            ),
        )

        val trackMap = result["acc|monza"]
        assertNotNull(trackMap)
        assertEquals(3, trackMap.points.size)
        assertNull(analysisService.requestedSessionIds.firstOrNull())
    }

    private fun sessionTrackMap(): SessionAnalysisTrackMap = SessionAnalysisTrackMap(
        points = listOf(
            SessionAnalysisTrackMapPoint(x = -100f, y = -100f),
            SessionAnalysisTrackMapPoint(x = 100f, y = 100f),
        ),
        idealPoints = listOf(
            SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 5f, rightWidthMeters = 6f),
            SessionAnalysisTrackMapPoint(x = 25f, y = 10f, leftWidthMeters = 4f, rightWidthMeters = 7f),
            SessionAnalysisTrackMapPoint(x = 50f, y = 0f, leftWidthMeters = 5f, rightWidthMeters = 6f),
        ),
        minX = 0f,
        minY = 0f,
        maxX = 50f,
        maxY = 10f,
    )

    private fun sessionReport(sessionId: Long, trackMap: SessionAnalysisTrackMap?): SessionAnalysisReport =
        SessionAnalysisReport(
            sessionId = sessionId,
            header = SessionAnalysisHeader(
                gameId = "lmu",
                sessionTypeLabel = "Practice",
                carLabel = "Car",
                trackLabel = "Track",
                trackLayoutLabel = "",
                startedAtMs = 1L,
            ),
            vehicleClass = SessionAnalysisVehicleClass.GT3,
            trackMap = trackMap,
        )

    private class FakeTrackMapRepository(
        private val trackMaps: Map<String, TrackMap> = emptyMap(),
    ) : TrackMapRepository {

        override suspend fun save(trackMap: TrackMap) = Unit

        override suspend fun load(gameId: String, trackId: String, layoutId: String?): TrackMap? =
            trackMaps[TrackMapPreparationUtil().key(gameId, trackId, layoutId)]

        override suspend fun loadAll(gameId: String?): List<TrackMap> = trackMaps.values.toList()
    }

    private class FakeRecordedTelemetryAnalysisService(
        private val reportsBySessionId: Map<Long, SessionAnalysisReport?>,
    ) : RecordedTelemetryAnalysisService {

        val requestedSessionIds = mutableListOf<Long>()

        override suspend fun loadSessionReportShell(
            sessionId: Long,
            includeTrackMap: Boolean,
            forceRefresh: Boolean,
        ): SessionAnalysisReport? {
            requestedSessionIds += sessionId
            return reportsBySessionId[sessionId]
        }

        override suspend fun enrichSessionReport(report: SessionAnalysisReport): SessionAnalysisReport = report

        override fun detectCornerZones(trackMap: SessionAnalysisTrackMap): List<SessionAnalysisCornerZone> = emptyList()
    }
}
