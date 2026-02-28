package com.project.analyzer.ac.telemetry.impl.internal.pipeline

import com.project.analyzer.ac.telemetry.impl.fallback.AcEvoFallbackShmPatcher
import com.project.analyzer.ac.telemetry.impl.internal.GameConnectionState
import dev.zacsweers.metro.Inject

@Inject
internal class AcFallbackPollSnapshotAdapter(private val fallback: AcEvoFallbackShmPatcher) : AcPollSnapshotAdapter {

    override val order: Int = 100

    override fun onStateChanged(newState: GameConnectionState) {
        if (newState == GameConnectionState.DISCONNECTED) {
            fallback.clear()
        }
    }

    override fun apply(context: AcPollSnapshotAdapterContext) {
        context.withSharedMemory { shm ->
            context.snapshot.sessionRestartHint = fallback.patchIfNeeded(
                shm = shm,
                loopStartNanos = context.loopStartNanos.takeIf { it > 0L } ?: System.nanoTime(),
                gameState = context.gameState,
            )
        }
    }

    override fun onStop() {
        fallback.clear()
    }
}
