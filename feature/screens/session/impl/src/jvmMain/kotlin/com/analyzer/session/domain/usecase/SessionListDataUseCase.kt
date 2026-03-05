package com.analyzer.session.domain.usecase

import com.analyzer.session.domain.model.SessionListDataset

interface SessionListDataUseCase {

    suspend fun loadDataset(): SessionListDataset
    suspend fun saveSession(sessionId: Long): Boolean
    suspend fun deleteSession(sessionId: Long): Boolean
}
