package com.project.analyzer.versioncatalog.checker.model

internal sealed interface VersionResolution {
    data class Resolved(val source: VersionSource, val version: String) : VersionResolution
    data class Unresolved(val reason: String) : VersionResolution
}
