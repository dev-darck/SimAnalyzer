package com.analyzer.session.analysis.presentation.pipeline

import com.analyzer.session.analysis.domain.trackmap.SessionAnalysisTrackMapGeometry
import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSectorUi
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_sector_note_calibration_gap
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_sector_note_losing_time
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_sector_note_on_target
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_sector_note_telemetry_gap
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_sector_short
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.calibration.SectorCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import org.jetbrains.compose.resources.getString
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Builds sector cards either from authored calibration gates or from telemetry sector transitions
 * so the comparison view stays usable on sessions without imported track assets.
 */
internal suspend fun buildSectors(
    selectedSamples: List<SessionAnalysisSample>,
    referenceSamples: List<SessionAnalysisSample>,
    comparisonPoints: List<SessionAnalysisComparisonPointUi>,
    trackMap: SessionAnalysisTrackMap?,
    displayTrackMap: SessionAnalysisTrackMap? = trackMap,
    calibration: TrackCalibration?,
): List<SessionAnalysisSectorUi> {
    if (comparisonPoints.size < 2) return emptyList()

    buildFromCalibration(
        comparisonPoints = comparisonPoints,
        trackMap = trackMap,
        displayTrackMap = displayTrackMap,
        calibration = calibration,
    )?.let { calibratedSectors ->
        if (calibratedSectors.isNotEmpty()) return calibratedSectors
    }

    val boundaries = resolveBoundaries(selectedSamples, referenceSamples)
    return if (boundaries.size >= 2) {
        buildFallbackSectors(boundaries = boundaries, comparisonPoints = comparisonPoints)
    } else {
        emptyList()
    }
}

private suspend fun buildFromCalibration(
    comparisonPoints: List<SessionAnalysisComparisonPointUi>,
    trackMap: SessionAnalysisTrackMap?,
    displayTrackMap: SessionAnalysisTrackMap?,
    calibration: TrackCalibration?,
): List<SessionAnalysisSectorUi>? {
    calibration ?: return null
    val guidePoints = trackMap.guidePoints()
        ?: return null
    val displayGuidePoints = displayTrackMap.guidePoints() ?: guidePoints
    val guide = guidePoints.withCumulativeFractions()
    if (guide.size < 2) return null

    val startFinishFraction = guide.nearestFraction(
        x = calibration.startFinish.center.x,
        y = calibration.startFinish.center.y,
    ) ?: return null
    val orderedSectors = calibration.sectors.sortedBy { sector -> sector.index }
    if (orderedSectors.isEmpty()) return null

    val normalizedStarts = orderedSectors.resolveNormalizedStarts(
        guide = guide,
        startFinishFraction = startFinishFraction,
    )
    if (normalizedStarts.isEmpty()) return null
    val displayGateCenters = resolveDisplayGateCenters(
        gates = orderedSectors.map { sector -> sector.start },
        calibrationGuidePoints = guidePoints,
        displayGuidePoints = displayGuidePoints,
    )

    val result = ArrayList<SessionAnalysisSectorUi>(orderedSectors.size)
    for ((index, sector) in orderedSectors.withIndex()) {
        val start = normalizedStarts[index]
        val end = normalizedStarts.sectorEnd(index = index, start = start)
        result += buildCalibratedSector(
            sector = sector,
            start = start,
            end = end,
            comparisonPoints = comparisonPoints,
            displayGateCenter = displayGateCenters.getOrNull(index),
        )
    }
    return result
}

private suspend fun buildFallbackSectors(
    boundaries: List<Float>,
    comparisonPoints: List<SessionAnalysisComparisonPointUi>,
): List<SessionAnalysisSectorUi> {
    val result = ArrayList<SessionAnalysisSectorUi>(boundaries.size - 1)
    for (index in 0 until boundaries.lastIndex) {
        val start = boundaries[index]
        val end = boundaries[index + 1]
        result += buildSector(
            label = getString(Res.string.session_analysis_sector_short, index + 1),
            start = start,
            end = end,
            comparisonPoints = comparisonPoints,
            missingTimeNote = getString(Res.string.session_analysis_sector_note_telemetry_gap),
        )
    }
    return result
}

