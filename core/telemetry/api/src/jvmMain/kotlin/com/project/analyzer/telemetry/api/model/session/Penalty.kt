package com.project.analyzer.telemetry.api.model.session

public data class Penalty(val type: PenaltyType? = null, val penaltyTimeSec: Float? = null)

public enum class PenaltyType {
    NONE,
    DRIVE_THROUGH_CUTTING,
    STOP_AND_GO_10_CUTTING,
    STOP_AND_GO_20_CUTTING,
    STOP_AND_GO_30_CUTTING,
    DISQUALIFIED_CUTTING,
    REMOVE_BEST_LAP_TIME_CUTTING,
    DRIVE_THROUGH_PIT_SPEEDING,
    STOP_AND_GO_10_PIT_SPEEDING,
    STOP_AND_GO_20_PIT_SPEEDING,
    STOP_AND_GO_30_PIT_SPEEDING,
    DISQUALIFIED_PIT_SPEEDING,
    REMOVE_BEST_LAP_TIME_PIT_SPEEDING,
    DISQUALIFIED_IGNORED_MANDATORY_PIT,
    POST_RACE_TIME,
    DISQUALIFIED_TROLLING,
    DISQUALIFIED_PIT_ENTRY,
    DISQUALIFIED_PIT_EXIT,
    DISQUALIFIED_WRONG_WAY,
    DRIVE_THROUGH_IGNORED_DRIVER_STINT,
    DISQUALIFIED_IGNORED_DRIVER_STINT,
    DISQUALIFIED_EXCEEDED_DRIVER_STINT_LIMIT,
    ;

    public companion object {

        public fun fromAcValue(value: Int): PenaltyType = entries.getOrElse(value) { NONE }
    }
}
