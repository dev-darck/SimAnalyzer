package com.project.analyzer.ac.telemetry.impl.fallback

import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoFileInfo
import kotlin.time.Duration.Companion.milliseconds

internal class IdentityResetDetector {

    private var stableTrackId: String? = null
    private var stableCarModel: String? = null
    private var pendingTrackId: String? = null
    private var pendingCarModel: String? = null
    private var pendingSinceNs: Long = 0L

    fun reset() {
        stableTrackId = null
        stableCarModel = null
        pendingTrackId = null
        pendingCarModel = null
        pendingSinceNs = 0L
    }

    fun observe(info: EvoFileInfo, nowNs: Long) {
        val trackId = info.trackId?.trim().takeIf { !it.isNullOrBlank() }
        val carModel = info.carModel?.trim().takeIf { !it.isNullOrBlank() }
        stableTrackId = trackId ?: stableTrackId
        stableCarModel = carModel ?: stableCarModel
        pendingTrackId = stableTrackId
        pendingCarModel = stableCarModel
        pendingSinceNs = nowNs
    }

    fun update(info: EvoFileInfo, nowNs: Long): IdentityChange {
        val newTrackId = info.trackId?.trim().takeIf { !it.isNullOrBlank() }
        val newCarModel = info.carModel?.trim().takeIf { !it.isNullOrBlank() }
        if (newTrackId == null && newCarModel == null) return IdentityChange.NONE

        val candidateTrackId = newTrackId ?: stableTrackId
        val candidateCarModel = newCarModel ?: stableCarModel
        if (candidateTrackId != pendingTrackId || candidateCarModel != pendingCarModel) {
            pendingTrackId = candidateTrackId
            pendingCarModel = candidateCarModel
            pendingSinceNs = nowNs
            return IdentityChange.NONE
        }

        if (!isIdentityDebounced(nowNs)) return IdentityChange.NONE

        return resolveIdentityChange(
            candidateTrackId = candidateTrackId,
            candidateCarModel = candidateCarModel,
        )
    }

    private fun isIdentityDebounced(nowNs: Long): Boolean = (nowNs - pendingSinceNs) >= IDENTITY_DEBOUNCE_NS

    private fun resolveIdentityChange(candidateTrackId: String?, candidateCarModel: String?): IdentityChange {
        val trackChanged = candidateTrackId.hasChangedFrom(stableTrackId)
        val carChanged = candidateCarModel.hasChangedFrom(stableCarModel)
        adoptMissingStableIdentity(
            candidateTrackId = candidateTrackId,
            candidateCarModel = candidateCarModel,
        )

        if (!trackChanged && !carChanged) {
            stableTrackId = candidateTrackId ?: stableTrackId
            stableCarModel = candidateCarModel ?: stableCarModel
            return IdentityChange.NONE
        }

        stableTrackId = candidateTrackId
        stableCarModel = candidateCarModel
        return when {
            trackChanged && carChanged -> IdentityChange.BOTH
            trackChanged -> IdentityChange.TRACK
            else -> IdentityChange.CAR
        }
    }

    private fun adoptMissingStableIdentity(candidateTrackId: String?, candidateCarModel: String?) {
        if (stableTrackId == null && candidateTrackId != null) {
            stableTrackId = candidateTrackId
        }
        if (stableCarModel == null && candidateCarModel != null) {
            stableCarModel = candidateCarModel
        }
    }

    private fun String?.hasChangedFrom(previous: String?): Boolean =
        this != null && previous != null && this != previous

    private companion object {

        val IDENTITY_DEBOUNCE_NS = 300.milliseconds.inWholeNanoseconds
    }
}
