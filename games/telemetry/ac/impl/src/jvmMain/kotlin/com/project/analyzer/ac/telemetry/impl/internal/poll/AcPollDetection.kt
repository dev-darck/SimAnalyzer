package com.project.analyzer.ac.telemetry.impl.internal.poll

internal data class AcPollDetection(
    val state: GameConnectionState,
    val dataSource: DataSourceType,
    val needsFallback: Boolean,
)
