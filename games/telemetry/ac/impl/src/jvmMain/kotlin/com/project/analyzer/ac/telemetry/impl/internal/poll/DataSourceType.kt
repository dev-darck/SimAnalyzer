package com.project.analyzer.ac.telemetry.impl.internal.poll

/**
 * Data source type currently being used
 */
enum class DataSourceType {

    /** Using native AC/ACC shared memory data */
    NATIVE,

    /** Using fallback data (AC Evo mode - reconstructed from physics + logs) */
    FALLBACK,
}
