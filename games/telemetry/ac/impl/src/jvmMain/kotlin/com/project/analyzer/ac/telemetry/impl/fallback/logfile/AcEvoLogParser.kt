package com.project.analyzer.ac.telemetry.impl.fallback.logfile

import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.CarSource
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoFileInfo
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoSessionType
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.Parsed
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.ResolvedTrack
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.SessionTypeSource
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.TrackIdSource
import com.project.analyzer.ac.telemetry.impl.internal.TrackIdNormalizer
import com.project.analyzer.utils.TrackIdentityAliasMatcher

internal class AcEvoLogParser {

    private var trackIdSource: TrackIdSource = TrackIdSource.NONE
    private var carSource: CarSource = CarSource.NONE
    private var penaltyGroupId: String? = null
    private var penaltyGroupMs: Long = 0L
    private var playerCarUuid: String? = null
    private var sessionTypeSource: SessionTypeSource = SessionTypeSource.NONE
    private val uuidToCarMap = mutableMapOf<String, String>()

    val currentPlayerCarUuid: String?
        get() = playerCarUuid

    fun resetForEpoch() {
        trackIdSource = TrackIdSource.NONE
        carSource = CarSource.NONE
        sessionTypeSource = SessionTypeSource.NONE
        clearPenaltyGroup()
    }

    fun clearPenaltyGroup() {
        penaltyGroupId = null
        penaltyGroupMs = 0L
    }

    fun clear() {
        resetForEpoch()
        playerCarUuid = null
        uuidToCarMap.clear()
    }

    fun parseLines(lines: List<String>, includePenalties: Boolean = true): Parsed {
        val result = AcEvoLogParseAccumulator(
            includePenalties = includePenalties,
            playerCarUuid = playerCarUuid,
            uuidToCarMap = uuidToCarMap,
            penaltyGroupId = penaltyGroupId,
            penaltyGroupMs = penaltyGroupMs,
        ).parse(lines)
        playerCarUuid = result.playerCarUuid
        penaltyGroupId = result.penaltyGroupId
        penaltyGroupMs = result.penaltyGroupMs
        return result.parsed
    }

    fun chooseCarModel(p: Parsed, lastInfo: EvoFileInfo): String? {
        val candidate = p.carModel?.takeIf { it.isNotBlank() } ?: return null

        val alreadySet = !lastInfo.carModel.isNullOrBlank()

        if (p.carSource.ordinal > carSource.ordinal) {
            carSource = p.carSource
            return candidate
        }

        if (!alreadySet) {
            carSource = maxOf(carSource, p.carSource)
            return candidate
        }

        return null
    }

    fun chooseSessionType(p: Parsed, lastInfo: EvoFileInfo): EvoSessionType? {
        val candidate = p.sessionType ?: return null
        val candidateSource = p.sessionTypeSource
        val alreadySet = lastInfo.sessionType != EvoSessionType.UNKNOWN

        if (candidateSource.ordinal > sessionTypeSource.ordinal) {
            sessionTypeSource = candidateSource
            return candidate
        }

        if (!alreadySet || candidateSource == sessionTypeSource) {
            sessionTypeSource = maxOf(sessionTypeSource, candidateSource)
            return candidate
        }

        return null
    }

    fun resolveTrackId(p: Parsed, lastInfo: EvoFileInfo): ResolvedTrack {
        val selectedCandidate = buildTrackCandidates(p).selectBestCandidate()
        val resolvedTrack = resolveTrackCandidate(
            candidate = selectedCandidate,
            lastInfo = lastInfo,
        )
        trackIdSource = resolvedTrack.source
        return resolvedTrack
    }

    fun buildDisplayName(physicsName: String?, layout: String?): String? {
        val n = physicsName?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val l = layout?.trim()?.takeIf { it.isNotBlank() } ?: return n
        return "$n ${l.uppercase()}"
    }

