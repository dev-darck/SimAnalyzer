package com.project.analyzer.ac.telemetry.impl.internal.poll.pipeline

import com.project.analyzer.ac.telemetry.impl.internal.poll.GameConnectionState

internal interface AcPollSnapshotAdapter {

    val order: Int get() = 0

    fun onStateChanged(newState: GameConnectionState) = Unit

    fun apply(context: AcPollSnapshotAdapterContext) = Unit

    fun onStop() = Unit
}
