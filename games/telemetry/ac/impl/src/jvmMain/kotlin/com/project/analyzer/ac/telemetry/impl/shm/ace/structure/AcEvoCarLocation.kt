package com.project.analyzer.ac.telemetry.impl.shm.ace.structure

public enum class AcEvoCarLocation(val rawValue: Int) {
    UNASSIGNED(0),
    PITLANE(1),
    PITENTRY(2),
    PITEXIT(3),
    TRACK(4),
    ;

    companion object {

        fun fromRaw(value: Int): AcEvoCarLocation? = entries.firstOrNull { it.rawValue == value }
    }
}
