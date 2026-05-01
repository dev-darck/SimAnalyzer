package com.project.analyzer.telemetry.recording.api.recording

import com.project.analyzer.telemetry.api.contract.LapValidity
import com.project.analyzer.telemetry.api.model.TelemetryFrame

public data class TelemetryRecordingFrameSnapshot(
    val session: SessionSnapshot? = null,
    val lap: LapSnapshot? = null,
    val car: CarSnapshot? = null,
    val wheels: WheelsSnapshot? = null,
    val environment: EnvironmentSnapshot? = null,
) {

    public data class SessionSnapshot(
        val completedLaps: Int? = null,
        val track: TrackSnapshot? = null,
        val car: SessionCarSnapshot? = null,
        val pit: PitSnapshot? = null,
    )

    public data class TrackSnapshot(
        val trackId: String? = null,
        val trackName: String? = null,
        val layoutId: String? = null,
        val normalizedLapPosition: Float? = null,
    )

    public data class SessionCarSnapshot(
        val carModel: String? = null,
        val carName: String? = null,
        val carId: Int? = null,
    )

    public data class PitSnapshot(val isInPit: Boolean? = null, val isInPitLane: Boolean? = null,)

    public data class LapSnapshot(
        val currentLapIndex: Int? = null,
        val completedLaps: Int? = null,
        val currentSectorIndex: Int? = null,
        val validity: LapValidity? = null,
    )

    public data class CarSnapshot(
        val speedKmh: Float? = null,
        val velocityX: Float? = null,
        val velocityZ: Float? = null,
        val worldPositionX: Float? = null,
        val worldPositionZ: Float? = null,
        val heading: Float? = null,
    )

    public data class WheelsSnapshot(
        val fl: WheelSnapshot? = null,
        val fr: WheelSnapshot? = null,
        val rl: WheelSnapshot? = null,
        val rr: WheelSnapshot? = null,
    )

    public data class WheelSnapshot(val contactPointX: Float? = null, val contactPointZ: Float? = null,)

    public data class EnvironmentSnapshot(val airTempC: Float? = null, val roadTempC: Float? = null,)

    public companion object {
        public fun from(frame: TelemetryFrame): TelemetryRecordingFrameSnapshot = TelemetryRecordingFrameSnapshot(
            session = frame.session?.let { session ->
                SessionSnapshot(
                    completedLaps = session.completedLaps,
                    track = session.track?.let { track ->
                        TrackSnapshot(
                            trackId = track.trackId,
                            trackName = track.trackName,
                            layoutId = track.layoutId,
                            normalizedLapPosition = track.normalizedLapPosition,
                        )
                    },
                    car = session.car?.let { car ->
                        SessionCarSnapshot(
                            carModel = car.carModel,
                            carName = car.carName,
                            carId = car.carId,
                        )
                    },
                    pit = session.pit?.let { pit ->
                        PitSnapshot(
                            isInPit = pit.isInPit,
                            isInPitLane = pit.isInPitLane,
                        )
                    },
                )
            },
            lap = frame.lap?.let { lap ->
                LapSnapshot(
                    currentLapIndex = lap.currentLapIndex,
                    completedLaps = lap.completedLaps,
                    currentSectorIndex = lap.currentSectorIndex,
                    validity = lap.validity,
                )
            },
            car = frame.car?.let { car ->
                CarSnapshot(
                    speedKmh = car.speedKmh,
                    velocityX = car.velocity?.x,
                    velocityZ = car.velocity?.z,
                    worldPositionX = car.worldPosition?.x,
                    worldPositionZ = car.worldPosition?.z,
                    heading = car.heading,
                )
            },
            wheels = frame.wheels?.let { wheels ->
                WheelsSnapshot(
                    fl = wheels.fl?.let { wheel ->
                        WheelSnapshot(
                            contactPointX = wheel.contactPoint?.x,
                            contactPointZ = wheel.contactPoint?.z,
                        )
                    },
                    fr = wheels.fr?.let { wheel ->
                        WheelSnapshot(
                            contactPointX = wheel.contactPoint?.x,
                            contactPointZ = wheel.contactPoint?.z,
                        )
                    },
                    rl = wheels.rl?.let { wheel ->
                        WheelSnapshot(
                            contactPointX = wheel.contactPoint?.x,
                            contactPointZ = wheel.contactPoint?.z,
                        )
                    },
                    rr = wheels.rr?.let { wheel ->
                        WheelSnapshot(
                            contactPointX = wheel.contactPoint?.x,
                            contactPointZ = wheel.contactPoint?.z,
                        )
                    },
                )
            },
            environment = frame.environment?.let { environment ->
                EnvironmentSnapshot(
                    airTempC = environment.airTempC,
                    roadTempC = environment.roadTempC,
                )
            },
        )
    }
}
