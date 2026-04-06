package com.analyzer.session.analysis.domain.usecase

import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisCornerZone
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisReport
import kotlin.math.roundToInt

/**
 * Replaces report corner zones with imported ones only when the imported geometry looks plausible
 * enough to improve analysis without destabilizing corner numbering.
 */
internal fun SessionAnalysisReport.withPreferredCornerZones(
    preferredCornerZones: List<SessionAnalysisCornerZone>,
): SessionAnalysisReport {
    if (preferredCornerZones.isEmpty()) return this

    val defaultSegmentId = segments.lastOrNull()?.segmentId ?: sessionId
    val existingCornerZones = cornerZonesBySegmentId[defaultSegmentId].orEmpty()
    if (!shouldPreferCornerZones(existingCornerZones, preferredCornerZones)) return this

    return copy(
        cornerZonesBySegmentId = cornerZonesBySegmentId + (defaultSegmentId to preferredCornerZones),
    )
}

private fun shouldPreferCornerZones(
    existingCornerZones: List<SessionAnalysisCornerZone>,
    preferredCornerZones: List<SessionAnalysisCornerZone>,
): Boolean {
    if (existingCornerZones.isEmpty()) return true

    val existingCount = existingCornerZones.size
    val preferredCount = preferredCornerZones.size
    return when {
        isSuspiciouslyLarger(candidateCount = preferredCount, baselineCount = existingCount) -> false
        isSuspiciouslyLarger(candidateCount = existingCount, baselineCount = preferredCount) -> true
        preferredCount >= existingCount -> true
        existingCount - preferredCount <= preferredCornerZoneNearCountTolerance -> true
        else -> false
    }
}

private fun isSuspiciouslyLarger(candidateCount: Int, baselineCount: Int): Boolean {
    val absoluteLimit = baselineCount + preferredCornerZoneAbsoluteGrowthTolerance
    val ratioLimit = (baselineCount * preferredCornerZoneGrowthRatioTolerance).roundToInt()
    return candidateCount > absoluteLimit && candidateCount > ratioLimit
}

private const val preferredCornerZoneNearCountTolerance: Int = 4
private const val preferredCornerZoneAbsoluteGrowthTolerance: Int = 4
private const val preferredCornerZoneGrowthRatioTolerance: Float = 2.25f
