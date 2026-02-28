package com.project.analyzer.fuel.presentation.map

import com.project.analyzer.fuel.domain.model.FuelEstimate
import com.project.analyzer.fuel.domain.model.FuelPhase
import org.junit.Test
import kotlin.test.assertEquals

class UiStateMapperTest {

    @Test
    fun `display lap number prefers completed laps when current lap lags`() {
        val estimate = FuelEstimate(
            phase = FuelPhase.PER_LAP,
            currentFuelLiters = 50.0,
            maxFuelLiters = 110.0,
            litersPerLap = 2.5,
            litersPerSecond = null,
            lastLapTimeMs = 90_000,
            estimatedLapTimeSec = 90.0,
            isLapTimeFromCompletedLap = true,
            lapsRemaining = 10.0,
            gameFuelPerLap = null,
            gameFuelEstimatedLaps = null,
            carModel = "ks_bmw_m4_gt3",
            carId = 1333049901,
            trackId = "brands_hatch_indy",
            currentLapIndex = 3,
            completedLaps = 3,
            confidence = 1.0,
            isCurrentLapValid = true,
        )

        val uiState = estimate.toUiState(safetyFactor = 1.0)

        assertEquals(4, uiState.displayLapNumber)
    }

    @Test
    fun `per lap remaining count uses ceiling to keep current game lap`() {
        val estimate = FuelEstimate(
            phase = FuelPhase.PER_LAP,
            currentFuelLiters = 50.0,
            maxFuelLiters = 110.0,
            litersPerLap = 2.5,
            litersPerSecond = null,
            lastLapTimeMs = 90_000,
            estimatedLapTimeSec = 90.0,
            isLapTimeFromCompletedLap = true,
            lapsRemaining = 2.1,
            gameFuelPerLap = 2.5f,
            gameFuelEstimatedLaps = 2.1f,
            carModel = "ks_bmw_m4_gt3",
            carId = 1333049901,
            trackId = "brands_hatch_indy",
            currentLapIndex = 3,
            completedLaps = 2,
            confidence = 1.0,
            isCurrentLapValid = true,
        )

        val uiState = estimate.toUiState(safetyFactor = 1.0)

        assertEquals(3, uiState.lapsRemainingCount)
    }
}
