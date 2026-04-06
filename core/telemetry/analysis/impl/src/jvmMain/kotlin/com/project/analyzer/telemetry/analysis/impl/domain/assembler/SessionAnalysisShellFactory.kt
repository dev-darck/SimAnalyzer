package com.project.analyzer.telemetry.analysis.impl.domain.assembler

import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisReport
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.segment.SessionAnalysisSegment
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.telemetry.analysis.impl.domain.assembler.mapper.SessionAnalysisSampleMapper
import com.project.analyzer.telemetry.analysis.impl.domain.assembler.mapper.toSessionAnalysisHeader
import com.project.analyzer.telemetry.analysis.impl.domain.assembler.mapper.toSessionAnalysisSegment
import com.project.analyzer.telemetry.analysis.impl.domain.extension.bestLapBySegmentId
import com.project.analyzer.telemetry.analysis.impl.domain.extension.toApiCornerZonesBySegmentId
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.lap.SessionAnalysisLapDeltaResolver
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.track.SessionAnalysisTrackPositionResolver
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.TrackMapCornerZone
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.TrackMapCornerZoneDetector
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.tyre.SessionAnalysisTyreProfileResolver
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.vehicle.SessionAnalysisVehicleClassResolver
import com.project.analyzer.telemetry.recording.api.session.DecodedRecordedTelemetrySegment
import com.project.analyzer.telemetry.recording.api.session.DecodedRecordedTelemetrySession
import dev.zacsweers.metro.Inject

/**
 * Builds the lightweight report needed to render the session screen before highlight analysis runs.
 */
