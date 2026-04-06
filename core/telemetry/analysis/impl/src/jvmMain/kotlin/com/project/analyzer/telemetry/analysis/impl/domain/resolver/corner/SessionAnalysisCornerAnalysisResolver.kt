package com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner

import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.TrackMapCornerZone
import dev.zacsweers.metro.Inject

internal const val cornerSteeringThresholdRad: Float = 0.055f
internal const val cornerLateralThresholdG: Float = 0.72f
internal const val cornerYawRateThreshold: Float = 0.18f
internal const val cornerLoadedSpeedKmh: Float = 60f
internal const val cornerBrakeThreshold: Float = 0.15f
internal const val cornerBrakeReleaseThreshold: Float = 0.06f
internal const val cornerThrottlePickupThreshold: Float = 0.35f
internal const val cornerCoastThrottleThreshold: Float = 0.08f
internal const val cornerCoastBrakeThreshold: Float = 0.08f
internal const val cornerReferenceMatchWindowPct: Float = 0.08f
internal const val cornerApexShiftWarnPct: Float = 0.012f
internal const val cornerFrontSlipWarn: Float = 0.16f
internal const val cornerRearSlipWarn: Float = 0.14f
internal const val cornerSlipDeltaWarn: Float = 0.06f
internal const val cornerMinWindowSamples: Int = 5
internal const val cornerMinWindowSpanPct: Float = 0.008f
internal const val cornerIndexGapTolerance: Int = 3
internal const val cornerContextPaddingSamples: Int = 6
internal const val cornerExitLookAheadSamples: Int = 8
internal const val geometryCornerCoverageRatio: Float = 0.6f
internal const val cornerMeaningfulSteeringRad: Float = 0.095f
internal const val cornerMeaningfulLateralG: Float = 1.05f
internal const val cornerMeaningfulSpeedDropKmh: Float = 10f
internal const val cornerMeaningfulLoadedSampleRatio: Float = 0.42f

/**
 * Builds per-corner telemetry analyses, preferring geometry-derived corner zones when the track map
 * is reliable and falling back to telemetry-only corner windows otherwise.
 */
@Inject
internal class SessionAnalysisCornerAnalysisResolver {

    /**
     * Resolves corner references and per-lap corner passes for each segment in the session.
     */
    fun resolve(
        samples: List<SessionAnalysisSample>,
        bestLapBySegmentId: Map<Long, Int?>,
        cornerZonesBySegmentId: Map<Long, List<TrackMapCornerZone>> = emptyMap(),
    ): SessionAnalysisCornerAnalysisReport {
        if (samples.isEmpty()) return SessionAnalysisCornerAnalysisReport()

        val analyses = mutableListOf<SessionAnalysisCornerAnalysis>()
        val referenceCornersBySegmentId = mutableMapOf<Long, List<SessionAnalysisCornerReference>>()

        samples.groupBy(SessionAnalysisSample::segmentId).forEach { (segmentId, segmentSamples) ->
            val samplesByLap = segmentSamples
                .groupBy(SessionAnalysisSample::lapNumber)
                .filterKeys { lapNumber -> lapNumber > 0 }
                .mapValues { (_, lapSamples) -> lapSamples.sortedBy(SessionAnalysisSample::sampleIndexInLap) }
                .filterValues { lapSamples -> lapSamples.size >= cornerMinWindowSamples }
            if (samplesByLap.isEmpty()) return@forEach

            val referenceLapNumber = bestLapBySegmentId[segmentId]
                ?.takeIf(samplesByLap::containsKey)
                ?: samplesByLap.maxByOrNull { (_, lapSamples) -> lapSamples.size }?.key
            val referenceSamples = referenceLapNumber?.let(samplesByLap::get).orEmpty()
            val geometryZones = cornerZonesBySegmentId[segmentId].orEmpty()
            val geometryReferenceCorners = buildReferenceCornersFromGeometry(referenceSamples, geometryZones)
            val usesGeometry = geometryZones.isNotEmpty() &&
                geometryReferenceCorners.size >= minimumGeometryCoverage(geometryZones.size)
            val referenceCorners = if (usesGeometry) {
                geometryReferenceCorners.sortedBy(SessionAnalysisCornerReference::cornerNumber)
            } else {
                buildReferenceCornersFromTelemetry(referenceSamples)
            }
            if (referenceCorners.isNotEmpty()) {
                referenceCornersBySegmentId[segmentId] = referenceCorners
            }

            samplesByLap.toSortedMap().forEach { (lapNumber, lapSamples) ->
                val geometryAnalyses = if (usesGeometry) {
                    resolveLapFromGeometry(
                        segmentId = segmentId,
                        lapNumber = lapNumber,
                        lapSamples = lapSamples,
                        geometryZones = geometryZones,
                        referenceCorners = referenceCorners,
                    )
                } else {
                    emptyList()
                }
                analyses += if (geometryAnalyses.isNotEmpty()) {
                    geometryAnalyses
                } else {
                    resolveLapFromTelemetry(
                        segmentId = segmentId,
                        lapNumber = lapNumber,
                        lapSamples = lapSamples,
                        referenceCorners = referenceCorners,
                    )
                }
            }
        }

        return SessionAnalysisCornerAnalysisReport(
            corners = analyses.sortedWith(
                compareBy<SessionAnalysisCornerAnalysis>(SessionAnalysisCornerAnalysis::segmentId)
                    .thenBy(SessionAnalysisCornerAnalysis::lapNumber)
                    .thenBy(SessionAnalysisCornerAnalysis::cornerNumber),
            ),
            referenceCornersBySegmentId = referenceCornersBySegmentId,
        )
    }

