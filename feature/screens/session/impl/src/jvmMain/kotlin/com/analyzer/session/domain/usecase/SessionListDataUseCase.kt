package com.analyzer.session.domain.usecase

import com.analyzer.session.domain.model.SessionListPageResult
import com.analyzer.session.domain.model.SessionListQuery

interface SessionListDataUseCase {

    suspend fun loadPage(query: SessionListQuery, forceRefresh: Boolean = false): SessionListPageResult
    suspend fun saveSession(sessionId: Long): Boolean
    suspend fun deleteSession(sessionId: Long): Boolean
}
