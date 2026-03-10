package com.analyzer.session.data.repository

import com.analyzer.session.data.model.RecordedSessionDetailPage
import com.analyzer.session.data.model.RecordedSessionListPage

public interface RecordedSessionRepository {

    public suspend fun loadSessionListPage(
        request: RecordedSessionListRequest,
        forceRefresh: Boolean = false,
    ): RecordedSessionListPage

    public suspend fun loadSessionDetailPage(
        sessionId: Long,
        request: RecordedSessionDetailRequest,
        forceRefresh: Boolean = false,
    ): RecordedSessionDetailPage?

    public suspend fun saveSession(sessionId: Long): Boolean

    public suspend fun deleteSession(sessionId: Long): Boolean
}
