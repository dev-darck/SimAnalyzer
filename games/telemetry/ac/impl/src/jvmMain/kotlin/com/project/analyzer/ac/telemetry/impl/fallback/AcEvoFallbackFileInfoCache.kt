package com.project.analyzer.ac.telemetry.impl.fallback

import com.project.analyzer.ac.telemetry.impl.fallback.logfile.EvoFileInfoSource
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoFileInfo
import com.project.analyzer.utils.logger.logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal class AcEvoFallbackFileInfoCache(
    private val fileInfoExtractor: EvoFileInfoSource,
    ioDispatcher: CoroutineDispatcher,
    private val useAsyncFilePolling: Boolean,
) {

    private val logger = logger()
    private val fileInfoScope = CoroutineScope(
        SupervisorJob() + ioDispatcher.limitedParallelism(1, "AcEvoFileInfoPoller"),
    )
    private val fileInfoLock = Any()
    private val seenPenaltyIds = ArrayDeque<String>(PENALTY_DEDUP_CAPACITY)
    private val seenPenaltySet = HashSet<String>(PENALTY_DEDUP_CAPACITY * 2)

    @Volatile
    private var lastFilePollNs: Long = 0L

    @Volatile
    private var cachedInfo: EvoFileInfo = EvoFileInfo()

    @Volatile
    private var fileInfoPollScheduled: Boolean = false

    @Volatile
    private var fileInfoGeneration: Long = 0L

    fun clear() {
        fileInfoGeneration += 1L
        fileInfoPollScheduled = false
        lastFilePollNs = 0L
        cachedInfo = EvoFileInfo()
        clearPenaltyDedup()
        if (useAsyncFilePolling) {
            fileInfoScope.launch {
                synchronized(fileInfoLock) {
                    fileInfoExtractor.clear()
                }
            }
        } else {
            synchronized(fileInfoLock) {
                fileInfoExtractor.clear()
            }
        }
    }

    fun poll(nowNs: Long): EvoFileInfo {
        if (!useAsyncFilePolling) {
            if (lastFilePollNs == 0L || (nowNs - lastFilePollNs) >= FILE_POLL_INTERVAL_NS) {
                refreshFileInfoNow(nowNs)
            }
            return cachedInfo
        }
        if (lastFilePollNs == 0L && !fileInfoPollScheduled) {
            refreshFileInfoNow(nowNs)
            return cachedInfo
        }
        if ((nowNs - lastFilePollNs) >= FILE_POLL_INTERVAL_NS) {
            scheduleFileInfoRefresh(nowNs)
        }
        return cachedInfo
    }

    fun consumePenalty(info: EvoFileInfo): Boolean {
        val penaltyId = info.penaltyId ?: return false
        val isNewPenalty = seenPenaltySet.add(penaltyId)
        if (isNewPenalty) {
            seenPenaltyIds.addLast(penaltyId)
            while (seenPenaltyIds.size > PENALTY_DEDUP_CAPACITY) {
                val removed = seenPenaltyIds.removeFirst()
                seenPenaltySet.remove(removed)
            }
        }
        clearPenaltyAsync()
        return isNewPenalty
    }

    fun clearPenaltyDedup() {
        seenPenaltyIds.clear()
        seenPenaltySet.clear()
    }

    private fun refreshFileInfoNow(nowNs: Long) {
        lastFilePollNs = nowNs
        cachedInfo = synchronized(fileInfoLock) {
            fileInfoExtractor.poll()
        }
    }

    private fun scheduleFileInfoRefresh(nowNs: Long) {
        if (fileInfoPollScheduled) return

        fileInfoPollScheduled = true
        lastFilePollNs = nowNs
        val generation = fileInfoGeneration

        fileInfoScope.launch {
            val polled = runCatching {
                synchronized(fileInfoLock) {
                    fileInfoExtractor.poll()
                }
            }.onFailure { error ->
                logger.warn(error) { "FallbackSHM file info poll failed" }
            }.getOrNull()

            if (polled != null && fileInfoGeneration == generation) {
                cachedInfo = polled
            }
            fileInfoPollScheduled = false
        }
    }

    private fun clearPenaltyAsync() {
        cachedInfo = cachedInfo.copy(
            hasPenalty = false,
            penaltyId = null,
            penaltyReason = null,
            penaltyTimestamp = null,
        )
        if (!useAsyncFilePolling) {
            synchronized(fileInfoLock) {
                fileInfoExtractor.clearPenalty()
            }
            return
        }
        val generation = fileInfoGeneration
        fileInfoScope.launch {
            if (fileInfoGeneration != generation) return@launch
            synchronized(fileInfoLock) {
                fileInfoExtractor.clearPenalty()
            }
        }
    }

    private companion object {

        const val PENALTY_DEDUP_CAPACITY = 32
        val FILE_POLL_INTERVAL_NS = 100.milliseconds.inWholeNanoseconds
    }
}
