package com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot

import kotlin.test.Test
import kotlin.test.assertEquals

class AceFallbackSnapshotPagesTest {

    @Test
    fun `seedFrom clears scratch pages before applying ACE values`() {
        val pages = AceFallbackSnapshotPages().apply {
            statics.maxRpm = 12_345
            statics.maxTorque = 456f
            statics.maxFuel = 78f
            statics.trackSPlineLength = 999f
        }
        val snapshot = AceRawSnapshot().apply {
            physics.currentMaxRPM = 9_876f
            graphics.maxFuelRaw = 101f
            graphics.maxTurboBoostRaw = 1.6f
            graphics.hasKersRaw = 1
            statics.trackLengthMRaw = 5_732f
            statics.isTimedRaceRaw = 1
            statics.isOnlineRaw = 1
        }

        pages.seedFrom(snapshot)

        assertEquals(9_876, pages.statics.maxRpm)
        assertEquals(0f, pages.statics.maxTorque)
        assertEquals(101f, pages.statics.maxFuel)
        assertEquals(1.6f, pages.statics.maxTurboBoost)
        assertEquals(5_732f, pages.statics.trackSPlineLength)
        assertEquals(1, pages.statics.isTimedRace)
        assertEquals(1, pages.statics.isOnline)
        assertEquals(1, pages.statics.hasKERS)
        assertEquals(0, pages.statics.sectorCount)
    }
}
