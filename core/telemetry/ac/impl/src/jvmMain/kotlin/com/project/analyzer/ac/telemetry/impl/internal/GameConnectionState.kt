package com.project.analyzer.ac.telemetry.impl.internal

/**
 * Game connection state detected by AcPollLoop
 */
enum class GameConnectionState {

    /** Game is not running - shared memory not attached */
    DISCONNECTED,

    /** Game is running but in menu (no active session) */
    IN_MENU,

    /** Game is running with active session (practice/race/etc) */
    IN_SESSION
}