    private fun buildReferenceCornersFromGeometry(
        referenceSamples: List<SessionAnalysisSample>,
        geometryZones: List<TrackMapCornerZone>,
    ): List<SessionAnalysisCornerReference> = geometryZones.mapNotNull { zone ->
        val window = referenceSamples.extractGeometryWindow(zone) ?: return@mapNotNull null
        window.analyzeWindow(referenceSamples)
            ?.takeIf(CornerMeasurements::isMeaningfulCorner)
            ?.toGeometryReference(zone)
    }

    private fun buildReferenceCornersFromTelemetry(
        referenceSamples: List<SessionAnalysisSample>,
    ): List<SessionAnalysisCornerReference> {
        val referenceWindows = referenceSamples.extractCornerWindows()
        return referenceWindows.mapIndexedNotNull { index, window ->
            window.analyzeWindow(referenceSamples)
                ?.takeIf(CornerMeasurements::isMeaningfulCorner)
                ?.toReference(cornerNumber = index + 1)
        }
    }

    private fun resolveLapFromGeometry(
        segmentId: Long,
        lapNumber: Int,
        lapSamples: List<SessionAnalysisSample>,
        geometryZones: List<TrackMapCornerZone>,
        referenceCorners: List<SessionAnalysisCornerReference>,
    ): List<SessionAnalysisCornerAnalysis> {
        if (geometryZones.isEmpty()) return emptyList()
        val referenceByCornerNumber = referenceCorners.associateBy(SessionAnalysisCornerReference::cornerNumber)
        val analyses = geometryZones.mapNotNull { zone ->
            val window = lapSamples.extractGeometryWindow(zone) ?: return@mapNotNull null
            val measurements = window.analyzeWindow(lapSamples) ?: return@mapNotNull null
            if (!measurements.isMeaningfulCorner()) return@mapNotNull null
            measurements.toCornerAnalysis(
                segmentId = segmentId,
                lapNumber = lapNumber,
                cornerNumber = zone.cornerNumber,
                reference = referenceByCornerNumber[zone.cornerNumber],
            )
        }
        return analyses.takeIf { resolvedAnalyses ->
            resolvedAnalyses.size >= minimumGeometryCoverage(referenceCorners.size.coerceAtLeast(geometryZones.size))
        }.orEmpty()
    }

    private fun resolveLapFromTelemetry(
        segmentId: Long,
        lapNumber: Int,
        lapSamples: List<SessionAnalysisSample>,
        referenceCorners: List<SessionAnalysisCornerReference>,
    ): List<SessionAnalysisCornerAnalysis> {
        val windows = lapSamples.extractCornerWindows()
        if (windows.isEmpty()) return emptyList()

        return windows.matchCornerWindows(
            allSamples = lapSamples,
            referenceCorners = referenceCorners,
        ).mapNotNull { match ->
            val measurements = match.window.analyzeWindow(lapSamples) ?: return@mapNotNull null
            if (!measurements.isMeaningfulCorner()) return@mapNotNull null
            measurements.toCornerAnalysis(
                segmentId = segmentId,
                lapNumber = lapNumber,
                cornerNumber = match.cornerNumber,
                reference = match.reference,
            )
        }
    }
}
