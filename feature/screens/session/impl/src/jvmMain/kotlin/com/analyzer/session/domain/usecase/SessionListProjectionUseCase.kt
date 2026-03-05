package com.analyzer.session.domain.usecase

import com.analyzer.session.domain.model.SessionListDataset
import com.analyzer.session.domain.model.SessionListProjection
import com.analyzer.session.domain.model.SessionListQuery

interface SessionListProjectionUseCase {

    suspend fun project(dataset: SessionListDataset, query: SessionListQuery): SessionListProjection
}
