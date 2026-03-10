package com.project.analyzer.telemetry.recording.impl.controller

import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionConfig
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionSettings
import com.project.analyzer.telemetry.recording.impl.file.META_FILE_NAME
import com.project.analyzer.telemetry.recording.impl.file.model.SessionMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TelemetryRecordingUnsavedSessionCleanupTest {

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    @Test
    fun `cleanup keeps completed unsaved sessions`() = runTest {
        val root = createTempDirectory("telemetry-cleanup-keep").toFile()
        try {
            val cleanup = TelemetryRecordingUnsavedSessionCleanup(
                settings = TestSettings(root.absolutePath),
                json = json,
                ioDispatcher = StandardTestDispatcher(testScheduler),
            )
            val completedDir = writeSessionDir(
                root = root,
                dirName = "completed",
                metadata = sessionMetadata(sessionId = 1L, endedAtMs = 2_000L, isSaved = false),
            )

            cleanup.cleanup(reason = "test")

            assertTrue(completedDir.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `cleanup removes incomplete unsaved sessions`() = runTest {
        val root = createTempDirectory("telemetry-cleanup-drop").toFile()
        try {
            val cleanup = TelemetryRecordingUnsavedSessionCleanup(
                settings = TestSettings(root.absolutePath),
                json = json,
                ioDispatcher = StandardTestDispatcher(testScheduler),
            )
            val incompleteDir = writeSessionDir(
                root = root,
                dirName = "incomplete",
                metadata = sessionMetadata(sessionId = 2L, endedAtMs = null, isSaved = false),
            )

            cleanup.cleanup(reason = "test")

            assertFalse(incompleteDir.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    private fun writeSessionDir(root: File, dirName: String, metadata: SessionMetadata): File {
        val dir = File(root, dirName).apply { mkdirs() }
        File(dir, META_FILE_NAME).writeText(json.encodeToString(SessionMetadata.serializer(), metadata))
        return dir
    }

    private fun sessionMetadata(
        sessionId: Long,
        endedAtMs: Long?,
        isSaved: Boolean,
    ): SessionMetadata = SessionMetadata(
        sessionId = sessionId,
        gameId = "ac",
        sessionType = "PRACTICE",
        carModel = "ks_bmw_m4_gt3",
        trackId = "brands_hatch_indy",
        startedAtMs = 1_000L,
        endedAtMs = endedAtMs,
        isSaved = isSaved,
        dataSource = "NATIVE",
        payloadType = "ac_shm_v1",
        payloadSize = 3208,
        samplingRateHz = 100,
        frameCount = 0,
        receivedFrames = 0,
        droppedFrames = 0,
        firstTimestampNs = null,
        lastTimestampNs = null,
        fileVersion = 2,
        indexVersion = 1,
        indexRecordSize = 64,
        indexFields = emptyList(),
        framesFile = "frames.bin",
        indexFile = "index.bin",
        eventsFile = "events.jsonl",
    )

    private class TestSettings(
        private val storageLocation: String,
    ) : TelemetryAcquisitionSettings {

        private val config = TelemetryAcquisitionConfig(
            samplingRateHz = 100,
            storageLocation = storageLocation,
            recordingEnabled = true,
            maxRecordedLaps = 0,
        )

        override fun observeConfig(): Flow<TelemetryAcquisitionConfig> = flowOf(config)

        override suspend fun currentConfig(): TelemetryAcquisitionConfig = config
    }
}