private suspend fun buildCalibratedSector(
    sector: SectorCalibration,
    start: Float,
    end: Float,
    comparisonPoints: List<SessionAnalysisComparisonPointUi>,
    displayGateCenter: SessionAnalysisTrackMapPoint?,
): SessionAnalysisSectorUi = buildSector(
    label = getString(Res.string.session_analysis_sector_short, sector.index),
    start = start,
    end = end,
    comparisonPoints = comparisonPoints,
    missingTimeNote = getString(Res.string.session_analysis_sector_note_calibration_gap),
    gateCenter = displayGateCenter ?: SessionAnalysisTrackMapPoint(
        x = sector.start.center.x,
        y = sector.start.center.y,
    ),
    gate = sector.start,
)

private suspend fun buildSector(
    label: String,
    start: Float,
    end: Float,
    comparisonPoints: List<SessionAnalysisComparisonPointUi>,
    missingTimeNote: String,
    gateCenter: SessionAnalysisTrackMapPoint? = null,
    gate: Gate? = null,
): SessionAnalysisSectorUi {
    val timing = comparisonPoints.sectorTiming(start = start, end = end)
    return SessionAnalysisSectorUi(
        label = label,
        startTrackPosition = start,
        endTrackPosition = end,
        markerTrackPosition = ((start + end) * 0.5f).coerceIn(0f, 1f),
        selectedTimeMs = timing.selectedTimeMs,
        referenceTimeMs = timing.referenceTimeMs,
        deltaMs = timing.deltaMs,
        note = timing.resolveNote(missingTimeNote),
        gateCenterX = gateCenter?.x,
        gateCenterY = gateCenter?.y,
        gateNormalX = gate?.normal?.x,
        gateNormalY = gate?.normal?.y,
        gateHalfWidthMeters = gate?.halfWidthMeters,
    )
}

private fun List<SessionAnalysisComparisonPointUi>.sectorTiming(start: Float, end: Float): SectorTiming {
    val selectedStart = elapsedAt(start, selected = true)
    val selectedEnd = elapsedAt(end, selected = true)
    val referenceStart = elapsedAt(start, selected = false)
    val referenceEnd = elapsedAt(end, selected = false)
    return SectorTiming(
        selectedTimeMs = if (selectedStart != null && selectedEnd != null) selectedEnd - selectedStart else null,
        referenceTimeMs = if (referenceStart != null && referenceEnd != null) referenceEnd - referenceStart else null,
    )
}

private suspend fun SectorTiming.resolveNote(missingTimeNote: String): String = when {
    selectedTimeMs == null || referenceTimeMs == null -> missingTimeNote
    selectedTimeMs <= referenceTimeMs -> getString(Res.string.session_analysis_sector_note_on_target)
    else -> getString(Res.string.session_analysis_sector_note_losing_time)
}

private val SectorTiming.deltaMs: Int?
    get() = if (selectedTimeMs != null && referenceTimeMs != null) {
        selectedTimeMs - referenceTimeMs
    } else {
        null
    }

private fun resolveBoundaries(
    selectedSamples: List<SessionAnalysisSample>,
    referenceSamples: List<SessionAnalysisSample>,
): List<Float> {
    val preferred = referenceSamples.ifEmpty { selectedSamples }
    val transitions = buildList<Float> {
        var previousSectorIndex: Int? = null
        preferred
            .asSequence()
            .filter { sample -> sample.sectorIndex >= 0 }
            .sortedBy(SessionAnalysisSample::sampleIndexInLap)
            .forEach { sample ->
                val trackPosition = sample.trackPosition?.coerceIn(0f, 1f) ?: return@forEach
                if (sample.sectorIndex != previousSectorIndex && trackPosition in 0.02f..0.98f) {
                    if (none { boundary -> abs(boundary - trackPosition) < 0.01f }) {
                        add(trackPosition)
                    }
                }
                previousSectorIndex = sample.sectorIndex
            }
    }.sorted()

    return buildList<Float> {
        add(0f)
        addAll(transitions)
        add(1f)
    }.distinct()
}

/**
 * Projects sector gate markers onto the display track map so authored calibration can stay aligned
 * with the currently rendered circuit geometry.
 */
private fun resolveDisplayGateCenters(
    gates: List<Gate>,
    calibrationGuidePoints: List<SessionAnalysisTrackMapPoint>,
    displayGuidePoints: List<SessionAnalysisTrackMapPoint>,
): List<SessionAnalysisTrackMapPoint> {
    if (gates.isEmpty()) return emptyList()
    val rawCenters = gates.map { gate ->
        SessionAnalysisTrackMapPoint(x = gate.center.x, y = gate.center.y)
    }
    if (rawCenters.size < 2 || calibrationGuidePoints.size < 2 || displayGuidePoints.size < 2) {
        return rawCenters
    }
    return SessionAnalysisTrackMapGeometry().transformOverlay(
        overlayPoints = rawCenters,
        sourceCenterLine = calibrationGuidePoints,
        targetCenterLine = displayGuidePoints,
    ).takeIf { centers -> centers.size == rawCenters.size } ?: rawCenters
}

