package com.project.analyzer.ac.telemetry.impl.shm.ace.structure

public enum class AcEvoStatus(val rawValue: Int) {
    OFF(0),
    REPLAY(1),
    LIVE(2),
    PAUSE(3),
    ;

    companion object {

        fun fromRaw(value: Int): AcEvoStatus? = entries.firstOrNull { it.rawValue == value }
    }
}
