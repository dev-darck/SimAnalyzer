package com.project.analyzer.ac.telemetry.impl.shm.ace.structure

public enum class AcEvoEngineType(val rawValue: Int) {
    INTERNAL_COMBUSTION(0),
    ELECTRIC_MOTOR(1),
    ;

    companion object {

        fun fromRaw(value: Int): AcEvoEngineType? = entries.firstOrNull { it.rawValue == value }
    }
}
