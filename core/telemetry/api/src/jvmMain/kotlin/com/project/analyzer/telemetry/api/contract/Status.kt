package com.project.analyzer.telemetry.api.contract

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
    QUALIFYING,
    RACE,
    HOTLAP,
    TIME_ATTACK,
    DRIFT,
    DRAG,
    WARMUP;

    public companion object {

        public fun fromAcValue(value: Int): SessionType = when (value) {
            0 -> PRACTICE
            1 -> QUALIFYING
            2 -> RACE
            3 -> HOTLAP
            4 -> TIME_ATTACK
            5 -> DRIFT
            6 -> DRAG
            7 -> WARMUP
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
