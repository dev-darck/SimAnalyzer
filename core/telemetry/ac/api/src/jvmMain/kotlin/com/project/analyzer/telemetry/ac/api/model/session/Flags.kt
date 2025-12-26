package com.project.analyzer.telemetry.ac.api.model.session

public data class Flags(
    val flag: FlagType? = null,

    // Global flags
    val globalYellow: Boolean? = null,
    val globalYellowSector1: Boolean? = null,
    val globalYellowSector2: Boolean? = null,
    val globalYellowSector3: Boolean? = null,
    val globalWhite: Boolean? = null,
    val globalGreen: Boolean? = null,
    val globalChequered: Boolean? = null,
    val globalRed: Boolean? = null,
)

public enum class FlagType {
    NONE,
    BLUE,
    YELLOW,
    BLACK,
    WHITE,
    CHECKERED,
    PENALTY,
    GREEN,
    ORANGE;

    public companion object {
        public fun fromAcValue(value: Int): FlagType = when (value) {
            0 -> NONE
            1 -> BLUE
            2 -> YELLOW
            3 -> BLACK
            4 -> WHITE
            5 -> CHECKERED
            6 -> PENALTY
            7 -> GREEN
            8 -> ORANGE
            else -> NONE
        }
    }
}
