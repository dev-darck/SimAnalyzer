package com.project.analyzer.ac.telemetry.impl.fallback.logfile.model

enum class EvoSessionType(val shmValue: Int) {
    UNKNOWN(-1),
    PRACTICE(0),
    QUALIFYING(1),
    RACE(2),
    HOTLAP(3),
    TIME_ATTACK(4),
    DRIFT(5),
    DRAG(6),
    WARMUP(7),
    ;

    companion object {

        fun fromLogString(value: String): EvoSessionType {
            val key = value
                .lowercase()
                .trim()
                .replace(Regex("[^a-z]"), "")

            return when (key) {
                "practice" -> PRACTICE
                "qualifying", "qualify" -> QUALIFYING
                "race" -> RACE
                "hotlap" -> HOTLAP
                "timeattack" -> TIME_ATTACK
                "drift" -> DRIFT
                "drag" -> DRAG
                "warmup" -> WARMUP
                else -> UNKNOWN
            }
        }
    }
}

data class EvoFileInfo(
    val trackName: String? = null,
    val trackId: String? = null,
    val layoutId: String? = null,
    val carModel: String? = null,
    val sessionEpoch: Long = 0L,
    val sessionEpochStartedFromMainMenu: Boolean = false,
    val driverName: String? = null,
    val driverSteamId: String? = null,
    val penaltyId: String? = null,
    val hasPenalty: Boolean = false,
    val penaltyReason: String? = null,
    val penaltyTimestamp: String? = null,
    val sessionType: EvoSessionType = EvoSessionType.UNKNOWN,
    val playerCarUuid: String? = null,
)
