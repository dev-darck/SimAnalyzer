package com.project.analyzer.telemetry.lmu.impl.mapper

import com.project.analyzer.telemetry.api.model.session.CarInfo
import com.project.analyzer.telemetry.api.model.session.PitState
import com.project.analyzer.telemetry.api.model.session.SessionFrame
import com.project.analyzer.telemetry.api.model.session.TrackInfo
import com.project.analyzer.telemetry.lmu.api.model.LmuScoringInfo
import com.project.analyzer.telemetry.lmu.api.model.LmuVehicleScoring
import com.project.analyzer.telemetry.lmu.api.model.LmuVehicleTelemetry
import com.project.analyzer.telemetry.lmu.impl.common.LmuTelemetryConversions
import com.project.analyzer.utils.TelemetryIdentityFormatter

internal fun mapSession(
    telemetry: LmuVehicleTelemetry,
    scoring: LmuVehicleScoring?,
    scoringInfo: LmuScoringInfo?,
): SessionFrame {
    val completedLaps = telemetry.lapNumber.takeIf { it >= 0 }
    val rawTrackName = scoringInfo?.trackName?.trim()?.takeIf { it.isNotBlank() }
        ?: telemetry.trackName.trim().takeIf { it.isNotBlank() }
    val trackName = rawTrackName?.let { raw ->
        TelemetryIdentityFormatter.formatTrackName(trackName = raw)
    }
    val carModel = telemetry.vehicleName.trim().takeIf { it.isNotBlank() }
    val trackLength = scoringInfo?.lapDist?.toFloat()?.takeIf { it.isFinite() && it > 0f }
    val distanceTraveled = scoring?.lapDist?.toFloat()?.takeIf { it.isFinite() && it >= 0f }
    val normalizedLapPosition = if (trackLength != null && distanceTraveled != null) {
        (distanceTraveled / trackLength).coerceIn(0f, 1f)
    } else {
        null
    }
    val sessionTimeElapsedSec = scoringInfo?.currentEt?.toFloat()?.takeIf { it.isFinite() && it >= 0f }
        ?: telemetry.elapsedTime.toFloat().takeIf { it.isFinite() && it >= 0f }
    val sessionTimeLeftSec = scoringInfo?.let { info ->
        (info.endEt - info.currentEt).toFloat().takeIf { it.isFinite() && it >= 0f }
    }
    val rawGamePhase = scoringInfo?.gamePhase
    return SessionFrame(
        status = LmuTelemetryConversions.simStatus(scoringInfo?.inRealtime, rawGamePhase),
        sessionType = scoringInfo?.session?.let(LmuTelemetryConversions::sessionType),
        phase = rawGamePhase?.let(LmuTelemetryConversions::sessionPhase),
        phaseLabel = rawGamePhase?.let(LmuTelemetryConversions::sessionPhaseLabel),
        completedLaps = completedLaps,
        position = scoring?.place?.takeIf { it > 0 },
        track = rawTrackName?.let {
            TrackInfo(
                trackId = it,
                trackName = trackName,
                sectorCount = SECTOR_COUNT,
                lengthMeters = trackLength,
                normalizedLapPosition = normalizedLapPosition,
                distanceTraveled = distanceTraveled,
            )
        },
        pit = PitState(
            isInPit = scoring?.inPits ?: false,
            isInPitLane = scoring?.pitState?.let { it in PIT_LANE_STATES } ?: false,
        ),
        car = CarInfo(
            carModel = carModel,
            carName = TelemetryIdentityFormatter.formatCarName(carName = carModel, carModel = carModel),
            carId = telemetry.id,
            maxRpm = telemetry.engineMaxRpm.toInt().takeIf { it > 0 },
            maxFuelLiters = telemetry.fuelCapacity.toFloat().takeIf { it > 0f },
        ),
        sessionTimeElapsedSec = sessionTimeElapsedSec,
        sessionTimeLeftSec = sessionTimeLeftSec,
        plannedLaps = scoringInfo?.maxLaps?.takeIf { it > 0 && it != Int.MAX_VALUE },
        gapAheadMs = telemetry.timeGapPlaceAhead.toPositiveMillis(),
        gapBehindMs = telemetry.timeGapPlaceBehind.toPositiveMillis(),
    )
}

private fun Float.toPositiveMillis(): Int? = takeIf { it.isFinite() && it > 0f }
    ?.let { (it * MS_IN_SECOND).toInt() }

private val PIT_LANE_STATES = setOf(1, 2, 3, 4)
private const val SECTOR_COUNT = 3
private const val MS_IN_SECOND = 1000f
