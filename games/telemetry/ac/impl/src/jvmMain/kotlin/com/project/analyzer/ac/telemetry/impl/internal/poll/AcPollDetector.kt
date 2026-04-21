package com.project.analyzer.ac.telemetry.impl.internal.poll

import com.project.analyzer.ac.telemetry.impl.internal.poll.backend.AcPollBackend

internal class AcPollDetector {

    var stalePacketCounter: Int = 0
        private set

    var activePacketCounter: Int = 0
        private set

    var graphicsStaleCounter: Int = 0
        private set

    private var lastDetectedPhysicsPacket: Int = -1
    private var lastDetectedGraphicsPacket: Int = -1

    fun detect(currentState: GameConnectionState, backend: AcPollBackend?): AcPollDetection {
        if (backend == null || !backend.isAnyAttached()) {
            reset()
            return AcPollDetection(
                state = GameConnectionState.DISCONNECTED,
                dataSource = DataSourceType.NATIVE,
                needsFallback = false,
            )
        }

        val graphicsIsStale = updateGraphicsStaleness(backend)
        val hasNativeGraphics = backend.hasGraphicsSignal()
        val hasNativeStatic = backend.hasNativeSignature()

        val hasPhysicsPacket = updatePhysicsActivity(backend)
        val physicsIsActive = activePacketCounter >= ACTIVE_PACKET_THRESHOLD
        val physicsIsInactive = stalePacketCounter >= STALE_PACKET_THRESHOLD

        val statusOverride = resolveStatusOverride(backend, hasNativeGraphics)
        val statusHint = resolveStatusHint(backend, hasNativeGraphics, graphicsIsStale)
        val needsStaticPatch = backend.needsFallback()

        val isNative = hasNativeGraphics || hasNativeStatic
        val needsFallback = !hasNativeGraphics || needsStaticPatch

        if (isNative) {
            return AcPollDetection(
                state = resolveNativeState(
                    currentState = currentState,
                    statusOverride = statusOverride,
                    statusHint = statusHint,
                    hasPhysicsPacket = hasPhysicsPacket,
                    physicsIsActive = physicsIsActive,
                    physicsIsInactive = physicsIsInactive,
                ),
                dataSource = DataSourceType.NATIVE,
                needsFallback = needsFallback,
            )
        }

        return AcPollDetection(
            state = resolveFallbackState(
                currentState = currentState,
                hasPhysicsPacket = hasPhysicsPacket,
                physicsIsActive = physicsIsActive,
                physicsIsInactive = physicsIsInactive,
            ),
            dataSource = DataSourceType.FALLBACK,
            needsFallback = hasPhysicsPacket,
        )
    }

    fun reset() {
        lastDetectedPhysicsPacket = -1
        lastDetectedGraphicsPacket = -1
        stalePacketCounter = 0
        activePacketCounter = 0
        graphicsStaleCounter = 0
    }

    private fun resolveStatusOverride(backend: AcPollBackend, hasNativeGraphics: Boolean): GameConnectionState? {
        if (!hasNativeGraphics) return null
        val status = backend.graphicsStatus()
        if (status !in STATUS_OFF..STATUS_PAUSE) return null

        val setupMenuVisible = backend.graphicsSetupMenuVisible()
        return when (status) {
            STATUS_OFF, STATUS_REPLAY, STATUS_PAUSE -> GameConnectionState.IN_MENU
            else -> if (setupMenuVisible) GameConnectionState.IN_MENU else null
        }
    }

    private fun resolveStatusHint(
        backend: AcPollBackend,
        hasNativeGraphics: Boolean,
        graphicsIsStale: Boolean,
    ): GameConnectionState? {
        if (!hasNativeGraphics) return null
        if (graphicsIsStale) return null
        if (backend.graphicsStatus() != STATUS_LIVE) return null
        return GameConnectionState.IN_SESSION
    }

    private fun resolveNativeState(
        currentState: GameConnectionState,
        statusOverride: GameConnectionState?,
        statusHint: GameConnectionState?,
        hasPhysicsPacket: Boolean,
        physicsIsActive: Boolean,
        physicsIsInactive: Boolean,
    ): GameConnectionState = when {
        statusOverride != null -> statusOverride
        hasPhysicsPacket && physicsIsActive -> GameConnectionState.IN_SESSION
        hasPhysicsPacket && physicsIsInactive -> GameConnectionState.IN_MENU
        statusHint != null -> statusHint
        currentState == GameConnectionState.IN_SESSION -> currentState
        else -> GameConnectionState.IN_MENU
    }

    private fun resolveFallbackState(
        currentState: GameConnectionState,
        hasPhysicsPacket: Boolean,
        physicsIsActive: Boolean,
        physicsIsInactive: Boolean,
    ): GameConnectionState = when {
        physicsIsActive -> GameConnectionState.IN_SESSION

        physicsIsInactive -> {
            if (hasPhysicsPacket) {
                GameConnectionState.IN_MENU
            } else {
                GameConnectionState.DISCONNECTED
            }
        }

        else -> when {
            currentState == GameConnectionState.IN_SESSION -> currentState
            hasPhysicsPacket -> GameConnectionState.IN_MENU
            else -> GameConnectionState.DISCONNECTED
        }
    }

    private fun updateGraphicsStaleness(backend: AcPollBackend): Boolean {
        val packetId = backend.graphicsPacketId()
        val hasPacket = packetId > 0
        val packetChanged = hasPacket && packetId != lastDetectedGraphicsPacket

        if (packetChanged) {
            lastDetectedGraphicsPacket = packetId
            graphicsStaleCounter = 0
        } else {
            graphicsStaleCounter = (graphicsStaleCounter + 1).coerceAtMost(STALE_PACKET_THRESHOLD)
        }

        return graphicsStaleCounter >= STALE_PACKET_THRESHOLD
    }

    private fun updatePhysicsActivity(backend: AcPollBackend): Boolean {
        val packetId = backend.physicsPacketId()
        val hasPacket = packetId > 0
        val packetChanged = hasPacket && packetId != lastDetectedPhysicsPacket

        if (packetChanged) {
            lastDetectedPhysicsPacket = packetId
            activePacketCounter = (activePacketCounter + 1).coerceAtMost(ACTIVE_PACKET_THRESHOLD)
            stalePacketCounter = 0
        } else {
            stalePacketCounter = (stalePacketCounter + 1).coerceAtMost(STALE_PACKET_THRESHOLD)
            if (stalePacketCounter >= STALE_PACKET_THRESHOLD) {
                activePacketCounter = 0
            }
        }

        return hasPacket
    }

    private companion object {
        const val STATUS_OFF = 0
        const val STATUS_REPLAY = 1
        const val STATUS_LIVE = 2
        const val STATUS_PAUSE = 3

        const val STALE_PACKET_THRESHOLD = 30
        const val ACTIVE_PACKET_THRESHOLD = 5
    }
}
