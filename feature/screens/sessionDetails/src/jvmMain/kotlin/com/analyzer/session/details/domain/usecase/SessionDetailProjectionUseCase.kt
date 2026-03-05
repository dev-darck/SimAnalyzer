package com.analyzer.session.details.domain.usecase

import com.analyzer.session.details.domain.model.SessionDetailDataset
import com.analyzer.session.details.domain.model.SessionDetailProjection
import com.analyzer.session.details.domain.model.SessionDetailQuery

interface SessionDetailProjectionUseCase {

    suspend fun project(dataset: SessionDetailDataset, query: SessionDetailQuery): SessionDetailProjection
}
