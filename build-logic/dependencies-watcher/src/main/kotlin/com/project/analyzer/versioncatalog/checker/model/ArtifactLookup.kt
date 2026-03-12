package com.project.analyzer.versioncatalog.checker.model

internal data class ArtifactLookup(
    val repositoryBaseUrls: List<String>,
    val groupId: String,
    val artifactId: String,
) {

    val coordinate: String = "$groupId:$artifactId"

    fun metadataUrls(): List<String> = repositoryBaseUrls.map { repositoryBaseUrl ->
        buildString {
            append(repositoryBaseUrl.trimEnd('/'))
            append('/')
            append(groupId.replace('.', '/'))
            append('/')
            append(artifactId)
            append("/maven-metadata.xml")
        }
    }
}
