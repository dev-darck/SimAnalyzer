package com.project.analyzer.telemetry.ac.api.contract

public enum class SimStatus {
    OFF,
    REPLAY,
    LIVE,
    PAUSE;

    public companion object {

        public fun fromAcValue(value: Int): SimStatus = when (value) {
            0 -> OFF
            1 -> REPLAY
            2 -> LIVE
            3 -> PAUSE
            else -> OFF
        }
    }
}

public enum class SessionType {
    UNKNOWN,
    PRACTICE,
    QUALIFY,
    RACE,
    HOTLAP,
    TIME_ATTACK,
    DRIFT,
    DRAG,
    HOTSTINT,
    HOTLAP_SUPERPOLE;

    public companion object {

        public fun fromAcValue(value: Int): SessionType = when (value) {
            -1 -> UNKNOWN
            0 -> PRACTICE
            1 -> QUALIFY
            2 -> RACE
            3 -> HOTLAP
            4 -> TIME_ATTACK
            5 -> DRIFT
            6 -> DRAG
            7 -> HOTSTINT
            8 -> HOTLAP_SUPERPOLE
            else -> UNKNOWN
        }
    }
}

public enum class SessionPhase {
    NONE,
    STARTING,
    GREEN_FLAG,
    SESSION_OVER
}

public enum class LapValidity {
    VALID,
    INVALID,
    UNKNOWN;

    public companion object {

        public fun fromBoolean(isValid: Boolean?): LapValidity = when (isValid) {
            true -> VALID
            false -> INVALID
            null -> UNKNOWN
        }
    }
}
