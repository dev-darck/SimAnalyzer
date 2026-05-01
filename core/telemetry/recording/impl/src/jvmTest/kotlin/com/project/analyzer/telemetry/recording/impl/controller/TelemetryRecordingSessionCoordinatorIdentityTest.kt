package com.project.analyzer.telemetry.recording.impl.controller

import com.project.analyzer.telemetry.api.contract.SessionInfo
import com.project.analyzer.telemetry.api.contract.SessionType
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.telemetry.api.model.session.CarInfo
import com.project.analyzer.telemetry.api.model.session.SessionFrame
import com.project.analyzer.telemetry.api.model.session.TrackInfo
import com.project.analyzer.telemetry.recording.api.payload.TelemetryFramePayload
import com.project.analyzer.telemetry.recording.api.recording.TelemetryRecordingFrameSnapshot
import com.project.analyzer.telemetry.recording.api.recorder.TelemetryRecorder
import com.project.analyzer.telemetry.recording.api.recording.TelemetryRecordingSample
import com.project.analyzer.telemetry.recording.api.session.TelemetrySessionDescriptor
import com.project.analyzer.telemetry.recording.api.session.TelemetrySessionUpdate
import com.project.analyzer.telemetry.recording.impl.index.TelemetryFrameIndexBuilder
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TelemetryRecordingSessionCoordinatorIdentityTest {

    @Test
    fun `session recorder keeps ids stable and names human readable`() = runTest {
        val recorder = FakeTelemetryRecorder()
        val coordinator = TelemetryRecordingSessionCoordinator(
            recorder = recorder,
            frameIndexBuilder = TelemetryFrameIndexBuilder(),
        )

        coordinator.handle(
            TelemetryRecordingInput.TelemetryRecordingEventInput(
                TelemetryLifecycleEvent.SessionStarted(
                    SessionInfo(
                        sessionId = 42L,
                        sessionType = SessionType.PRACTICE,
                        carModel = "ks_bmw_m4_gt3",
                        trackId = "brands_hatch_indy",
                    ),
                ),
            ),
        )
        coordinator.handle(
            TelemetryRecordingInput.TelemetryRecordingSampleInput(
                sample(
                    sessionId = 42L,
                    frame = TelemetryFrame(
                        session = SessionFrame(
                            track = TrackInfo(
                                trackId = "brands_hatch_indy",
                                trackName = "Brands Hatch Indy",
                                layoutId = "indy",
                            ),
                            car = CarInfo(
                                carModel = "ks_bmw_m4_gt3",
                                carName = "BMW M4 GT3",
                            ),
                        ),
                    ),
                ),
            ),
        )

        val started = recorder.startedDescriptors.single()
        assertEquals("brands_hatch_indy", started.trackId)
        assertEquals("Brands Hatch Indy", started.trackName)
        assertEquals("indy", started.layoutId)
        assertEquals("ks_bmw_m4_gt3", started.carModel)
        assertEquals("BMW M4 GT3", started.carName)
    }

    @Test
    fun `session recorder backfills names without changing ids`() = runTest {
        val recorder = FakeTelemetryRecorder()
        val coordinator = TelemetryRecordingSessionCoordinator(
            recorder = recorder,
            frameIndexBuilder = TelemetryFrameIndexBuilder(),
        )

        coordinator.handle(
            TelemetryRecordingInput.TelemetryRecordingEventInput(
                TelemetryLifecycleEvent.SessionStarted(
                    SessionInfo(
                        sessionId = 7L,
                        sessionType = SessionType.PRACTICE,
                        carModel = "ks_bmw_m4_gt3",
                        trackId = "brands_hatch_indy",
                    ),
                ),
            ),
        )
        coordinator.handle(
            TelemetryRecordingInput.TelemetryRecordingSampleInput(
                sample(sessionId = 7L),
            ),
        )
        coordinator.handle(
            TelemetryRecordingInput.TelemetryRecordingSampleInput(
                sample(
                    sessionId = 7L,
                    timestampNs = 8_000_000L,
                    frame = TelemetryFrame(
                        session = SessionFrame(
                            track = TrackInfo(
                                trackId = "brands_hatch_indy",
                                trackName = "Brands Hatch Indy",
                                layoutId = "indy",
                            ),
                            car = CarInfo(
                                carModel = "ks_bmw_m4_gt3",
                                carName = "BMW M4 GT3",
                            ),
                        ),
                    ),
                ),
            ),
        )

        val started = recorder.startedDescriptors.single()
        val update = recorder.sessionUpdates.single()
        assertEquals("brands_hatch_indy", started.trackId)
        assertEquals(null, started.trackName)
        assertEquals(null, started.layoutId)
        assertEquals("Brands Hatch Indy", update.trackName)
        assertEquals("indy", update.layoutId)
        assertEquals("BMW M4 GT3", update.carName)
        assertEquals(null, update.trackId)
        assertEquals(null, update.carModel)
    }

    private fun sample(
        sessionId: Long,
        timestampNs: Long = sessionId * 1_000_000L,
        frame: TelemetryFrame = TelemetryFrame(),
    ): TelemetryRecordingSample = TelemetryRecordingSample(
        sessionId = sessionId,
        timestampNs = timestampNs,
        frameId = sessionId,
        gameId = "ac",
        dataSourceId = 1,
        dataSource = "NATIVE",
        payloadType = "ac_shm_v1",
        payload = byteArrayOf(1, 2, 3),
        frame = TelemetryRecordingFrameSnapshot.from(frame),
    )

    private class FakeTelemetryRecorder : TelemetryRecorder {

        val startedDescriptors = mutableListOf<TelemetrySessionDescriptor>()
        val sessionUpdates = mutableListOf<TelemetrySessionUpdate>()

        override suspend fun startSession(descriptor: TelemetrySessionDescriptor) {
            startedDescriptors += descriptor
        }

        override suspend fun updateSession(update: TelemetrySessionUpdate) {
            sessionUpdates += update
        }

        override suspend fun recordFrame(payload: TelemetryFramePayload) = Unit

        override suspend fun pauseSession(gameId: String, sessionId: Long, reason: String?) = Unit

        override suspend fun resumeSession(gameId: String, sessionId: Long) = Unit

        override suspend fun endSession(gameId: String, sessionId: Long, reason: String?) = Unit

        override suspend fun close() = Unit
    }
}
