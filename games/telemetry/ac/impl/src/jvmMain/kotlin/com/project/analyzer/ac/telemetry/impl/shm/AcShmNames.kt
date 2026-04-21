package com.project.analyzer.ac.telemetry.impl.shm

data class AcShmNames(
    val evo: AcShmLayoutNames = AcShmLayoutNames(
        physics = "Local\\acevo_pmf_physics",
        graphics = "Local\\acevo_pmf_graphics",
        statics = "Local\\acevo_pmf_static",
    ),
    val legacy: AcShmLayoutNames = AcShmLayoutNames(
        physics = "Local\\acpmf_physics",
        graphics = "Local\\acpmf_graphics",
        statics = "Local\\acpmf_static",
    ),
)
