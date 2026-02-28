package com.analyzer.session.details.domain.interactor

import com.analyzer.session.details.domain.model.SESSION_DETAIL_TYPE_ALL
import com.analyzer.session.details.domain.model.SESSION_DETAIL_TYPE_AUTO
import com.analyzer.session.details.domain.model.SessionDetailDataset
import com.analyzer.session.details.domain.model.SessionDetailQuery
import com.analyzer.session.details.domain.model.SessionDetailResult
import com.analyzer.session.details.domain.usecase.SessionDetailDataUseCase
import com.analyzer.session.details.domain.usecase.SessionDetailProjectionUseCase
import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(ScreenScope::class)
class SessionDetailManager(
    private val dataUseCase: SessionDetailDataUseCase,
    private val projectionUseCase: SessionDetailProjectionUseCase,
) {

    suspend fun load(sessionId: Long, query: SessionDetailQuery): SessionDetailResult? {
        val dataset = dataUseCase.loadDataset(sessionId) ?: return null
        return project(dataset, query)
    }

    suspend fun project(dataset: SessionDetailDataset, query: SessionDetailQuery): SessionDetailResult {
        val normalizedQuery = normalizeQuery(dataset, query)
        val projection = projectionUseCase.project(dataset, normalizedQuery)
        return SessionDetailResult(
            dataset = dataset,
            query = normalizedQuery.copy(page = projection.page),
            projection = projection,
        )
    }

    private fun normalizeQuery(dataset: SessionDetailDataset, query: SessionDetailQuery): SessionDetailQuery {
        val availableTypeIds = dataset.sessionTypeOptions.mapTo(LinkedHashSet()) { it.id }
        val resolvedSessionTypeId = when {
            query.sessionTypeId == SESSION_DETAIL_TYPE_AUTO && dataset.sessionTypeOptions.isNotEmpty() -> dataset.defaultSessionTypeId
            query.sessionTypeId == SESSION_DETAIL_TYPE_AUTO -> SESSION_DETAIL_TYPE_ALL
            query.sessionTypeId == SESSION_DETAIL_TYPE_ALL -> SESSION_DETAIL_TYPE_ALL
            availableTypeIds.contains(query.sessionTypeId) -> query.sessionTypeId
            dataset.sessionTypeOptions.isNotEmpty() -> dataset.defaultSessionTypeId
            else -> SESSION_DETAIL_TYPE_ALL
        }
        return query.copy(sessionTypeId = resolvedSessionTypeId)
    }
}
