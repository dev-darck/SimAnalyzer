package com.project.analyzer.telemetry.lmu.impl.shm.structure

import kotlin.test.Test
import kotlin.test.assertEquals

internal class LmuStructureLayoutTest {

    @Test
    fun telemetryLayoutMatchesPack4HeaderSize() {
        assertEquals(1888, Rf2VehicleTelemetry().size())
    }

    @Test
    fun wheelLayoutMatchesPack4HeaderSize() {
        assertEquals(260, Rf2Wheel().size())
    }

    @Test
    fun vehicleScoringLayoutMatchesPack4HeaderSize() {
        assertEquals(584, Rf2VehicleScoring().size())
    }

    @Test
    fun scoringInfoLayoutMatchesPack4HeaderSize() {
        assertEquals(548, Rf2ScoringInfo().size())
        assertEquals(Rf2ScoringInfo.OFFSET + Rf2ScoringInfo.SIZE, Rf2VehicleScoring.OFFSET)
    }
}
