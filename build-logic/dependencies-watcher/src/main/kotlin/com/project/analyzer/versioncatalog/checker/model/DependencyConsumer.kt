package com.project.analyzer.versioncatalog.checker.model

internal data class DependencyConsumer(
    val kind: ConsumerKind,
    val alias: String,
    val displayName: String,
    val lookup: ArtifactLookup,
)
