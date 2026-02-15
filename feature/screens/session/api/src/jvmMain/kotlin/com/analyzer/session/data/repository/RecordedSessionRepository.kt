package com.analyzer.session.data.repository

import com.analyzer.session.data.model.RecordedSessionDetail
import com.analyzer.session.data.model.RecordedSessionSummary

public interface RecordedSessionRepository {

    public suspend fun loadSessions(): List<RecordedSessionSummary>

    public suspend fun loadSessionDetails(sessionId: Long): RecordedSessionDetail?

    public suspend fun saveSession(sessionId: Long): Boolean

    public suspend fun deleteSession(sessionId: Long): Boolean
}
