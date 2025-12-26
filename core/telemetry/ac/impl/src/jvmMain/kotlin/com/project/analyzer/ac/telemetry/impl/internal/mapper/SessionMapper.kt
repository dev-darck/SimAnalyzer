package com.project.analyzer.ac.telemetry.impl.internal.mapper

import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileStatic
import com.project.analyzer.ac.telemetry.impl.shm.structure.toBoolean
import com.project.analyzer.telemetry.ac.api.contract.SessionType
import com.project.analyzer.telemetry.ac.api.contract.SimStatus
import com.project.analyzer.telemetry.ac.api.model.session.FlagType
import com.project.analyzer.telemetry.ac.api.model.session.Flags
import com.project.analyzer.telemetry.ac.api.model.session.Penalty
import com.project.analyzer.telemetry.ac.api.model.session.PenaltyType
import com.project.analyzer.telemetry.ac.api.model.session.PitState
import com.project.analyzer.telemetry.ac.api.model.session.SessionFrame
import dev.zacsweers.metro.Inject

@Inject
class SessionMapper(
    private val cache: AcSessionCache,
) {

    fun map(graphics: SPageFileGraphics, statics: SPageFileStatic): SessionFrame {
        return SessionFrame(
            status = SimStatus.fromAcValue(graphics.status),
            sessionType = SessionType.fromAcValue(graphics.session),

            track = cache.trackInfo?.copy(
                normalizedLapPosition = graphics.normalizedCarPosition,
                distanceTraveled = graphics.distanceTraveled,
                trackStatus = graphics.trackStatus.toString(),
            ),
            car = cache.carInfo?.copy(carId = graphics.playerCarID),
            driver = cache.driverInfo?.copy(
                stintTotalTimeLeftMs = graphics.driverStintTotalTimeLeft,
                stintTimeLeftMs = graphics.driverStintTimeLeft,
            ),

            sessionTimeLeftSec = graphics.sessionTimeLeft,
            completedLaps = graphics.completedLaps,
            plannedLaps = graphics.numberOfLaps.takeIf { it > 0 },
            position = graphics.position,

            flags = mapFlags(graphics),
            penalty = mapPenalty(graphics),
            pit = mapPitState(graphics, statics),

            sessionIndex = graphics.sessionIndex,
            numberOfSessions = statics.numberOfSessions,
            isOnline = statics.isOnline.toBoolean(),
            isTimedRace = statics.isTimedRace.toBoolean(),
            hasExtraLap = statics.hasExtraLap.toBoolean(),

            activeCars = graphics.activeCars,
            numCars = statics.numCars,

            gapAheadMs = graphics.gapAhead.takeIf { it > 0 },
            gapBehindMs = graphics.gapBehind.takeIf { it > 0 },

            isSetupMenuVisible = graphics.isSetupMenuVisible.toBoolean(),
            mainDisplayIndex = graphics.mainDisplayIndex,
            secondaryDisplayIndex = graphics.secondaryDisplayIndex,
        )
    }

    private fun mapFlags(graphics: SPageFileGraphics): Flags {
        return Flags(
            flag = FlagType.fromAcValue(graphics.flag),
            globalYellow = graphics.globalYellow.toBoolean(),
            globalYellowSector1 = graphics.globalYellow1.toBoolean(),
            globalYellowSector2 = graphics.globalYellow2.toBoolean(),
            globalYellowSector3 = graphics.globalYellow3.toBoolean(),
            globalWhite = graphics.globalWhite.toBoolean(),
            globalGreen = graphics.globalGreen.toBoolean(),
            globalChequered = graphics.globalChequered.toBoolean(),
            globalRed = graphics.globalRed.toBoolean(),
        )
    }

    private fun mapPenalty(graphics: SPageFileGraphics): Penalty {
        return Penalty(
            type = PenaltyType.fromAcValue(graphics.penalty),
            penaltyTimeSec = graphics.penaltyTime.takeIf { it > 0 },
        )
    }

    private fun mapPitState(graphics: SPageFileGraphics, statics: SPageFileStatic): PitState {
        return PitState(
            isInPit = graphics.isInPit.toBoolean(),
            isInPitLane = graphics.isInPitLane.toBoolean(),
            mandatoryPitDone = graphics.mandatoryPitDone.toBoolean(),
            missingMandatoryPits = graphics.missingMandatoryPits,
            pitWindowStart = statics.pitWindowStart.takeIf { it > 0 },
            pitWindowEnd = statics.pitWindowEnd.takeIf { it > 0 },
        )
    }
}
