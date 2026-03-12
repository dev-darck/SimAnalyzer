package com.project.analyzer.versioncatalog.checker.model

internal data class VersionTarget(
    val key: String,
    val source: VersionSource,
    val currentVersion: String,
    val consumers: MutableList<DependencyConsumer> = mutableListOf(),
)
