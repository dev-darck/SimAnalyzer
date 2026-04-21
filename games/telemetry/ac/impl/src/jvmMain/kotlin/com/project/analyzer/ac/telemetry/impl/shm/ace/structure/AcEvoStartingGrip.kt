package com.project.analyzer.ac.telemetry.impl.shm.ace.structure

public enum class AcEvoStartingGrip(val rawValue: Int) {
    GREEN(0),
    FAST(1),
    OPTIMUM(2),
    ;

    companion object {

        fun fromRaw(value: Int): AcEvoStartingGrip? = entries.firstOrNull { it.rawValue == value }
    }
}
