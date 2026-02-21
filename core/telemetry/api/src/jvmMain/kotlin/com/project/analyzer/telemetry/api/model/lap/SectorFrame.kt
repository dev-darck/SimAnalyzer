package com.project.analyzer.telemetry.api.model.lap

public data class SectorFrame(
    val index: Int, // 0..sectorCount-1
    val timeMs: Int? = null,
    val bestTimeMs: Int? = null,
    val deltaToBestMs: Int? = null,
    val status: SectorStatus = SectorStatus.UNKNOWN,

    val validity: SectorValidity = SectorValidity.UNKNOWN,
    val invalidReason: SectorInvalidReason? = null,
)

public enum class SectorStatus {
    UNKNOWN,
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
}

public enum class SectorValidity {
    UNKNOWN,
    VALID,
    INVALID,
}

public enum class SectorInvalidReason {
    OFFTRACK,
    CUT_TRACK,
    PENALTY,
    CRASH,
    CONTACT,
    UNKNOWN,
}
