package com.analyzer.session.data.repository.cache

import com.analyzer.session.data.model.RecordedSessionSummary
import com.analyzer.session.data.model.SessionBundleLocation

internal data class BundleIndexCache(
    val rootPath: String,
    val fingerprint: RootFingerprint,
    val summaries: List<RecordedSessionSummary>,
    val bundles: List<SessionBundleLocation>,
    val sessionIndex: Map<Long, SessionBundleLocation>,
)
