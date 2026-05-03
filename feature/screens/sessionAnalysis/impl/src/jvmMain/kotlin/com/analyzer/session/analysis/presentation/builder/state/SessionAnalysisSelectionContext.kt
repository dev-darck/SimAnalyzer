package com.analyzer.session.analysis.presentation.builder.state

import com.analyzer.session.analysis.presentation.builder.lap.findBestReferenceLap
import com.project.analyzer.telemetry.analysis.api.model.lap.SessionAnalysisLap
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisCornerZone
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisReport
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.segment.SessionAnalysisSegment

internal data class SessionAnalysisSelectionContext(
    val segment: SessionAnalysisSegment?,
    val segmentId: Long?,
    val segmentLaps: List<SessionAnalysisLap>,
    val segmentSamples: List<SessionAnalysisSample>,
    val selectedLapNumber: Int?,
    val selectedLap: SessionAnalysisLap?,
    val referenceLap: SessionAnalysisLap?,
    val referenceLapNumber: Int?,
    val selectedSamples: List<SessionAnalysisSample>,
    val referenceSamples: List<SessionAnalysisSample>,
    val cornerZones: List<SessionAnalysisCornerZone>,
    val referenceLapIsCustom: Boolean,
    val hasExternalReference: Boolean,
)

internal fun SessionAnalysisReport.resolveSelectionContext(
    referenceReport: SessionAnalysisReport? = null,
    selectedSegmentId: Long?,
    selectedReferenceSegmentId: Long?,
    selectedLapNumber: Int?,
    selectedReferenceLapNumber: Int?,
): SessionAnalysisSelectionContext {
    val resolvedSegment = resolveSegment(selectedSegmentId)
    val resolvedSegmentId = resolvedSegment?.segmentId
    val segmentLaps = laps.filterLapsForSegment(resolvedSegmentId)
    val segmentSamples = samples.filterForSelection(
        segmentId = resolvedSegmentId,
        lapNumber = null,
    )
    val autoReferenceLap = findBestReferenceLap(
        segmentLaps = segmentLaps,
        segmentSamples = segmentSamples,
    )
    val resolvedLapNumber = resolveLapNumber(
        segmentId = resolvedSegmentId,
        selectedLapNumber = selectedLapNumber,
        segmentLaps = segmentLaps,
        referenceLapNumber = autoReferenceLap?.lapNumber,
    )
    val externalReference = referenceReport?.takeUnless { report -> report.sessionId == sessionId }
    val referenceSegmentId = externalReference?.resolveSegment(selectedReferenceSegmentId)?.segmentId
    val referenceSegmentLaps = externalReference?.laps?.filterLapsForSegment(referenceSegmentId).orEmpty()
    val referenceSegmentSamples = externalReference?.samples?.filterForSelection(
        segmentId = referenceSegmentId,
        lapNumber = null,
    ).orEmpty()
    val referenceLap = if (externalReference != null) {
        resolveReferenceLapSelection(
            segmentLaps = referenceSegmentLaps,
            segmentSamples = referenceSegmentSamples,
            selectedReferenceLapNumber = selectedReferenceLapNumber,
            selectedLapNumber = null,
        )
    } else {
        resolveReferenceLapSelection(
            segmentLaps = segmentLaps,
            segmentSamples = segmentSamples,
            selectedReferenceLapNumber = selectedReferenceLapNumber,
            selectedLapNumber = resolvedLapNumber,
        )
    }
    val referenceLapNumber = referenceLap?.lapNumber
    return SessionAnalysisSelectionContext(
        segment = resolvedSegment,
        segmentId = resolvedSegmentId,
        segmentLaps = segmentLaps,
        segmentSamples = segmentSamples,
        selectedLapNumber = resolvedLapNumber,
        selectedLap = segmentLaps.firstOrNull { lap -> lap.lapNumber == resolvedLapNumber },
        referenceLap = referenceLap,
        referenceLapNumber = referenceLapNumber,
        selectedSamples = samples.filterForSelection(
            segmentId = resolvedSegmentId,
            lapNumber = resolvedLapNumber,
        ),
        referenceSamples = if (externalReference != null) {
            externalReference.samples.filterForSelection(
                segmentId = referenceSegmentId,
                lapNumber = referenceLapNumber,
            )
        } else {
            samples.filterForSelection(
                segmentId = resolvedSegmentId,
                lapNumber = referenceLapNumber,
            )
        },
        cornerZones = cornerZonesBySegmentId[resolvedSegmentId].orEmpty(),
        referenceLapIsCustom = selectedReferenceLapNumber != null &&
            referenceLapNumber == selectedReferenceLapNumber,
        hasExternalReference = externalReference != null,
    )
}
