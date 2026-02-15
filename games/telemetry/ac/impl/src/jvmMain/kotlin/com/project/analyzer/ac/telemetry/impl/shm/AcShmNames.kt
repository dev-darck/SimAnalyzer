package com.project.analyzer.ac.telemetry.impl.shm

data class AcShmNames(
    val physics: String = "Local\\acpmf_physics",
    val graphics: String = "Local\\acpmf_graphics",
    val statics: String = "Local\\acpmf_static"
)
