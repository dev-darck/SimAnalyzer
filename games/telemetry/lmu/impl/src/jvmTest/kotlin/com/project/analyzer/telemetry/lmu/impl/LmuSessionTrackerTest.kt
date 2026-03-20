package com.project.analyzer.telemetry.lmu.impl

import com.project.analyzer.telemetry.api.contract.SessionEndReason
import com.project.analyzer.telemetry.api.contract.SessionType
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.telemetry.api.model.lap.LapFrame
import com.project.analyzer.telemetry.api.model.session.CarInfo
import com.project.analyzer.telemetry.api.model.session.SessionFrame
import com.project.analyzer.telemetry.api.model.session.TrackInfo
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class LmuSessionTrackerTest {

    @Test
    fun `track identity change while connected starts a new session`() = runBlocking {
        val tracker = LmuSessionTracker()
        val events = mutableListOf<TelemetryLifecycleEvent>()

        tracker.onFrame(
            frame(
                sessionType = SessionType.PRACTICE,
                currentLap = 4,
                trackId = "watkins_glen_gp",
                layoutId = "gp",
                timestampNs = 1_000_000_000L,
            ),
            events::add,
        )

        events.clear()

        tracker.onFrame(
            frame(
                sessionType = SessionType.PRACTICE,
                currentLap = 1,
                trackId = "road_atlanta_gp",
                layoutId = "gp",
                timestampNs = 2_000_000_000L,
            ),
            events::add,
        )

        val ended = events.filterIsInstance<TelemetryLifecycleEvent.SessionEnded>().single()
        assertEquals(SessionEndReason.REPLACED_BY_NEW_SESSION, ended.reason)

        val started = events.filterIsInstance<TelemetryLifecycleEvent.SessionStarted>().single()
        assertEquals(2L, started.session.sessionId)
        assertEquals("road_atlanta_gp", started.session.trackId)

        val lapStarted = events.filterIsInstance<TelemetryLifecycleEvent.LapStarted>().single()
        assertEquals(1, lapStarted.lapNumber)
    }

    @Test
    fun `car identity change while connected starts a new session`() = runBlocking {
        val tracker = LmuSessionTracker()
        val events = mutableListOf<TelemetryLifecycleEvent>()

        tracker.onFrame(
            frame(
                sessionType = SessionType.PRACTICE,
                currentLap = 3,
                carModel = "bmw_m4_gt3",
                carId = 101,
                timestampNs = 1_000_000_000L,
            ),
            events::add,
        )

        events.clear()

        tracker.onFrame(
            frame(
                sessionType = SessionType.PRACTICE,
                currentLap = 1,
                carModel = "porsche_992_gt3r",
                carId = 202,
                timestampNs = 2_000_000_000L,
            ),
            events::add,
        )

        val ended = events.filterIsInstance<TelemetryLifecycleEvent.SessionEnded>().single()
        assertEquals(SessionEndReason.REPLACED_BY_NEW_SESSION, ended.reason)

        val started = events.filterIsInstance<TelemetryLifecycleEvent.SessionStarted>().single()
        assertEquals(2L, started.session.sessionId)
        assertEquals("porsche_992_gt3r", started.session.carModel)
        assertEquals(202, started.session.carId)
    }

    private fun frame(
        sessionType: SessionType,
        currentLap: Int,
        carModel: String = "bmw_m4_gt3",
        carId: Int? = 101,
        trackId: String = "watkins_glen_gp",
        layoutId: String? = "gp",
        timestampNs: Long,
    ): TelemetryFrame = TelemetryFrame(
        session = SessionFrame(
            sessionType = sessionType,
            car = CarInfo(
                carModel = carModel,
                carId = carId,
            ),
            track = TrackInfo(
                trackId = trackId,
                layoutId = layoutId,
            ),
        ),
        lap = LapFrame(currentLapIndex = currentLap),
        timestampNs = timestampNs,
    )
}