    private fun buildTrackCandidate(
        base: String?,
        preferredLayout: String?,
        fallbackLayout: String? = null,
    ): CandidateTrack {
        val normalizedBase = base?.takeIf { it.isNotBlank() } ?: return CandidateTrack()
        val rawLayout = preferredLayout?.takeIf { it.isNotBlank() } ?: fallbackLayout?.takeIf { it.isNotBlank() }
        val normalizedLayout = TrackIdNormalizer.normalizeLayoutId(rawLayout)
        val normalizedId = TrackIdNormalizer.normalize(track = normalizedBase, layout = normalizedLayout)
            .takeIf { it.isNotBlank() } ?: return CandidateTrack()
        return CandidateTrack(
            id = normalizedId,
            layout = normalizedLayout,
        )
    }

    private fun isTrackPrecisionDowngrade(stableId: String, candidateId: String): Boolean {
        if (stableId == candidateId) return false

        val stableLayout = stableId.substringAfterLast('_', missingDelimiterValue = "")
        val candidateLayout = candidateId.substringAfterLast('_', missingDelimiterValue = "")
        if (stableLayout.isBlank() || candidateLayout.isBlank() || stableLayout != candidateLayout) {
            return false
        }

        val stableBase = stableId.removeSuffix("_$stableLayout")
        val candidateBase = candidateId.removeSuffix("_$candidateLayout")
        if (stableBase.isBlank() || candidateBase.isBlank()) return false

        return stableBase == candidateBase || stableBase.startsWith("${candidateBase}_")
    }

    private fun areEquivalentTrackAliases(
        stableId: String,
        stableLayout: String?,
        candidateId: String,
        candidateLayout: String?,
    ): Boolean = TrackIdentityAliasMatcher.areEquivalent(
        trackId = stableId,
        layoutId = stableLayout ?: candidateLayout,
        otherTrackId = candidateId,
        otherLayoutId = candidateLayout ?: stableLayout,
    )

    private fun strongerTrackIdSource(first: TrackIdSource, second: TrackIdSource): TrackIdSource = when {
        trackIdSourcePriority(first) >= trackIdSourcePriority(second) -> first
        else -> second
    }

    private fun trackIdSourcePriority(source: TrackIdSource): Int = when (source) {
        TrackIdSource.NONE -> 0
        TrackIdSource.GAME_STARTED -> 1
        TrackIdSource.SLUG -> 2
        TrackIdSource.CONTAINER -> 3
    }

    private data class TrackCandidates(
        val slug: CandidateTrack,
        val container: CandidateTrack,
        val layoutFile: CandidateTrack,
        val gameStarted: CandidateTrack,
        val physics: CandidateTrack,
    )

    private data class SelectedTrackCandidate(
        val id: String? = null,
        val layout: String? = null,
        val source: TrackIdSource = TrackIdSource.NONE,
    )

    private fun AcEvoLogParser.buildTrackCandidates(parsed: Parsed): TrackCandidates {
        val layoutFromSlug = parsed.slugLayout?.takeIf(String::isNotBlank)
        val layoutFromContainer = parsed.containerLayout?.takeIf(String::isNotBlank)?.let(::mapContainerLayout)
        val layoutFromDynamic = parsed.dynamicTrackLayout?.takeIf(String::isNotBlank)?.let(::mapContainerLayout)
        val layoutFromLayoutFile = parsed.layoutFileLayout?.takeIf(String::isNotBlank)?.let(::mapContainerLayout)
        val fallbackLayout = layoutFromSlug ?: layoutFromContainer ?: layoutFromDynamic ?: layoutFromLayoutFile

        return TrackCandidates(
            slug = buildTrackCandidate(
                base = parsed.slugBase?.takeIf(String::isNotBlank),
                preferredLayout = layoutFromSlug,
                fallbackLayout = layoutFromContainer ?: layoutFromDynamic ?: layoutFromLayoutFile,
            ),
            container = buildTrackCandidate(
                base = parsed.containerFolder?.takeIf(String::isNotBlank)
                    ?: parsed.dynamicTrackFolder?.takeIf(String::isNotBlank),
                preferredLayout = layoutFromContainer ?: layoutFromDynamic,
                fallbackLayout = layoutFromSlug ?: layoutFromLayoutFile,
            ),
            layoutFile = buildTrackCandidate(
                base = parsed.layoutFileFolder?.takeIf(String::isNotBlank),
                preferredLayout = layoutFromLayoutFile,
                fallbackLayout = layoutFromSlug ?: layoutFromContainer ?: layoutFromDynamic,
            ),
            gameStarted = buildTrackCandidate(
                base = parsed.gameStartedTrackName?.takeIf(String::isNotBlank),
                preferredLayout = fallbackLayout,
            ),
            physics = buildTrackCandidate(
                base = parsed.physicsTrackName?.takeIf(String::isNotBlank),
                preferredLayout = fallbackLayout,
            ),
        )
    }

