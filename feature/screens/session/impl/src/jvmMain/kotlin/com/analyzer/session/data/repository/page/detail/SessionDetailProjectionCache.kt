package com.analyzer.session.data.repository.page.detail

import com.analyzer.session.data.model.SessionBundleLocation

internal data class SessionDetailProjectionCache(
    val source: SessionBundleLocation,
    val key: SessionDetailProjectionKey,
    val data: SessionDetailProjectionData,
)
