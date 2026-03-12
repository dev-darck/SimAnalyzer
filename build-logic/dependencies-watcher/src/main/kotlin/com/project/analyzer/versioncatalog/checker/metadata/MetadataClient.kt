package com.project.analyzer.versioncatalog.checker.metadata

import com.project.analyzer.versioncatalog.checker.model.ArtifactLookup
import com.project.analyzer.versioncatalog.checker.model.DependencyWatcherConstants
import com.project.analyzer.versioncatalog.checker.model.MetadataFetchResult
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

internal class MetadataClient(
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(DependencyWatcherConstants.CONNECTION_TIMEOUT_SECONDS))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build(),
) {

    private val metadataCache = linkedMapOf<String, MetadataFetchResult>()

    fun fetchMetadata(lookup: ArtifactLookup): MetadataFetchResult {
        val cacheKey = buildString {
            append(lookup.coordinate)
            append('|')
            append(lookup.repositoryBaseUrls.joinToString(separator = ","))
        }
        metadataCache[cacheKey]?.let { return it }

        val result = runCatching {
            val failures = mutableListOf<String>()
            for (metadataUrl in lookup.metadataUrls()) {
                val request = HttpRequest.newBuilder(URI.create(metadataUrl))
                    .header("User-Agent", "SimAnalyzer-VersionCatalogChecker")
                    .timeout(Duration.ofSeconds(DependencyWatcherConstants.REQUEST_TIMEOUT_SECONDS))
                    .GET()
                    .build()
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))

                if (response.statusCode() != 200) {
                    failures += "${
                        metadataUrl.substringBefore("/m2/").ifBlank { metadataUrl }
                    } -> HTTP ${response.statusCode()}"
                    continue
                }

                val versions = DependencyWatcherConstants.versionTagRegex.findAll(response.body())
                    .map { match -> match.groupValues[1].trim() }
                    .filter(String::isNotBlank)
                    .toCollection(linkedSetOf())

                if (versions.isEmpty()) {
                    failures += "${metadataUrl.substringBefore("/m2/").ifBlank { metadataUrl }} -> empty metadata"
                    continue
                }

                return@runCatching MetadataFetchResult.Success(versions)
            }

            MetadataFetchResult.Failure("${lookup.coordinate}: ${failures.joinToString(separator = "; ")}")
        }.getOrElse { error ->
            MetadataFetchResult.Failure("${lookup.coordinate}: ${error.message ?: error::class.simpleName.orEmpty()}")
        }

        metadataCache[cacheKey] = result
        return result
    }
}