@Inject
internal class SessionAnalysisShellFactory(
    private val vehicleClassResolver: SessionAnalysisVehicleClassResolver,
    private val tyreProfileResolver: SessionAnalysisTyreProfileResolver,
    private val sampleMapper: SessionAnalysisSampleMapper,
    private val trackMapAssembler: SessionAnalysisTrackMapAssembler,
    private val trackPositionResolver: SessionAnalysisTrackPositionResolver,
    private val lapAssembler: SessionAnalysisLapAssembler,
    private val lapDeltaResolver: SessionAnalysisLapDeltaResolver,
    private val trackMapCornerZoneDetector: TrackMapCornerZoneDetector,
) {

    fun createShell(data: DecodedRecordedTelemetrySession, includeTrackMap: Boolean = true): SessionAnalysisReport {
        val firstPayload = data.frames.firstOrNull()?.payload
        val compoundLabel = data.frames.firstNotNullOfOrNull { frame ->
            frame.payload.tyreCompoundLabel?.takeIf(String::isNotBlank)
        }
        val isRainTyres = data.frames.firstNotNullOfOrNull { frame ->
            frame.payload.isRainTyres
        }

        val vehicleClass = vehicleClassResolver.resolve(
            gameId = data.metadata.gameId,
            carModel = data.metadata.carModel ?: firstPayload?.carModel,
            carLabel = data.metadata.carName ?: firstPayload?.carLabel,
            vehicleClassHint = firstPayload?.vehicleClassHint,
        )
        val tyreProfile = tyreProfileResolver.resolve(
            vehicleClass = vehicleClass,
            tyreCompoundLabel = compoundLabel,
            isRainTyres = isRainTyres,
        )
        val segments = buildSegments(data)
        val sessionTypeBySegmentId = segments.associate { segment -> segment.segmentId to segment.sessionTypeLabel }
        val defaultSegmentId = segments.lastOrNull()?.segmentId

        val rawSamples = sampleMapper.map(
            frames = data.frames,
            tyreProfile = tyreProfile,
        )

        val initialTrackMap = if (includeTrackMap) {
            trackMapAssembler.build(
                samples = rawSamples,
                preferredSegmentId = defaultSegmentId,
                preferredBestLapNumber = null,
            )
        } else {
            SessionAnalysisTrackMapBuildResult.EMPTY
        }
        val trackMap = initialTrackMap.trackMap

        val samples = trackPositionResolver.enrich(rawSamples, trackMap?.points)
        val laps = lapAssembler.assemble(
            samples = samples,
            sessionTypeBySegmentId = sessionTypeBySegmentId,
        )
        val bestLapBySegmentId = laps.bestLapBySegmentId()
        val bestLapNumber = defaultSegmentId?.let(bestLapBySegmentId::get)
        val finalTrackMap = resolveFinalTrackMap(
            includeTrackMap = includeTrackMap,
            initialTrackMap = initialTrackMap,
            fallbackTrackMap = trackMap,
            samples = samples,
            preferredSegmentId = defaultSegmentId,
            preferredBestLapNumber = bestLapNumber,
        )
        val cornerZonesBySegmentId = resolveCornerZonesBySegmentId(
            segmentId = defaultSegmentId,
            trackMap = finalTrackMap,
        )
        val enrichedSamples = enrichSamplesWithLapDelta(
            samples = samples,
            bestLapBySegmentId = bestLapBySegmentId,
        )
        val enrichedLaps = lapAssembler.assemble(
            samples = enrichedSamples,
            sessionTypeBySegmentId = sessionTypeBySegmentId,
        )

        return SessionAnalysisReport(
            sessionId = data.sessionId,
            header = data.metadata.toSessionAnalysisHeader(firstPayload),
            vehicleClass = vehicleClass,
            tyreProfile = tyreProfile,
            trackMap = finalTrackMap,
            segments = segments,
            laps = enrichedLaps,
            samples = enrichedSamples,
            highlights = emptyList(),
            cornerZonesBySegmentId = cornerZonesBySegmentId.toApiCornerZonesBySegmentId(),
            bestLapNumber = bestLapNumber,
        )
    }

    private fun resolveFinalTrackMap(
        includeTrackMap: Boolean,
        initialTrackMap: SessionAnalysisTrackMapBuildResult,
        fallbackTrackMap: SessionAnalysisTrackMap?,
        samples: List<SessionAnalysisSample>,
        preferredSegmentId: Long?,
        preferredBestLapNumber: Int?,
    ): SessionAnalysisTrackMap? {
        if (!includeTrackMap) return fallbackTrackMap
        if (preferredBestLapNumber == null || preferredBestLapNumber == initialTrackMap.sourceLapNumber) {
            return fallbackTrackMap
        }
        return trackMapAssembler.build(
            samples = samples,
            preferredSegmentId = preferredSegmentId,
            preferredBestLapNumber = preferredBestLapNumber,
        ).trackMap ?: fallbackTrackMap
    }

    private fun enrichSamplesWithLapDelta(
        samples: List<SessionAnalysisSample>,
        bestLapBySegmentId: Map<Long, Int?>,
    ): List<SessionAnalysisSample> {
        val deltaByFrameId = lapDeltaResolver.resolve(
            samples = samples,
            bestLapBySegmentId = bestLapBySegmentId,
        )
        return samples.map { sample ->
            sample.copy(deltaToBestMs = deltaByFrameId[sample.frameId])
        }
    }

    private fun resolveCornerZonesBySegmentId(
        segmentId: Long?,
        trackMap: SessionAnalysisTrackMap?,
    ): Map<Long, List<TrackMapCornerZone>> = segmentId
        ?.let { resolvedSegmentId ->
            trackMap
                ?.let(trackMapCornerZoneDetector::detect)
                ?.takeIf(List<TrackMapCornerZone>::isNotEmpty)
                ?.let { zones -> mapOf(resolvedSegmentId to zones) }
        }
        .orEmpty()

    private fun buildSegments(data: DecodedRecordedTelemetrySession): List<SessionAnalysisSegment> = data.segments
        .takeIf(List<DecodedRecordedTelemetrySegment>::isNotEmpty)
        ?.map(DecodedRecordedTelemetrySegment::toSessionAnalysisSegment)
        ?: listOf(
            SessionAnalysisSegment(
                segmentId = data.sessionId,
                sessionTypeLabel = data.metadata.sessionType.orEmpty(),
                sessionLabel = data.metadata.sessionType.orEmpty(),
                carLabel = data.metadata.carName ?: data.metadata.carModel.orEmpty(),
                trackLabel = data.metadata.trackName ?: data.metadata.trackId.orEmpty(),
                startedAtMs = data.metadata.startedAtMs,
                endedAtMs = data.metadata.endedAtMs,
            ),
        )
}
