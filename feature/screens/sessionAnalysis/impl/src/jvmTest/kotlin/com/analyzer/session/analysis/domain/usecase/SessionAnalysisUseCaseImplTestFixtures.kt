package com.analyzer.session.analysis.domain.usecase

import com.analyzer.session.analysis.domain.repository.SessionAnalysisRepository
import com.analyzer.session.analysis.domain.trackmap.SessionAnalysisTrackMapGeometry
import com.analyzer.session.analysis.domain.trackmap.SessionAnalysisTrackMapMerger
import com.analyzer.session.analysis.domain.trackmap.TrackMapSessionAnalysisMapper
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibrationSource
import com.project.analyzer.telemetry.ac.api.model.calibration.Vec2Dto
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMap
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapPoint
import com.project.analyzer.telemetry.analysis.api.model.header.SessionAnalysisHeader
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisCornerZone
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisReport
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import com.project.analyzer.telemetry.analysis.api.model.vehicle.SessionAnalysisVehicleClass
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionMetadata
import kotlinx.coroutines.Dispatchers

internal fun buildUseCase(repository: SessionAnalysisRepository): SessionAnalysisUseCaseImpl =
    SessionAnalysisUseCaseImpl(
        repository = repository,
        trackMapMapper = TrackMapSessionAnalysisMapper(),
        trackMapMerger = SessionAnalysisTrackMapMerger(SessionAnalysisTrackMapGeometry()),
        ioDispatcher = Dispatchers.IO,
    )

internal fun sessionReport(
    trackMap: SessionAnalysisTrackMap?,
    cornerZonesBySegmentId: Map<Long, List<SessionAnalysisCornerZone>> = emptyMap(),
): SessionAnalysisReport = SessionAnalysisReport(
    sessionId = 1L,
    header = SessionAnalysisHeader(),
    vehicleClass = SessionAnalysisVehicleClass.Unknown,
    trackMap = trackMap,
    cornerZonesBySegmentId = cornerZonesBySegmentId,
)

internal fun sessionTrackMap(points: List<SessionAnalysisTrackMapPoint>): SessionAnalysisTrackMap =
    SessionAnalysisTrackMap(
        points = points,
        minX = points.minOf(SessionAnalysisTrackMapPoint::x),
        minY = points.minOf(SessionAnalysisTrackMapPoint::y),
        maxX = points.maxOf(SessionAnalysisTrackMapPoint::x),
        maxY = points.maxOf(SessionAnalysisTrackMapPoint::y),
    )

internal fun twoPointSessionTrackMap(): SessionAnalysisTrackMap = sessionTrackMap(
    points = listOf(
        sessionTrackPoint(x = 0f, y = 0f),
        sessionTrackPoint(x = 10f, y = 0f),
    ),
)

internal fun sessionTrackPoint(
    x: Float,
    y: Float,
    leftWidth: Float? = null,
    rightWidth: Float? = null,
): SessionAnalysisTrackMapPoint = SessionAnalysisTrackMapPoint(
    x = x,
    y = y,
    leftWidthMeters = leftWidth,
    rightWidthMeters = rightWidth,
)

internal fun rawTrackMap(points: List<TrackMapPoint>): TrackMap = TrackMap(
    trackId = "test_track",
    trackName = "Test Track",
    createdAtEpochMs = 1L,
    points = points,
)

internal fun twoPointRawTrackMap(): TrackMap = rawTrackMap(
    points = listOf(
        rawTrackPoint(x = 0f, y = 0f, leftWidth = 7f, rightWidth = 7f),
        rawTrackPoint(x = 10f, y = 1f, leftWidth = 7f, rightWidth = 7f),
    ),
)

internal fun rawTrackPoint(x: Float, y: Float, leftWidth: Float = 0f, rightWidth: Float = 0f): TrackMapPoint =
    TrackMapPoint(
        x = x,
        y = y,
        leftWidthMeters = leftWidth,
        rightWidthMeters = rightWidth,
    )

internal fun sessionMetadata(trackId: String): RecordedTelemetrySessionMetadata = RecordedTelemetrySessionMetadata(
    sessionId = 1L,
    gameId = "acevo",
    sessionType = "Practice",
    carModel = "car",
    trackId = trackId,
    startedAtMs = 1L,
    endedAtMs = null,
    dataSource = "test",
    payloadType = "payload",
    payloadSize = 1,
    samplingRateHz = 60,
    frameCount = 1L,
    receivedFrames = 1L,
    droppedFrames = 0L,
    firstTimestampNs = null,
    lastTimestampNs = null,
    fileVersion = 1,
    indexVersion = 1,
    indexRecordSize = 1,
    indexFields = emptyList(),
    framesFile = "frames",
    indexFile = "index",
    eventsFile = "events",
)

internal fun trackCalibration(source: TrackCalibrationSource): TrackCalibration = TrackCalibration(
    trackId = "mount_panorama",
    trackName = "Mount Panorama",
    createdAtEpochMs = 1L,
    source = source,
    startFinish = gate(),
    sectors = emptyList(),
)

private fun gate(): Gate = Gate(
    center = Vec2Dto(x = 0f, y = 0f),
    forward = Vec2Dto(x = 1f, y = 0f),
    normal = Vec2Dto(x = 0f, y = 1f),
)

internal fun cornerZone(cornerNumber: Int): SessionAnalysisCornerZone {
    val start = (cornerNumber - 1) * 0.1f
    return SessionAnalysisCornerZone(
        cornerNumber = cornerNumber,
        startTrackPosition = start,
        apexTrackPosition = start + 0.04f,
        endTrackPosition = start + 0.08f,
        peakCurvature = 0.01f,
    )
}

internal fun cornerZones(count: Int): List<SessionAnalysisCornerZone> =
    (1..count).map { cornerNumber ->
        val start = ((cornerNumber - 1) % CornerZoneLayoutSlots) * CornerZoneLayoutStep
        SessionAnalysisCornerZone(
            cornerNumber = cornerNumber,
            startTrackPosition = start,
            apexTrackPosition = start + CornerZoneLayoutStep * 0.5f,
            endTrackPosition = start + CornerZoneLayoutStep * 0.8f,
            peakCurvature = 0.01f,
        )
    }

private const val CornerZoneLayoutSlots: Int = 20
private const val CornerZoneLayoutStep: Float = 0.045f


