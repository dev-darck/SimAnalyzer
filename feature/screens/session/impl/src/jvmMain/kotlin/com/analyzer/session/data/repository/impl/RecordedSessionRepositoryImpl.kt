package com.analyzer.session.data.repository.impl

import com.analyzer.session.data.model.RecordedSessionDetailPage
import com.analyzer.session.data.model.RecordedSessionListPage
import com.analyzer.session.data.repository.RecordedSessionDetailRequest
import com.analyzer.session.data.repository.RecordedSessionListRequest
import com.analyzer.session.data.repository.RecordedSessionRepository
import com.project.analyzer.api.di.IO
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionSettings
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@Inject
@SingleIn(ScreenScope::class)
internal class RecordedSessionRepositoryImpl(
    settings: TelemetryAcquisitionSettings,
    json: Json,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : RecordedSessionRepository {

    private val bundleStore = RecordedSessionBundleStore(
        settings = settings,
        json = json,
    )
    private val pageFactory = RecordedSessionPageFactory()

    override suspend fun loadSessionListPage(
        request: RecordedSessionListRequest,
        forceRefresh: Boolean,
    ): RecordedSessionListPage =
        withContext(ioDispatcher) {
            pageFactory.buildSessionListPage(
                summaries = bundleStore.loadSummaries(forceRefresh = forceRefresh),
                request = request,
            )
        }

    override suspend fun loadSessionDetailPage(
        sessionId: Long,
        request: RecordedSessionDetailRequest,
        forceRefresh: Boolean,
    ): RecordedSessionDetailPage? = withContext(ioDispatcher) {
        val bundle = bundleStore.findBundle(
            sessionId = sessionId,
            forceRefresh = forceRefresh,
        ) ?: return@withContext null
        pageFactory.buildSessionDetailPage(
            bundle = bundle,
            request = request,
            analysisLoader = bundleStore::resolveAnalysis,
        )
    }

    override suspend fun saveSession(sessionId: Long): Boolean = withContext(ioDispatcher) {
        bundleStore.saveSession(sessionId)
    }

    override suspend fun deleteSession(sessionId: Long): Boolean = withContext(ioDispatcher) {
        bundleStore.deleteSession(sessionId)
    }
}
