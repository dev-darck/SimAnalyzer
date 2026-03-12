package com.project.analyzer.versioncatalog.checker.model

internal data class ConsumerCheckResult(
    val consumer: DependencyConsumer,
    val latestVersion: String?,
    val newerVersions: Set<String>,
    val fetchError: String? = null,
)
