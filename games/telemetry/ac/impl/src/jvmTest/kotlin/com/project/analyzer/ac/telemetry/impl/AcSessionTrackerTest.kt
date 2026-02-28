package com.project.analyzer.ac.telemetry.impl

import com.project.analyzer.ac.telemetry.impl.internal.AcSessionRestartHint
import com.project.analyzer.ac.telemetry.impl.internal.DataSourceType
import com.project.analyzer.ac.telemetry.impl.internal.GameConnectionState
import com.project.analyzer.telemetry.api.contract.LapValidity
import com.project.analyzer.telemetry.api.contract.SessionEndReason
import com.project.analyzer.telemetry.api.contract.SessionType
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.telemetry.api.model.lap.LapFrame
import com.project.analyzer.telemetry.api.model.session.CarInfo
import com.project.analyzer.telemetry.api.model.session.SessionFrame
import com.project.analyzer.telemetry.api.model.session.TrackInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class AcSessionTrackerTest {

    @Test
    fun `menu resume with lap counter reset starts a new session`() {
        val tracker = AcSessionTracker()
        val events = mutableListOf<TelemetryLifecycleEvent>()

        tracker.onConnectionStateChanged(GameConnectionState.IN_SESSION, DataSourceType.NATIVE, events::add)
        tracker.onFrame(
            frame(
                sessionType = SessionType.RACE,
                currentLap = 6,
                completedLaps = 5,
                sessionTimeLeftSec = 120f,
                timestampNs = 1_000_000_000L,
            ),
            DataSourceType.NATIVE,
            events::add,
        )

        events.clear()

        tracker.onConnectionStateChanged(GameConnectionState.IN_MENU, DataSourceType.NATIVE, events::add)
        tracker.onConnectionStateChanged(GameConnectionState.IN_SESSION, DataSourceType.NATIVE, events::add)
        tracker.onFrame(
            frame(
                sessionType = SessionType.RACE,
                currentLap = 1,
                completedLaps = 0,
                sessionTimeLeftSec = 1_800f,
                timestampNs = 2_000_000_000L,
            ),
            DataSourceType.NATIVE,
            events::add,
        )
        tracker.onFrame(
            frame(
                sessionType = SessionType.RACE,
                currentLap = 1,
                completedLaps = 0,
                sessionTimeLeftSec = 1_799f,
                timestampNs = 3_100_000_000L,
            ),
            DataSourceType.NATIVE,
            events::add,
        )

        assertFalse(events.any { it is TelemetryLifecycleEvent.SessionResumed })

        val ended = events.filterIsInstance<TelemetryLifecycleEvent.SessionEnded>().single()
        assertEquals(SessionEndReason.REPLACED_BY_NEW_SESSION, ended.reason)

        val started = events.filterIsInstance<TelemetryLifecycleEvent.SessionStarted>().single()
        assertEquals(2L, started.session.sessionId)

        val lapStarted = events.filterIsInstance<TelemetryLifecycleEvent.LapStarted>().last()
        assertEquals(1, lapStarted.lapNumber)
    }

    @Test
    fun `tracker keeps raw lap counters without baseline normalization`() {
        val tracker = AcSessionTracker()
        val events = mutableListOf<TelemetryLifecycleEvent>()

        tracker.onConnectionStateChanged(GameConnectionState.IN_SESSION, DataSourceType.NATIVE, events::add)
        val result = tracker.onFrame(
            frame(
                sessionType = SessionType.RACE,
                currentLap = 4,
                completedLaps = 3,
                sessionTimeLeftSec = 800f,
                timestampNs = 1_000_000_000L,
            ),
            DataSourceType.NATIVE,
            events::add,
        )

        val lap = result.frame.lap
        val session = result.frame.session
        assertNotNull(lap)
        assertNotNull(session)
        assertEquals(4, lap.currentLapIndex)
        assertEquals(3, lap.completedLaps)
        assertEquals(3, session.completedLaps)
    }

    @Test
    fun `menu resume with stable lap counters keeps current session`() {
        val tracker = AcSessionTracker()
        val events = mutableListOf<TelemetryLifecycleEvent>()

        tracker.onConnectionStateChanged(GameConnectionState.IN_SESSION, DataSourceType.NATIVE, events::add)
        tracker.onFrame(
            frame(
                sessionType = SessionType.RACE,
                currentLap = 6,
                completedLaps = 5,
                sessionTimeLeftSec = 120f,
                timestampNs = 1_000_000_000L,
            ),
            DataSourceType.NATIVE,
            events::add,
        )

        events.clear()

        tracker.onConnectionStateChanged(GameConnectionState.IN_MENU, DataSourceType.NATIVE, events::add)
        tracker.onConnectionStateChanged(GameConnectionState.IN_SESSION, DataSourceType.NATIVE, events::add)
        tracker.onFrame(
            frame(
                sessionType = SessionType.RACE,
                currentLap = 6,
                completedLaps = 5,
                sessionTimeLeftSec = 115f,
                timestampNs = 2_000_000_000L,
            ),
            DataSourceType.NATIVE,
            events::add,
        )

        tracker.onFrame(
            frame(
                sessionType = SessionType.RACE,
                currentLap = 6,
                completedLaps = 5,
                sessionTimeLeftSec = 114f,
                timestampNs = 3_100_000_000L,
            ),
            DataSourceType.NATIVE,
            events::add,
        )

        assertFalse(events.any { it is TelemetryLifecycleEvent.SessionEnded })
        assertFalse(events.any { it is TelemetryLifecycleEvent.SessionStarted })
        assertEquals(1, events.filterIsInstance<TelemetryLifecycleEvent.SessionResumed>().size)
    }

    @Test
    fun `large session clock jump alone does not start new session`() {
        val tracker = AcSessionTracker()
        val events = mutableListOf<TelemetryLifecycleEvent>()

        tracker.onConnectionStateChanged(GameConnectionState.IN_SESSION, DataSourceType.NATIVE, events::add)
        tracker.onFrame(
            frame(
                sessionType = SessionType.RACE,
                currentLap = 8,
                completedLaps = 7,
                sessionTimeLeftSec = 45f,
                timestampNs = 1_000_000_000L,
            ),
            DataSourceType.NATIVE,
            events::add,
        )

        events.clear()

        tracker.onFrame(
            frame(
                sessionType = SessionType.RACE,
                currentLap = 8,
                completedLaps = 7,
                sessionTimeLeftSec = 1_200f,
                timestampNs = 2_000_000_000L,
            ),
            DataSourceType.NATIVE,
            events::add,
        )

        assertFalse(events.any { it is TelemetryLifecycleEvent.SessionEnded })
        assertFalse(events.any { it is TelemetryLifecycleEvent.SessionStarted })
    }

    @Test
    fun `session index wobble alone does not split active practice session`() {
        val tracker = AcSessionTracker()
        val events = mutableListOf<TelemetryLifecycleEvent>()

        tracker.onConnectionStateChanged(GameConnectionState.IN_SESSION, DataSourceType.NATIVE, events::add)
        tracker.onFrame(
            frame(
                sessionType = SessionType.PRACTICE,
                currentLap = 2,
                completedLaps = 1,
                sessionTimeLeftSec = 1_540f,
                sessionIndex = 2,
                timestampNs = 1_000_000_000L,
            ),
            DataSourceType.NATIVE,
            events::add,
        )

        events.clear()

        tracker.onFrame(
            frame(
                sessionType = SessionType.PRACTICE,
                currentLap = 2,
                completedLaps = 1,
                sessionTimeLeftSec = 1_535f,
                sessionIndex = 3,
                timestampNs = 2_000_000_000L,
            ),
            DataSourceType.NATIVE,
            events::add,
        )

        assertFalse(events.any { it is TelemetryLifecycleEvent.SessionEnded })
        assertFalse(events.any { it is TelemetryLifecycleEvent.SessionStarted })
    }

    @Test
    fun `main menu restart hint starts a new session even with stable counters`() {
        val tracker = AcSessionTracker()
        val events = mutableListOf<TelemetryLifecycleEvent>()

        tracker.onConnectionStateChanged(GameConnectionState.IN_SESSION, DataSourceType.NATIVE, events::add)
        tracker.onFrame(
            frame(
                sessionType = SessionType.RACE,
                currentLap = 6,
                completedLaps = 5,
                sessionTimeLeftSec = 120f,
                timestampNs = 1_000_000_000L,
            ),
            DataSourceType.NATIVE,
            emit = events::add,
        )

        events.clear()

        tracker.onConnectionStateChanged(GameConnectionState.IN_MENU, DataSourceType.NATIVE, events::add)
        tracker.onConnectionStateChanged(GameConnectionState.IN_SESSION, DataSourceType.NATIVE, events::add)
        tracker.onFrame(
            frame(
                sessionType = SessionType.RACE,
                currentLap = 6,
                completedLaps = 5,
                sessionTimeLeftSec = 1_200f,
                timestampNs = 2_000_000_000L,
            ),
            DataSourceType.NATIVE,
            restartHint = AcSessionRestartHint.NEW_GROUP_AFTER_MAIN_MENU,
            emit = events::add,
        )

        assertFalse(events.any { it is TelemetryLifecycleEvent.SessionResumed })

        val ended = events.filterIsInstance<TelemetryLifecycleEvent.SessionEnded>().single()
        assertEquals(SessionEndReason.REPLACED_AFTER_MAIN_MENU, ended.reason)

        val started = events.filterIsInstance<TelemetryLifecycleEvent.SessionStarted>().single()
        assertEquals(2L, started.session.sessionId)
    }

    @Test
    fun `same group restart hint starts a new session without leaving in-session state`() {
        val tracker = AcSessionTracker()
        val events = mutableListOf<TelemetryLifecycleEvent>()

        tracker.onConnectionStateChanged(GameConnectionState.IN_SESSION, DataSourceType.NATIVE, events::add)
        tracker.onFrame(
            frame(
                sessionType = SessionType.RACE,
                currentLap = 6,
                completedLaps = 5,
                sessionTimeLeftSec = 120f,
                timestampNs = 1_000_000_000L,
            ),
            DataSourceType.NATIVE,
            emit = events::add,
        )

        events.clear()

        tracker.onFrame(
            frame(
                sessionType = SessionType.RACE,
                currentLap = 6,
                completedLaps = 5,
                sessionTimeLeftSec = 1_800f,
                timestampNs = 2_000_000_000L,
            ),
            DataSourceType.NATIVE,
            restartHint = AcSessionRestartHint.PRESERVE_GROUP,
            emit = events::add,
        )

        assertFalse(events.any { it is TelemetryLifecycleEvent.SessionPaused })
        assertFalse(events.any { it is TelemetryLifecycleEvent.SessionResumed })

        val ended = events.filterIsInstance<TelemetryLifecycleEvent.SessionEnded>().single()
        assertEquals(SessionEndReason.REPLACED_BY_NEW_SESSION, ended.reason)

        val started = events.filterIsInstance<TelemetryLifecycleEvent.SessionStarted>().single()
        assertEquals(2L, started.session.sessionId)
    }

    @Test
    fun `brief menu enter does not resume session`() {
        val tracker = AcSessionTracker()
        val events = mutableListOf<TelemetryLifecycleEvent>()

        tracker.onConnectionStateChanged(GameConnectionState.IN_SESSION, DataSourceType.NATIVE, events::add)
        tracker.onFrame(
            frame(
                sessionType = SessionType.PRACTICE,
                currentLap = 2,
                completedLaps = 1,
                sessionTimeLeftSec = 1_200f,
                timestampNs = 1_000_000_000L,
            ),
            DataSourceType.NATIVE,
            events::add,
        )

        events.clear()

        tracker.onConnectionStateChanged(GameConnectionState.IN_MENU, DataSourceType.NATIVE, events::add)
        tracker.onConnectionStateChanged(GameConnectionState.IN_SESSION, DataSourceType.NATIVE, events::add)
        tracker.onFrame(
            frame(
                sessionType = SessionType.PRACTICE,
                currentLap = 2,
                completedLaps = 1,
                sessionTimeLeftSec = 1_199f,
                timestampNs = 2_000_000_000L,
            ),
            DataSourceType.NATIVE,
            events::add,
        )
        tracker.onConnectionStateChanged(GameConnectionState.IN_MENU, DataSourceType.NATIVE, events::add)

        assertEquals(2, events.filterIsInstance<TelemetryLifecycleEvent.SessionPaused>().size)
        assertEquals(1, events.filterIsInstance<TelemetryLifecycleEvent.SessionResumed>().size)
        assertFalse(events.any { it is TelemetryLifecycleEvent.SessionStarted })
    }

    private fun frame(
        sessionType: SessionType,
        currentLap: Int,
        completedLaps: Int,
        sessionTimeLeftSec: Float,
        plannedLaps: Int? = null,
        isTimedRace: Boolean? = false,
        sessionIndex: Int = 0,
        timestampNs: Long = 0L,
    ): TelemetryFrame = TelemetryFrame(
        session = SessionFrame(
            sessionType = sessionType,
            sessionTimeLeftSec = sessionTimeLeftSec,
            completedLaps = completedLaps,
            plannedLaps = plannedLaps,
            isTimedRace = isTimedRace,
            sessionIndex = sessionIndex,
            car = CarInfo(carModel = "ks_bmw_m4_gt3"),
            track = TrackInfo(trackId = "brands_hatch_indy"),
        ),
        lap = LapFrame(
            currentLapIndex = currentLap,
            completedLaps = completedLaps,
            validity = LapValidity.VALID,
        ),
        timestampNs = timestampNs,
    )
}
