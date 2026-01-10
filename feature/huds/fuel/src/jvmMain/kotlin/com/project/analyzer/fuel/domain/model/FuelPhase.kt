package com.project.analyzer.fuel.domain.model

/**
 * Phase of fuel calculation
 */
enum class FuelPhase {

    /** Waiting in pits (stationary at session start) */
    PIT_WAITING,

    /** Collecting initial data after leaving pits */
    WARMUP,

    /** Predictive mode - calculating from real-time consumption */
    PREDICTIVE,

    /** Per-lap mode - have completed lap(s) with measured fuel */
    PER_LAP
}
