package com.project.analyzer.versioncatalog.checker.model

import org.apache.maven.artifact.versioning.ComparableVersion

internal object VersionComparator {

    fun max(versions: Collection<String>): String? = versions.maxWithOrNull(::compare)

    fun compare(left: String, right: String): Int =
        ComparableVersion(left).compareTo(ComparableVersion(right))
}
