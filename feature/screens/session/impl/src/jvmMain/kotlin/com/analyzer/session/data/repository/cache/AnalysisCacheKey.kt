package com.analyzer.session.data.repository.cache

internal data class AnalysisCacheKey(val absolutePath: String, val lastModified: Long, val sizeBytes: Long)
