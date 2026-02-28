package com.project.analyzer.telemetry.recording.impl.file.engine

import com.project.analyzer.telemetry.recording.impl.file.model.ActiveSession
import com.project.analyzer.telemetry.recording.impl.file.model.SessionKey

internal class DefaultTelemetryFileActiveSessions : TelemetryFileActiveSessions {
    private val sessions = mutableMapOf<SessionKey, ActiveSession>()

    override fun put(key: SessionKey, session: ActiveSession) {
        sessions[key] = session
    }

    override fun get(key: SessionKey): ActiveSession? = sessions[key]

    override fun remove(key: SessionKey): ActiveSession? = sessions.remove(key)

    override fun keysForGame(gameId: String): List<SessionKey> = sessions.keys
        .asSequence()
        .filter { it.gameId == gameId }
        .toList()

    override fun values(): Collection<ActiveSession> = sessions.values

    override fun clear() {
        sessions.clear()
    }
}
