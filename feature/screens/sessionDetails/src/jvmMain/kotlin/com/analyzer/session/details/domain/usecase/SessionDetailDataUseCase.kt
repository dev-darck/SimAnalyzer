package com.analyzer.session.details.domain.usecase

import com.analyzer.session.data.repository.RecordedSessionRepository
import com.analyzer.session.details.domain.mapper.SessionDetailDomainMapper
import com.analyzer.session.details.domain.model.SessionDetailDataset
import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(ScreenScope::class)
class SessionDetailDataUseCase(
    private val repository: RecordedSessionRepository,
    private val domainMapper: SessionDetailDomainMapper,
    private val thumbnailResolver: AceSavedCarThumbnailResolver,
) {

    suspend fun loadDataset(sessionId: Long): SessionDetailDataset? {
        val details = repository.loadSessionDetails(sessionId) ?: return null
        val thumbnail = thumbnailResolver.resolve(
            gameId = details.summary.gameId,
            carModel = details.summary.carModel,
            sessionStartedAtMs = details.summary.startedAtMs,
        )
        return domainMapper.map(details, thumbnail)
    }
}
