package com.analyzer.session.details.domain.usecase

import com.analyzer.session.details.domain.model.SessionDetailDataset

interface SessionDetailDataUseCase {

    suspend fun loadDataset(sessionId: Long): SessionDetailDataset?
}
