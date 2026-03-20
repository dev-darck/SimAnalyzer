package com.analyzer.session.data.repository.cache

internal data class RootFingerprint(
    val rootLastModified: Long,
    val directoryCount: Int,
    val directoryHash: Long,
)
