package com.analyzer.session.domain.usecase

import com.analyzer.session.data.repository.RecordedSessionRepository
import com.analyzer.session.domain.mapper.SessionListDomainMapper
import com.analyzer.session.domain.model.SessionListDataset
import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(ScreenScope::class)
class SessionListDataUseCaseImpl(
    private val repository: RecordedSessionRepository,
    private val domainMapper: SessionListDomainMapper,
) : SessionListDataUseCase {

    override suspend fun loadDataset(): SessionListDataset {
        val sessions = repository.loadSessions()
        return domainMapper.map(sessions)
    }

    override suspend fun saveSession(sessionId: Long): Boolean = repository.saveSession(sessionId)

    override suspend fun deleteSession(sessionId: Long): Boolean = repository.deleteSession(sessionId)
}
