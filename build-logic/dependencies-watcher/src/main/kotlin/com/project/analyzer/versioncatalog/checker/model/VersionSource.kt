package com.project.analyzer.versioncatalog.checker.model

internal sealed interface VersionSource {

    val displayName: String

    data class Ref(val key: String) : VersionSource {

        override val displayName: String = "versions.$key"
    }

    data class Inline(val section: String, val alias: String) : VersionSource {

        override val displayName: String = "$section.$alias"
    }
}
