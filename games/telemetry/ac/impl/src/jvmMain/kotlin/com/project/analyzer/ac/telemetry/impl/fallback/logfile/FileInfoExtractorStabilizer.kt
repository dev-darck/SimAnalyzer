package com.project.analyzer.ac.telemetry.impl.fallback.logfile

import com.project.analyzer.ac.telemetry.impl.di.Extractor
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoFileInfo
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoSessionType
import com.project.analyzer.api.di.SessionScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Duration.Companion.milliseconds

@Inject
@SingleIn(SessionScope::class)
class FileInfoExtractorStabilizer(
    @param:Extractor
    private val upstream: EvoFileInfoSource,
) : EvoFileInfoSource {

    private val tuning: Tuning = Tuning()

    data class Tuning(
        val confirmCount: Int = 3,
        val promoteMs: Long = 250.milliseconds.inWholeMilliseconds,

        val sessionPromoteMs: Long = 250.milliseconds.inWholeMilliseconds,
        val sessionConfirmCount: Int = 3,

        val practicePromoteMs: Long = 1500.milliseconds.inWholeMilliseconds,
        val practiceConfirmCount: Int = 10,

        val uuidPromoteMs: Long = 500.milliseconds.inWholeMilliseconds,
        val uuidConfirmCount: Int = 5,
    )

    private var stable: EvoFileInfo = EvoFileInfo()
    private var hasStable = false

    private val trackId = StableStringField()
    private val layoutId = StableStringField()
    private val trackName = StableStringField()
    private val carModel = StableStringField()
    private val driverName = StableStringField()
    private val driverSteamId = StableStringField()
    private val playerCarUuid = StableStringField()

    private val clockMs: () -> Long = System::currentTimeMillis

    private val sessionType = StableSessionTypeField()

    override fun poll(): EvoFileInfo {
        val raw = upstream.poll()
        val now = clockMs()

        if (!hasStable) {
            initFrom(raw, now)
            return stable
        }

        val epochChanged = raw.sessionEpoch != stable.sessionEpoch
        if (epochChanged) {
            stable = stable.copy(sessionEpoch = raw.sessionEpoch)

            resetPendingFromStable(now)

            if (!raw.hasPenalty) {
                stable = stable.copy(
                    hasPenalty = false,
                    penaltyId = null,
                    penaltyReason = null,
                    penaltyTimestamp = null
                )
            }
        }

        stable = applyPenalty(stable, raw)

        val stType = sessionType.update(
            stable = stable.sessionType,
            raw = raw.sessionType,
            nowMs = now,
            tuning = tuning
        )

        val stTrackId = trackId.update(stable.trackId, raw.trackId, now, tuning.confirmCount, tuning.promoteMs)
        val stLayoutId = layoutId.update(stable.layoutId, raw.layoutId, now, tuning.confirmCount, tuning.promoteMs)
        val stTrackName = trackName.update(stable.trackName, raw.trackName, now, tuning.confirmCount, tuning.promoteMs)
        val stCarModel = carModel.update(stable.carModel, raw.carModel, now, tuning.confirmCount, tuning.promoteMs)
        val stDriverName =
            driverName.update(stable.driverName, raw.driverName, now, tuning.confirmCount, tuning.promoteMs)
        val stDriverSteamId =
            driverSteamId.update(stable.driverSteamId, raw.driverSteamId, now, tuning.confirmCount, tuning.promoteMs)

        val stUuid = playerCarUuid.update(
            stable.playerCarUuid,
            raw.playerCarUuid,
            now,
            tuning.uuidConfirmCount,
            tuning.uuidPromoteMs
        )

        stable = stable.copy(
            // важное: epoch уже обновлён выше
            sessionType = stType,
            trackId = stTrackId,
            layoutId = stLayoutId,
            trackName = stTrackName,
            carModel = stCarModel,
            driverName = stDriverName,
            driverSteamId = stDriverSteamId,
            playerCarUuid = stUuid
        )

        return stable
    }

    override fun clearPenalty() {
        upstream.clearPenalty()
        stable = stable.copy(
            hasPenalty = false,
            penaltyId = null,
            penaltyReason = null,
            penaltyTimestamp = null
        )
    }

    override fun clear() {
        upstream.clear()
        resetAll()
    }

    private fun initFrom(raw: EvoFileInfo, nowMs: Long) {
        hasStable = true
        stable = raw

        resetPendingFromStable(nowMs)
    }

    private fun resetAll() {
        hasStable = false
        stable = EvoFileInfo()

        trackId.reset()
        layoutId.reset()
        trackName.reset()
        carModel.reset()
        driverName.reset()
        driverSteamId.reset()
        playerCarUuid.reset()
        sessionType.reset()
    }

    private fun resetPendingFromStable(nowMs: Long) {
        trackId.observeStable(stable.trackId, nowMs)
        layoutId.observeStable(stable.layoutId, nowMs)
        trackName.observeStable(stable.trackName, nowMs)
        carModel.observeStable(stable.carModel, nowMs)
        driverName.observeStable(stable.driverName, nowMs)
        driverSteamId.observeStable(stable.driverSteamId, nowMs)
        playerCarUuid.observeStable(stable.playerCarUuid, nowMs)
        sessionType.observeStable(stable.sessionType, nowMs)
    }

    private fun applyPenalty(stable: EvoFileInfo, raw: EvoFileInfo): EvoFileInfo {
        if (raw.hasPenalty && raw.penaltyId != null) {
            return stable.copy(
                hasPenalty = true,
                penaltyId = raw.penaltyId,
                penaltyReason = raw.penaltyReason ?: stable.penaltyReason,
                penaltyTimestamp = raw.penaltyTimestamp ?: stable.penaltyTimestamp
            )
        }

        if (!raw.hasPenalty) {
            return stable.copy(
                hasPenalty = false,
                penaltyId = null,
                penaltyReason = null,
                penaltyTimestamp = null
            )
        }

        return stable
    }

    private class StableStringField {

        private var pending: String? = null
        private var pendingSinceMs: Long = 0L
        private var pendingCount: Int = 0

        fun reset() {
            pending = null
            pendingSinceMs = 0L
            pendingCount = 0
        }

        fun observeStable(stable: String?, nowMs: Long) {
            pending = stable?.trim()?.takeIf { it.isNotBlank() }
            pendingSinceMs = nowMs
            pendingCount = 0
        }

        fun update(
            stable: String?,
            raw: String?,
            nowMs: Long,
            confirmCount: Int,
            promoteMs: Long
        ): String? {
            val s = stable?.trim()?.takeIf { it.isNotBlank() }
            val r = raw?.trim()?.takeIf { it.isNotBlank() }

            if (r == null) return s
            if (s == r) {
                pending = null
                pendingCount = 0
                return s
            }

            if (pending != r) {
                pending = r
                pendingSinceMs = nowMs
                pendingCount = 1
                return s
            }

            pendingCount++

            val timeOk = (nowMs - pendingSinceMs) >= promoteMs
            val countOk = pendingCount >= confirmCount

            return if (timeOk || countOk) {
                pending = null
                pendingCount = 0
                r
            } else {
                s
            }
        }
    }

    private class StableSessionTypeField {

        private var pending: EvoSessionType? = null
        private var pendingSinceMs: Long = 0L
        private var pendingCount: Int = 0

        fun reset() {
            pending = null
            pendingSinceMs = 0L
            pendingCount = 0
        }

        fun observeStable(stable: EvoSessionType, nowMs: Long) {
            pending = stable
            pendingSinceMs = nowMs
            pendingCount = 0
        }

        fun update(
            stable: EvoSessionType,
            raw: EvoSessionType,
            nowMs: Long,
            tuning: Tuning
        ): EvoSessionType {
            if (raw == EvoSessionType.UNKNOWN) return stable

            if (raw == stable) {
                pending = null
                pendingCount = 0
                return stable
            }

            val isPracticeDemotion = (raw == EvoSessionType.PRACTICE && stable != EvoSessionType.UNKNOWN)

            val requiredMs = if (isPracticeDemotion) tuning.practicePromoteMs else tuning.sessionPromoteMs
            val requiredCount = if (isPracticeDemotion) tuning.practiceConfirmCount else tuning.sessionConfirmCount

            if (pending != raw) {
                pending = raw
                pendingSinceMs = nowMs
                pendingCount = 1
                return stable
            }

            pendingCount++

            val timeOk = (nowMs - pendingSinceMs) >= requiredMs
            val countOk = pendingCount >= requiredCount

            return if (timeOk || countOk) {
                pending = null
                pendingCount = 0
                raw
            } else {
                stable
            }
        }
    }
}
