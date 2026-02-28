package com.project.analyzer.telemetry.recording.impl.controller

import com.project.analyzer.telemetry.api.contract.SessionEndReason
import com.project.analyzer.telemetry.api.contract.SessionInfo
import com.project.analyzer.telemetry.api.contract.SessionType
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.telemetry.recording.api.payload.TelemetryFramePayload
import com.project.analyzer.telemetry.recording.api.recorder.TelemetryRecorder
import com.project.analyzer.telemetry.recording.api.recording.TelemetryRecordingSample
import com.project.analyzer.telemetry.recording.api.session.TelemetrySessionDescriptor
import com.project.analyzer.telemetry.recording.api.session.TelemetrySessionUpdate
import com.project.analyzer.telemetry.recording.impl.index.TelemetryFrameIndexBuilder
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class TelemetryRecordingSessionCoordinatorTest {

    @Test
    fun `new practice starts a new session group while qualifying keeps weekend group`() = runTest {
        val recorder = FakeTelemetryRecorder()
        val coordinator = TelemetryRecordingSessionCoordinator(
            recorder = recorder,
            frameIndexBuilder = TelemetryFrameIndexBuilder(),
        )

        startSession(coordinator, sessionId = 1L, sessionType = SessionType.PRACTICE)
        val practiceGroup = recorder.startedDescriptors[0].sessionGroupId
        assertNotNull(practiceGroup)

        endSession(coordinator, sessionId = 1L)
        startSession(coordinator, sessionId = 2L, sessionType = SessionType.QUALIFYING)
        val qualifyingGroup = recorder.startedDescriptors[1].sessionGroupId
        assertEquals(practiceGroup, qualifyingGroup)

        endSession(coordinator, sessionId = 2L)
        startSession(coordinator, sessionId = 3L, sessionType = SessionType.PRACTICE)
        val nextPracticeGroup = recorder.startedDescriptors[2].sessionGroupId
        assertNotNull(nextPracticeGroup)
        assertNotEquals(qualifyingGroup, nextPracticeGroup)
    }

    @Test
    fun `replacement practice placeholder keeps the same weekend group`() = runTest {
        val recorder = FakeTelemetryRecorder()
        val coordinator = TelemetryRecordingSessionCoordinator(
            recorder = recorder,
            frameIndexBuilder = TelemetryFrameIndexBuilder(),
        )

        startSession(coordinator, sessionId = 1L, sessionType = SessionType.PRACTICE)
        val practiceGroup = recorder.startedDescriptors[0].sessionGroupId
        assertNotNull(practiceGroup)

        endSession(coordinator, sessionId = 1L)
        startSession(coordinator, sessionId = 2L, sessionType = SessionType.PRACTICE)
        val boundaryPracticeGroup = recorder.startedDescriptors[1].sessionGroupId
        assertEquals(practiceGroup, boundaryPracticeGroup)

        endSession(coordinator, sessionId = 2L)
        startSession(coordinator, sessionId = 3L, sessionType = SessionType.QUALIFYING)
        val qualifyingGroup = recorder.startedDescriptors[2].sessionGroupId
        assertEquals(practiceGroup, qualifyingGroup)
    }

    @Test
    fun `qualifying after main menu replacement starts a new session group`() = runTest {
        val recorder = FakeTelemetryRecorder()
        val coordinator = TelemetryRecordingSessionCoordinator(
            recorder = recorder,
            frameIndexBuilder = TelemetryFrameIndexBuilder(),
        )

        startSession(coordinator, sessionId = 1L, sessionType = SessionType.PRACTICE)
        val firstGroup = recorder.startedDescriptors[0].sessionGroupId
        assertNotNull(firstGroup)

        endSession(
            coordinator = coordinator,
            sessionId = 1L,
            reason = SessionEndReason.REPLACED_AFTER_MAIN_MENU,
        )
        startSession(coordinator, sessionId = 2L, sessionType = SessionType.QUALIFYING)

        val nextGroup = recorder.startedDescriptors[1].sessionGroupId
        assertNotNull(nextGroup)
        assertNotEquals(firstGroup, nextGroup)
    }

    private suspend fun startSession(
        coordinator: TelemetryRecordingSessionCoordinator,
        sessionId: Long,
        sessionType: SessionType,
    ) {
        coordinator.handle(
            TelemetryRecordingInput.TelemetryRecordingEventInput(
                TelemetryLifecycleEvent.SessionStarted(
                    SessionInfo(
                        sessionId = sessionId,
                        sessionType = sessionType,
                        carModel = "ks_bmw_m4_gt3",
                        trackId = "brands_hatch_indy",
                    ),
                ),
            ),
        )
        coordinator.handle(TelemetryRecordingInput.TelemetryRecordingSampleInput(sample(sessionId = sessionId)))
    }

    private suspend fun endSession(
        coordinator: TelemetryRecordingSessionCoordinator,
        sessionId: Long,
        reason: SessionEndReason = SessionEndReason.REPLACED_BY_NEW_SESSION,
    ) {
        coordinator.handle(
            TelemetryRecordingInput.TelemetryRecordingEventInput(
                TelemetryLifecycleEvent.SessionEnded(
                    sessionId = sessionId,
                    reason = reason,
                ),
            ),
        )
    }

    private fun sample(sessionId: Long): TelemetryRecordingSample = TelemetryRecordingSample(
        sessionId = sessionId,
        timestampNs = sessionId * 1_000_000L,
        frameId = sessionId,
        gameId = "ac",
        dataSourceId = 1,
        dataSource = "NATIVE",
        payloadType = "ac_shm_v1",
        payload = byteArrayOf(1, 2, 3),
        frame = TelemetryFrame(),
    )

    private class FakeTelemetryRecorder : TelemetryRecorder {
        val startedDescriptors = mutableListOf<TelemetrySessionDescriptor>()

        override suspend fun startSession(descriptor: TelemetrySessionDescriptor) {
            startedDescriptors += descriptor
        }

        override suspend fun updateSession(update: TelemetrySessionUpdate) = Unit

        override suspend fun recordFrame(payload: TelemetryFramePayload) = Unit

        override suspend fun pauseSession(gameId: String, sessionId: Long, reason: String?) = Unit

        override suspend fun resumeSession(gameId: String, sessionId: Long) = Unit

        override suspend fun endSession(gameId: String, sessionId: Long, reason: String?) = Unit

        override suspend fun close() = Unit
    }
}
