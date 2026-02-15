package com.project.analyzer.telemetry.lmu.impl.mapper

import com.project.analyzer.telemetry.api.model.session.CarInfo
import com.project.analyzer.telemetry.api.model.session.PitState
import com.project.analyzer.telemetry.api.model.session.SessionFrame
import com.project.analyzer.telemetry.api.model.session.TrackInfo
import com.project.analyzer.telemetry.lmu.api.model.LmuVehicleScoring
import com.project.analyzer.telemetry.lmu.api.model.LmuVehicleTelemetry

internal fun mapSession(telemetry: LmuVehicleTelemetry, scoring: LmuVehicleScoring?): SessionFrame {
    val lapIndex = telemetry.lapNumber
    val trackName = telemetry.trackName.trim().takeIf { it.isNotBlank() }
    val carModel = telemetry.vehicleName.trim().takeIf { it.isNotBlank() }
    return SessionFrame(
        completedLaps = if (lapIndex > 0) lapIndex - 1 else null,
        plannedLaps = scoring?.totalLaps?.takeIf { it > 0 },
        position = scoring?.place?.takeIf { it > 0 },
        track = trackName?.let { TrackInfo(trackId = it, trackName = it) },
        pit = PitState(
            isInPit = scoring?.inPits ?: false
        ),
        car = CarInfo(
            carModel = carModel,
            carId = telemetry.id,
            maxRpm = telemetry.engineMaxRpm.toInt().takeIf { it > 0 },
            maxFuelLiters = telemetry.fuelCapacity.toFloat().takeIf { it > 0f }
        ),
        sessionTimeElapsedSec = telemetry.elapsedTime.toFloat().takeIf { it > 0 }
    )
}
