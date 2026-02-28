package com.analyzer.session.domain.interactor

import com.analyzer.session.domain.model.SessionFilterOption
import com.analyzer.session.domain.model.SessionListDataset
import com.analyzer.session.domain.model.SessionListQuery
import com.analyzer.session.domain.model.SessionListResult
import com.analyzer.session.domain.usecase.SessionListDataUseCase
import com.analyzer.session.domain.usecase.SessionListProjectionUseCase
import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(ScreenScope::class)
class SessionListManager(
    private val dataUseCase: SessionListDataUseCase,
    private val projectionUseCase: SessionListProjectionUseCase,
) {

    suspend fun refresh(query: SessionListQuery): SessionListResult {
        val dataset = dataUseCase.loadDataset()
        return project(dataset, query.normalizeFor(dataset))
    }

    suspend fun project(dataset: SessionListDataset, query: SessionListQuery): SessionListResult {
        val normalizedQuery = query.normalizeFor(dataset)
        val projection = projectionUseCase.project(dataset, normalizedQuery)
        return SessionListResult(
            dataset = dataset,
            query = normalizedQuery.copy(page = projection.page),
            projection = projection,
        )
    }

    suspend fun saveSession(sessionId: Long): Boolean = dataUseCase.saveSession(sessionId)

    suspend fun deleteSession(sessionId: Long): Boolean = dataUseCase.deleteSession(sessionId)

    private fun SessionListQuery.normalizeFor(dataset: SessionListDataset): SessionListQuery = copy(
        gameId = gameId.resolveId(dataset.gameOptions),
        trackId = trackId.resolveId(dataset.trackOptions),
        carId = carId.resolveId(dataset.carOptions),
        dateId = dateId.resolveId(dataset.dateOptions),
        sortId = sortId.resolveId(dataset.sortOptions),
    )

    private fun String.resolveId(options: List<SessionFilterOption>): String =
        if (options.any { it.id == this }) this else options.firstOrNull()?.id ?: this
}
