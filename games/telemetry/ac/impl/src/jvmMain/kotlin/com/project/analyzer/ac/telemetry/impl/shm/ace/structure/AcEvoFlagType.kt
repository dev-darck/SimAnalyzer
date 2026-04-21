package com.project.analyzer.ac.telemetry.impl.shm.ace.structure

public enum class AcEvoFlagType(val rawValue: Int) {
    NO_FLAG(0),
    WHITE_FLAG(1),
    GREEN_FLAG(2),
    RED_FLAG(3),
    BLUE_FLAG(4),
    YELLOW_FLAG(5),
    BLACK_FLAG(6),
    BLACK_WHITE_FLAG(7),
    CHECKERED_FLAG(8),
    ORANGE_CIRCLE_FLAG(9),
    RED_YELLOW_STRIPES_FLAG(10),
    ;

    companion object {

        fun fromRaw(value: Int): AcEvoFlagType? = entries.firstOrNull { it.rawValue == value }
    }
}
