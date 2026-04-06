package com.analyzer.session.analysis.presentation.builder.track.trace

import com.analyzer.session.analysis.domain.trackmap.SessionAnalysisTrackMapGeometry
import com.analyzer.session.analysis.presentation.builder.lap.resolveLapFractions
import com.analyzer.session.analysis.presentation.builder.lap.resolveTraceLapFractions
import com.analyzer.session.analysis.presentation.builder.track.support.SessionAnalysisTrackPointSampler
import com.analyzer.session.analysis.presentation.builder.track.support.SessionAnalysisTrackTraceArtifactFilter
import com.analyzer.session.analysis.presentation.builder.track.support.TrackMapFractionLookup
import com.analyzer.session.analysis.presentation.builder.track.support.TrackProjectionMatch
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Projects raw telemetry and overlay traces into the displayed track map while cleaning up ordering issues,
 * duplicate points, and small projection artifacts.
 */
internal class SessionAnalysisTrackCanvasTraceBuilder(
    private val pointSampler: SessionAnalysisTrackPointSampler = SessionAnalysisTrackPointSampler(),
    private val trackMapGeometry: SessionAnalysisTrackMapGeometry = SessionAnalysisTrackMapGeometry(),
    private val traceArtifactFilter: SessionAnalysisTrackTraceArtifactFilter =
        SessionAnalysisTrackTraceArtifactFilter(),
    private val traceGeometry: SessionAnalysisTrackCanvasTraceGeometry =
        SessionAnalysisTrackCanvasTraceGeometry(pointSampler, trackMapGeometry),
) {

    /**
     * Projects overlay points onto the display map, preferring cheap direct reuse before falling back
     * to guided reconstruction.
     */
    fun buildProjectedOverlayTrace(
        overlayPoints: List<SessionAnalysisTrackMapPoint>,
        sourceTrackMap: SessionAnalysisTrackMap?,
        displayTrackMap: SessionAnalysisTrackMap?,
        maxPoints: Int,
    ): ImmutableList<SessionAnalysisFractionPointUi> {
        if (overlayPoints.size < 2 || maxPoints <= 0) return persistentListOf()

        val sampledOverlay = pointSampler.sampleEvenlyByPathDistance(
            points = overlayPoints,
            maxPoints = maxPoints,
            xSelector = SessionAnalysisTrackMapPoint::x,
            ySelector = SessionAnalysisTrackMapPoint::y,
        )
        if (sampledOverlay.size < 2) return persistentListOf()

        val resolvedSourceTrackMap = sourceTrackMap ?: displayTrackMap
        val resolvedDisplayTrackMap = displayTrackMap ?: sourceTrackMap
        buildDirectOverlayTraceIfFits(
            sampledOverlay = sampledOverlay,
            displayTrackMap = resolvedDisplayTrackMap,
        )?.let { trace -> return trace }

        val sourcePoints = resolvedSourceTrackMap?.points.orEmpty()
        val sourceLookup = TrackMapFractionLookup.create(sourcePoints)
        val orderedOverlay = if (sourceLookup != null) {
            resolveOrderedOverlaySamples(
                overlayPoints = sampledOverlay,
                guideLookup = sourceLookup,
                guidePoints = sourcePoints,
            )
        } else {
            emptyList()
        }

        return if (orderedOverlay.size >= 2) {
            buildGuidedOverlayTrace(
                orderedOverlay = orderedOverlay,
                sourceTrackMap = resolvedSourceTrackMap,
                displayTrackMap = resolvedDisplayTrackMap,
            )
        } else {
            buildFallbackOverlayTrace(
                sampledOverlay = sampledOverlay,
                sourceTrackMap = resolvedSourceTrackMap,
                displayTrackMap = resolvedDisplayTrackMap,
            )
        }
    }

    /**
     * Builds the selected-lap trace in display-map space while preserving native telemetry geometry whenever possible.
     */
    fun buildProjectedTrackTrace(
        samples: List<SessionAnalysisSample>,
        sourceTrackMap: SessionAnalysisTrackMap?,
        displayTrackMap: SessionAnalysisTrackMap?,
        maxPoints: Int,
    ): ImmutableList<SessionAnalysisFractionPointUi> {
        if (samples.isEmpty() || maxPoints <= 0) return persistentListOf()

        val orderedSamples = samples.sortedBy(SessionAnalysisSample::sampleIndexInLap)
        val resolvedTrackMap = displayTrackMap ?: sourceTrackMap
        val rawRecordedSamples = buildRawRecordedSamples(orderedSamples)
        val usesMapProjection = shouldUseMapProjection(
            rawRecordedSamples = rawRecordedSamples,
            sourceTrackMap = sourceTrackMap,
            displayTrackMap = displayTrackMap,
        )
        val resolvedFractions = resolveRecordedTraceFractions(
            orderedSamples = orderedSamples,
            sourceTrackMap = sourceTrackMap,
            resolvedTrackMap = resolvedTrackMap,
            usesMapProjection = usesMapProjection,
        )
        val displayLookup = TrackMapFractionLookup.create(resolvedTrackMap?.points.orEmpty())
        val resolvedRecordedSamples = rawRecordedSamples.mapIndexed { index, sample ->
            sample.copy(fraction = resolvedFractions.getOrElse(index) { sample.fraction })
        }
        val projectedRecordedTrace = projectRecordedTrace(
            rawTrace = resolvedRecordedSamples.map(SessionAnalysisFractionPointUi::toTrackMapPoint),
            rawFractions = resolvedRecordedSamples.map(SessionAnalysisFractionPointUi::fraction),
            sourceTrackMap = sourceTrackMap,
            displayTrackMap = displayTrackMap,
        )
        val recordedLookup = TrackMapFractionLookup.create(
            points = projectedRecordedTrace,
            fractionsOverride = resolvedRecordedSamples.map(SessionAnalysisFractionPointUi::fraction),
        )
        val usesRecordedTelemetryTrace = projectedRecordedTrace.size >= 2 && recordedLookup != null

        val projectedTrace = buildProjectedTraceSamples(
            orderedSamples = orderedSamples,
            resolvedFractions = resolvedFractions,
            recordedLookup = recordedLookup,
            displayLookup = displayLookup,
        )
        val displayHalfWidth = traceGeometry.resolveMedianHalfWidth(
            primaryTrackMap = resolvedTrackMap,
            fallbackTrackMap = sourceTrackMap,
        )
        val suppressLocalFolds = sourceTrackMap != null && displayTrackMap != null && sourceTrackMap !== displayTrackMap
        val recordedTraceMaxLateralDrift = if (usesRecordedTelemetryTrace) displayHalfWidth * 0.24f else null
        return prepareProjectedTrackTrace(
            projectedTrace = projectedTrace,
            maxPoints = maxPoints,
            suppressLocalFolds = suppressLocalFolds,
            recordedTraceMaxLateralDrift = recordedTraceMaxLateralDrift,
            smoothingGuideLookup = displayLookup ?: recordedLookup,
            guideLookup = recordedLookup ?: displayLookup,
        )
    }

    private fun buildDirectOverlayTraceIfFits(
        sampledOverlay: List<SessionAnalysisTrackMapPoint>,
        displayTrackMap: SessionAnalysisTrackMap?,
    ): ImmutableList<SessionAnalysisFractionPointUi>? {
        if (displayTrackMap == null) return null
        if (!traceGeometry.overlayAlreadyFitsTrackWorld(
                overlayPoints = sampledOverlay,
                displayTrackMap = displayTrackMap,
            )
        ) {
            return null
        }

        val directOverlay = traceArtifactFilter.removeNearDuplicateTracePoints(
            points = traceArtifactFilter.toFractionTrace(sampledOverlay),
            minDistance = 0.02f,
        )
        val displayHalfWidth = traceGeometry.resolveMedianHalfWidth(primaryTrackMap = displayTrackMap)
        return traceArtifactFilter.smoothMinorTraceJitter(
            points = traceArtifactFilter.suppressLocalFoldArtifacts(directOverlay),
            maxLateralDrift = displayHalfWidth * 0.22f,
        ).toImmutableList()
    }

    private fun buildGuidedOverlayTrace(
        orderedOverlay: List<SessionAnalysisFractionPointUi>,
        sourceTrackMap: SessionAnalysisTrackMap?,
        displayTrackMap: SessionAnalysisTrackMap?,
    ): ImmutableList<SessionAnalysisFractionPointUi> {
        val projectedOverlay = if (
            sourceTrackMap != null &&
            displayTrackMap != null &&
            traceGeometry.shouldProjectBetweenMaps(sourceTrackMap = sourceTrackMap, displayTrackMap = displayTrackMap)
        ) {
            traceGeometry.projectPointsByFraction(
                points = orderedOverlay.map(SessionAnalysisFractionPointUi::toTrackMapPoint),
                fractions = orderedOverlay.map(SessionAnalysisFractionPointUi::fraction),
                sourceTrackMap = sourceTrackMap,
                displayTrackMap = displayTrackMap,
            )
        } else {
            orderedOverlay.map(SessionAnalysisFractionPointUi::toTrackMapPoint)
        }
        val deduplicatedOverlay = traceArtifactFilter.removeNearDuplicateTracePoints(
            points = projectedOverlay.mapIndexed { index, point ->
                SessionAnalysisFractionPointUi(
                    fraction = orderedOverlay[index].fraction,
                    x = point.x,
                    y = point.y,
                )
            },
            minDistance = 0.02f,
        )
        val displayHalfWidth = traceGeometry.resolveMedianHalfWidth(
            primaryTrackMap = displayTrackMap,
            fallbackTrackMap = sourceTrackMap,
        )
        return traceArtifactFilter.smoothMinorTraceJitter(
            points = traceArtifactFilter.suppressLocalFoldArtifacts(deduplicatedOverlay),
            maxLateralDrift = displayHalfWidth * 0.22f,
        ).toImmutableList()
    }

    private fun buildFallbackOverlayTrace(
        sampledOverlay: List<SessionAnalysisTrackMapPoint>,
        sourceTrackMap: SessionAnalysisTrackMap?,
        displayTrackMap: SessionAnalysisTrackMap?,
    ): ImmutableList<SessionAnalysisFractionPointUi> {
        val fallbackHalfWidth = traceGeometry.resolveMedianHalfWidth(
            primaryTrackMap = displayTrackMap,
            fallbackTrackMap = sourceTrackMap,
        )
        return traceArtifactFilter.smoothMinorTraceJitter(
            points = traceArtifactFilter.suppressLocalFoldArtifacts(
                traceArtifactFilter.toFractionTrace(sampledOverlay),
            ),
            maxLateralDrift = fallbackHalfWidth * 0.22f,
        ).toImmutableList()
    }

    private fun buildRawRecordedSamples(
        orderedSamples: List<SessionAnalysisSample>,
    ): List<SessionAnalysisFractionPointUi> = orderedSamples.mapIndexedNotNull { index, sample ->
        val sampleX = sample.trackX?.takeIf(Float::isFinite) ?: return@mapIndexedNotNull null
        val sampleY = sample.trackY?.takeIf(Float::isFinite) ?: return@mapIndexedNotNull null
        SessionAnalysisFractionPointUi(
            fraction = if (orderedSamples.lastIndex <= 0) 0f else index.toFloat() / orderedSamples.lastIndex.toFloat(),
            x = sampleX,
            y = sampleY,
            frameId = sample.frameId,
        )
    }

    private fun shouldUseMapProjection(
        rawRecordedSamples: List<SessionAnalysisFractionPointUi>,
        sourceTrackMap: SessionAnalysisTrackMap?,
        displayTrackMap: SessionAnalysisTrackMap?,
    ): Boolean {
        val rawTraceAlreadyFitsDisplayTrack = displayTrackMap?.let { trackMap ->
            traceGeometry.traceAlreadyFitsTrackWorld(
                rawTrace = rawRecordedSamples.map(SessionAnalysisFractionPointUi::toTrackMapPoint),
                displayTrackMap = trackMap,
            )
        } == true
        return traceGeometry.shouldProjectBetweenMaps(
            sourceTrackMap = sourceTrackMap,
            displayTrackMap = displayTrackMap,
        ) &&
            !rawTraceAlreadyFitsDisplayTrack
    }

    private fun resolveRecordedTraceFractions(
        orderedSamples: List<SessionAnalysisSample>,
        sourceTrackMap: SessionAnalysisTrackMap?,
        resolvedTrackMap: SessionAnalysisTrackMap?,
        usesMapProjection: Boolean,
    ): List<Float> = if (usesMapProjection) {
        resolveTraceLapFractions(
            samples = orderedSamples,
            trackMap = sourceTrackMap,
        )
    } else {
        resolveLapFractions(
            samples = orderedSamples,
            trackMap = sourceTrackMap ?: resolvedTrackMap,
        )
    }

    private fun buildProjectedTraceSamples(
        orderedSamples: List<SessionAnalysisSample>,
        resolvedFractions: List<Float>,
        recordedLookup: TrackMapFractionLookup?,
        displayLookup: TrackMapFractionLookup?,
    ): List<SessionAnalysisFractionPointUi> = orderedSamples.mapIndexedNotNull { index, sample ->
        val fraction = resolvedFractions.getOrElse(index) { 0f }
        val projectedPoint = recordedLookup?.samplePositionAt(fraction)
            ?: samplePositionAt(displayLookup, fraction)
            ?: return@mapIndexedNotNull null
        SessionAnalysisFractionPointUi(
            fraction = fraction,
            x = projectedPoint.first,
            y = projectedPoint.second,
            frameId = sample.frameId,
        )
    }

    private fun prepareProjectedTrackTrace(
        projectedTrace: List<SessionAnalysisFractionPointUi>,
        maxPoints: Int,
        suppressLocalFolds: Boolean,
        recordedTraceMaxLateralDrift: Float?,
        smoothingGuideLookup: TrackMapFractionLookup?,
        guideLookup: TrackMapFractionLookup?,
    ): ImmutableList<SessionAnalysisFractionPointUi> {
        val sampledTrace = pointSampler.sampleEvenlyByPathDistance(
            points = projectedTrace,
            maxPoints = maxPoints,
            xSelector = SessionAnalysisFractionPointUi::x,
            ySelector = SessionAnalysisFractionPointUi::y,
        )
        val deduplicatedTrace = traceArtifactFilter.removeNearDuplicateTracePoints(
            points = sampledTrace,
            minDistance = 0.02f,
        )
        val cleanedTrace = if (suppressLocalFolds) {
            traceArtifactFilter.suppressLocalFoldArtifacts(deduplicatedTrace)
        } else {
            deduplicatedTrace
        }
        val smoothedFallbackTrace = traceArtifactFilter.smoothTrace(cleanedTrace)
        val preparedTrace = if (recordedTraceMaxLateralDrift != null) {
            traceArtifactFilter.smoothMinorTraceJitter(
                points = cleanedTrace,
                maxLateralDrift = recordedTraceMaxLateralDrift,
                guideLookup = smoothingGuideLookup,
            )
        } else {
            smoothedFallbackTrace
        }
        val fallbackTrace = if (recordedTraceMaxLateralDrift != null) smoothedFallbackTrace else cleanedTrace
        return traceArtifactFilter.stabilizeTraceProgression(
            points = preparedTrace,
            fallbackPoints = fallbackTrace,
            guideLookup = guideLookup,
        ).toImmutableList()
    }

    private fun samplePositionAt(lookup: TrackMapFractionLookup?, fraction: Float): Pair<Float, Float>? =
        lookup?.samplePositionAt(fraction)

    private fun resolveOrderedOverlaySamples(
        overlayPoints: List<SessionAnalysisTrackMapPoint>,
        guideLookup: TrackMapFractionLookup,
        guidePoints: List<SessionAnalysisTrackMapPoint>,
    ): List<SessionAnalysisFractionPointUi> {
        if (overlayPoints.size < 2) return emptyList()

        val maxAllowedDistance = maxOf(trackMapGeometry.medianHalfWidth(guidePoints) ?: 8f, 6f) * 2.2f
        val maxAllowedDistanceSquared = maxAllowedDistance * maxAllowedDistance
        val guidedPoints = ArrayList<SessionAnalysisFractionPointUi>(overlayPoints.size)
        var previousUnwrappedFraction: Float? = null

        overlayPoints.forEach { point ->
            val preferredFraction = previousUnwrappedFraction?.normalizeTrackFraction()
            val localMatch = preferredFraction?.let { fraction ->
                guideLookup.nearestProjectionTo(
                    x = point.x,
                    y = point.y,
                    preferredFraction = fraction,
                    fractionWindow = 0.12f,
                )
            }
            val globalMatch = guideLookup.nearestProjectionTo(
                x = point.x,
                y = point.y,
                preferredFraction = null,
                fractionWindow = null,
            )
            val resolvedMatch = resolveGuidedOverlayMatch(
                localMatch = localMatch,
                globalMatch = globalMatch,
                previousUnwrappedFraction = previousUnwrappedFraction,
            ) ?: return@forEach
            if (resolvedMatch.distanceSquared > maxAllowedDistanceSquared) return@forEach

            val unwrappedFraction = unwrapTrackFraction(
                fraction = resolvedMatch.fraction,
                previousFraction = previousUnwrappedFraction,
            )
            if (previousUnwrappedFraction != null && unwrappedFraction < previousUnwrappedFraction - 0.035f) {
                return@forEach
            }

            guidedPoints += SessionAnalysisFractionPointUi(
                fraction = unwrappedFraction.normalizeTrackFraction(),
                x = point.x,
                y = point.y,
            )
            previousUnwrappedFraction = unwrappedFraction
        }

        return guidedPoints
    }

    private fun resolveGuidedOverlayMatch(
        localMatch: TrackProjectionMatch?,
        globalMatch: TrackProjectionMatch?,
        previousUnwrappedFraction: Float?,
    ): TrackProjectionMatch? {
        val resolvedGlobal = globalMatch ?: return localMatch
        val resolvedLocal = localMatch ?: return resolvedGlobal
        previousUnwrappedFraction ?: return if (resolvedLocal.distanceSquared <= resolvedGlobal.distanceSquared) {
            resolvedLocal
        } else {
            resolvedGlobal
        }

        val localUnwrappedFraction = unwrapTrackFraction(
            fraction = resolvedLocal.fraction,
            previousFraction = previousUnwrappedFraction,
        )
        val globalUnwrappedFraction = unwrapTrackFraction(
            fraction = resolvedGlobal.fraction,
            previousFraction = previousUnwrappedFraction,
        )
        val localBacktracks = localUnwrappedFraction < previousUnwrappedFraction - 0.02f
        val globalBacktracks = globalUnwrappedFraction < previousUnwrappedFraction - 0.02f

        return when {
            !localBacktracks && globalBacktracks -> resolvedLocal
            localBacktracks && !globalBacktracks -> resolvedGlobal
            resolvedLocal.distanceSquared <= resolvedGlobal.distanceSquared * 1.6f -> resolvedLocal
            else -> resolvedGlobal
        }
    }

    private fun projectRecordedTrace(
        rawTrace: List<SessionAnalysisTrackMapPoint>,
        rawFractions: List<Float>,
        sourceTrackMap: SessionAnalysisTrackMap?,
        displayTrackMap: SessionAnalysisTrackMap?,
    ): List<SessionAnalysisTrackMapPoint> {
        if (rawTrace.size < 2) return rawTrace
        val sourcePoints = sourceTrackMap?.points.orEmpty()
        val displayPoints = displayTrackMap?.points.orEmpty()
        if (sourcePoints.size < 2 || displayPoints.size < 2) return rawTrace
        if (sourceTrackMap == null || displayTrackMap == null) return rawTrace
        if (traceGeometry.traceAlreadyFitsTrackWorld(rawTrace = rawTrace, displayTrackMap = displayTrackMap)) {
            return rawTrace
        }
        if (!traceGeometry.shouldProjectBetweenMaps(
                sourceTrackMap = sourceTrackMap,
                displayTrackMap = displayTrackMap,
            )
        ) {
            return rawTrace
        }

        return traceGeometry.projectPointsByFraction(
            points = rawTrace,
            fractions = rawFractions,
            sourceTrackMap = sourceTrackMap,
            displayTrackMap = displayTrackMap,
        ).ifEmpty { rawTrace }
    }
}
