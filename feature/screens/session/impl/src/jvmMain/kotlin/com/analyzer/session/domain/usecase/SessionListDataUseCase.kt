package com.analyzer.session.domain.usecase

import com.analyzer.session.data.repository.RecordedSessionRepository
import com.analyzer.session.domain.mapper.SessionListDomainMapper
import com.analyzer.session.domain.model.SessionListDataset
import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(ScreenScope::class)
class SessionListDataUseCase(
    private val repository: RecordedSessionRepository,
    private val domainMapper: SessionListDomainMapper,
) {

    suspend fun loadDataset(): SessionListDataset = domainMapper.map(repository.loadSessions())

    suspend fun saveSession(sessionId: Long): Boolean = repository.saveSession(sessionId)

    suspend fun deleteSession(sessionId: Long): Boolean = repository.deleteSession(sessionId)
}
