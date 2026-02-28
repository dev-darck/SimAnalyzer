package com.project.analyzer.ac.telemetry.impl.internal.pipeline

internal interface AcPollSnapshotAdapter {

    val order: Int get() = 0

    fun onStateChanged(newState: com.project.analyzer.ac.telemetry.impl.internal.GameConnectionState) = Unit

    fun apply(context: AcPollSnapshotAdapterContext) = Unit

    fun onStop() = Unit
}
