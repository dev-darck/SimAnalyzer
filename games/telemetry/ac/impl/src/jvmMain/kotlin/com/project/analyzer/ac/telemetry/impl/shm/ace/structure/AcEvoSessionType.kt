package com.project.analyzer.ac.telemetry.impl.shm.ace.structure

public enum class AcEvoSessionType(val rawValue: Int) {
    UNKNOWN(-1),
    TIME_ATTACK(0),
    RACE(1),
    HOT_STINT(2),
    CRUISE(3),
    ;

    companion object {

        fun fromRaw(value: Int): AcEvoSessionType = entries.firstOrNull { it.rawValue == value } ?: UNKNOWN
    }
}
