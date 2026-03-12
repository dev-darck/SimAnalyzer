package com.project.analyzer.versioncatalog.checker.model

internal sealed interface MetadataFetchResult {
    data class Success(val versions: Set<String>) : MetadataFetchResult
    data class Failure(val reason: String) : MetadataFetchResult
}
