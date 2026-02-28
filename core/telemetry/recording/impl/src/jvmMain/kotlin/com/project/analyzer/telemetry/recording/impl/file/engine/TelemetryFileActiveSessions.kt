package com.project.analyzer.telemetry.recording.impl.file.engine

import com.project.analyzer.telemetry.recording.impl.file.model.ActiveSession
import com.project.analyzer.telemetry.recording.impl.file.model.SessionKey

internal interface TelemetryFileActiveSessions {
    fun put(key: SessionKey, session: ActiveSession)
    fun get(key: SessionKey): ActiveSession?
    fun remove(key: SessionKey): ActiveSession?
    fun keysForGame(gameId: String): List<SessionKey>
    fun values(): Collection<ActiveSession>
    fun clear()
}
