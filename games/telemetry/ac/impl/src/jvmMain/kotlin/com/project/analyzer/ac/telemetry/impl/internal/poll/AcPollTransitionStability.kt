package com.project.analyzer.ac.telemetry.impl.internal.poll

internal class AcPollTransitionStability {

    private var lastNeedsFallback: Boolean = false
    private var pendingTransitionState: GameConnectionState? = null
    private var pendingTransitionDataSource: DataSourceType? = null
    private var pendingTransitionHits: Int = 0
    private var menuExitDebounceActive: Boolean = false

    fun stabilize(
        detection: AcPollDetection,
        currentState: GameConnectionState,
        currentDataSource: DataSourceType,
    ): AcPollDetection {
        val threshold = transitionConfirmationThreshold(detection, currentState)
        if (threshold <= 1) {
            clearPendingTransition()
            return detection
        }

        if (detection.state == currentState && detection.dataSource == currentDataSource) {
            clearPendingTransition()
            return detection
        }

        if (pendingTransitionState == detection.state && pendingTransitionDataSource == detection.dataSource) {
            pendingTransitionHits = (pendingTransitionHits + 1).coerceAtMost(threshold)
        } else {
            pendingTransitionState = detection.state
            pendingTransitionDataSource = detection.dataSource
            pendingTransitionHits = 1
        }

        if (pendingTransitionHits >= threshold) {
            clearPendingTransition()
            return detection
        }

        return detection.copy(
            state = currentState,
            dataSource = currentDataSource,
        )
    }

    fun onStateChanged(oldState: GameConnectionState, newState: GameConnectionState) {
        menuExitDebounceActive = when {
            oldState == GameConnectionState.IN_SESSION && newState == GameConnectionState.IN_MENU -> true
            newState == GameConnectionState.DISCONNECTED -> false
            oldState == GameConnectionState.IN_MENU && newState == GameConnectionState.IN_SESSION -> false
            else -> menuExitDebounceActive
        }
    }

    fun consumeFallbackChange(needsFallback: Boolean): Boolean {
        if (needsFallback == lastNeedsFallback) return false
        lastNeedsFallback = needsFallback
        return true
    }

    fun reset() {
        clearPendingTransition()
        menuExitDebounceActive = false
        lastNeedsFallback = false
    }

    private fun transitionConfirmationThreshold(detection: AcPollDetection, currentState: GameConnectionState): Int =
        when {
            menuExitDebounceActive &&
                currentState == GameConnectionState.IN_MENU &&
                detection.state == GameConnectionState.IN_SESSION -> MENU_EXIT_CONFIRMATION_SAMPLES

            else -> 1
        }

    private fun clearPendingTransition() {
        pendingTransitionState = null
        pendingTransitionDataSource = null
        pendingTransitionHits = 0
    }

    private companion object {
        const val MENU_EXIT_CONFIRMATION_SAMPLES = 10
    }
}