    private fun TrackCandidates.selectBestCandidate(): SelectedTrackCandidate = when {
        container.id != null -> container.toSelectedTrackCandidate(TrackIdSource.CONTAINER)
        layoutFile.id != null -> layoutFile.toSelectedTrackCandidate(TrackIdSource.CONTAINER)
        slug.id != null -> slug.toSelectedTrackCandidate(TrackIdSource.SLUG)
        gameStarted.id != null -> gameStarted.toSelectedTrackCandidate(TrackIdSource.GAME_STARTED)
        physics.id != null -> physics.toSelectedTrackCandidate(TrackIdSource.NONE)
        else -> SelectedTrackCandidate()
    }

    private fun CandidateTrack.toSelectedTrackCandidate(source: TrackIdSource): SelectedTrackCandidate =
        SelectedTrackCandidate(
            id = id,
            layout = layout,
            source = source,
        )

    private fun AcEvoLogParser.resolveTrackCandidate(
        candidate: SelectedTrackCandidate,
        lastInfo: EvoFileInfo,
    ): ResolvedTrack {
        val stableId = lastInfo.trackId?.takeIf(String::isNotBlank)
        val stableLayout = lastInfo.layoutId?.takeIf(String::isNotBlank)
        val stableSource = trackIdSource
        if (stableId != null && stableLayout != null && candidate.layout.isNullOrBlank()) {
            return ResolvedTrack(
                trackId = stableId,
                layout = stableLayout,
                source = stableSource,
            )
        }

        val effectiveId = chooseEffectiveTrackId(
            stableId = stableId,
            stableLayout = stableLayout,
            stableSource = stableSource,
            candidate = candidate,
        )
        val effectiveSource = if (effectiveId == stableId) {
            strongerTrackIdSource(stableSource, candidate.source)
        } else {
            candidate.source
        }
        return ResolvedTrack(
            trackId = effectiveId,
            layout = if (effectiveId == candidate.id) candidate.layout else lastInfo.layoutId,
            source = effectiveSource,
        )
    }

    private fun AcEvoLogParser.chooseEffectiveTrackId(
        stableId: String?,
        stableLayout: String?,
        stableSource: TrackIdSource,
        candidate: SelectedTrackCandidate,
    ): String? {
        val candidateId = candidate.id
        if (candidateId.isNullOrBlank()) return stableId
        if (stableId.isNullOrBlank()) return candidateId
        if (candidateId == stableId) return stableId
        if (isTrackPrecisionDowngrade(stableId = stableId, candidateId = candidateId)) return stableId

        val stableSourcePriority = trackIdSourcePriority(stableSource)
        val candidateSourcePriority = trackIdSourcePriority(candidate.source)
        val keepStableAlias = areEquivalentTrackAliases(
            stableId = stableId,
            stableLayout = stableLayout,
            candidateId = candidateId,
            candidateLayout = candidate.layout,
        ) && stableSourcePriority >= candidateSourcePriority

        return if (keepStableAlias || candidateSourcePriority < stableSourcePriority) {
            stableId
        } else {
            candidateId
        }
    }
}
