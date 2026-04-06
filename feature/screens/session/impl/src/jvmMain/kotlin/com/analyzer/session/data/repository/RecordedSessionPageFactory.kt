package com.analyzer.session.data.repository

import com.analyzer.session.data.analysis.IndexAnalysis
import com.analyzer.session.data.model.RecordedSessionDetailPage
import com.analyzer.session.data.model.RecordedSessionListPage
import com.analyzer.session.data.model.RecordedSessionSummary
import com.analyzer.session.data.model.SessionBundleLocation
import com.analyzer.session.data.model.SessionLocation
import com.analyzer.session.data.repository.page.detail.RecordedSessionDetailPageFactory
import com.analyzer.session.data.repository.page.list.RecordedSessionListPageFactory
import dev.zacsweers.metro.Inject

@Inject
internal class RecordedSessionPageFactory(
    private val listPageFactory: RecordedSessionListPageFactory,
    private val detailPageFactory: RecordedSessionDetailPageFactory,
) {

    fun buildSessionListPage(
        summaries: List<RecordedSessionSummary>,
        request: RecordedSessionListRequest,
    ): RecordedSessionListPage = listPageFactory.buildPage(
        summaries = summaries,
        request = request,
    )

    fun buildSessionDetailPage(
        bundle: SessionBundleLocation,
        request: RecordedSessionDetailRequest,
        analysisLoader: (SessionLocation) -> IndexAnalysis?,
    ): RecordedSessionDetailPage = detailPageFactory.buildPage(
        bundle = bundle,
        request = request,
        analysisLoader = analysisLoader,
    )
}