private fun SessionAnalysisTrackMap?.guidePoints(): List<SessionAnalysisTrackMapPoint>? =
    this?.points?.takeIf { points -> points.size >= 2 }
        ?: this?.idealPoints?.takeIf { points -> points.size >= 2 }

private fun List<SectorCalibration>.resolveNormalizedStarts(
    guide: List<Pair<Float, SessionAnalysisTrackMapPoint>>,
    startFinishFraction: Float,
): List<Float> {
    val rawStarts = ArrayList<Float>(size)
    for (sector in this) {
        val absoluteFraction = guide.nearestFraction(
            x = sector.start.center.x,
            y = sector.start.center.y,
        ) ?: return emptyList()
        rawStarts += normalizeRelativeFraction(
            fraction = absoluteFraction,
            startFinishFraction = startFinishFraction,
        )
    }
    return rawStarts.ensureMonotonicBoundaries()
}

private fun List<Float>.sectorEnd(index: Int, start: Float): Float = getOrNull(index + 1)
    ?.takeIf { next -> next > start + 0.002f }
    ?: 1f

private fun List<SessionAnalysisTrackMapPoint>.withCumulativeFractions():
    List<Pair<Float, SessionAnalysisTrackMapPoint>> {
    if (isEmpty()) return emptyList()
    if (size == 1) return listOf(0f to first())

    val distances = FloatArray(size)
    var totalDistance = 0f
    for (index in 1 until size) {
        val previous = this[index - 1]
        val current = this[index]
        totalDistance += hypot(current.x - previous.x, current.y - previous.y)
        distances[index] = totalDistance
    }
    val safeTotalDistance = totalDistance.takeIf { it > 0.0001f } ?: return mapIndexed { index, point ->
        index.toFloat() / lastIndex.toFloat() to point
    }
    return mapIndexed { index, point ->
        (distances[index] / safeTotalDistance).coerceIn(0f, 1f) to point
    }
}

private fun List<Pair<Float, SessionAnalysisTrackMapPoint>>.nearestFraction(x: Float, y: Float): Float? =
    minByOrNull { (_, point) ->
        val dx = point.x - x
        val dy = point.y - y
        dx * dx + dy * dy
    }?.first

private fun normalizeRelativeFraction(fraction: Float, startFinishFraction: Float): Float {
    val shifted = fraction - startFinishFraction
    return when {
        shifted < 0f -> shifted + 1f
        shifted > 1f -> shifted - 1f
        else -> shifted
    }.coerceIn(0f, 1f)
}

private fun List<Float>.ensureMonotonicBoundaries(): List<Float> {
    if (isEmpty()) return emptyList()
    val normalized = ArrayList<Float>(size)
    forEachIndexed { index, value ->
        val resolved = when (index) {
            0 -> 0f
            else -> value.coerceAtLeast((normalized[index - 1] + 0.002f).coerceAtMost(0.998f))
        }.coerceIn(0f, 0.998f)
        normalized += resolved
    }
    return normalized
}

private fun List<SessionAnalysisComparisonPointUi>.elapsedAt(trackPosition: Float, selected: Boolean): Int? {
    val first = firstOrNull() ?: return null
    val last = lastOrNull() ?: return null
    if (trackPosition <= first.trackPosition) return if (selected) first.selectedElapsedMs else first.referenceElapsedMs
    if (trackPosition >= last.trackPosition) return if (selected) last.selectedElapsedMs else last.referenceElapsedMs

    val nextIndex = indexOfFirst { point -> point.trackPosition >= trackPosition }
    if (nextIndex <= 0) return null

    val previous = get(nextIndex - 1)
    val next = get(nextIndex)
    val startValue = if (selected) previous.selectedElapsedMs else previous.referenceElapsedMs
    val endValue = if (selected) next.selectedElapsedMs else next.referenceElapsedMs
    if (startValue == null || endValue == null) return startValue ?: endValue

    val distance = (next.trackPosition - previous.trackPosition).takeIf { it > 0.0001f } ?: return endValue
    val fraction = ((trackPosition - previous.trackPosition) / distance).coerceIn(0f, 1f)
    return startValue + ((endValue - startValue) * fraction).toInt()
}
