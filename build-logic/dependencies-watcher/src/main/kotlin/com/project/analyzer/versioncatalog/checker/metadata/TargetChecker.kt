package com.project.analyzer.versioncatalog.checker.metadata

import com.project.analyzer.versioncatalog.checker.model.ConsumerCheckResult
import com.project.analyzer.versioncatalog.checker.model.MetadataFetchResult
import com.project.analyzer.versioncatalog.checker.model.ResultStatus
import com.project.analyzer.versioncatalog.checker.model.TargetCheckResult
import com.project.analyzer.versioncatalog.checker.model.VersionComparator
import com.project.analyzer.versioncatalog.checker.model.VersionTarget

internal class TargetChecker(
    private val metadataClient: MetadataClient,
) {

    fun check(target: VersionTarget): TargetCheckResult {
        val consumerResults = target.consumers.map { consumer ->
            when (val metadata = metadataClient.fetchMetadata(consumer.lookup)) {
                is MetadataFetchResult.Success -> {
                    val latestVersion = VersionComparator.max(metadata.versions)
                    ConsumerCheckResult(
                        consumer = consumer,
                        latestVersion = latestVersion,
                        newerVersions = metadata.versions.filterTo(linkedSetOf()) { version ->
                            VersionComparator.compare(version, target.currentVersion) > 0
                        },
                    )
                }

                is MetadataFetchResult.Failure -> ConsumerCheckResult(
                    consumer = consumer,
                    latestVersion = null,
                    newerVersions = emptySet(),
                    fetchError = metadata.reason,
                )
            }
        }

        if (consumerResults.any { it.fetchError != null }) {
            return TargetCheckResult(
                target = target,
                status = ResultStatus.FetchFailed,
                consumerResults = consumerResults,
            )
        }

        val latestCommonVersion = consumerResults
            .map(ConsumerCheckResult::newerVersions)
            .reduceOrNull { left, right -> left intersect right }
            ?.let(VersionComparator::max)

        return when {
            latestCommonVersion != null -> TargetCheckResult(
                target = target,
                status = ResultStatus.UpdateAvailable,
                latestCommonVersion = latestCommonVersion,
                consumerResults = consumerResults,
            )

            consumerResults.any { it.newerVersions.isNotEmpty() } -> TargetCheckResult(
                target = target,
                status = ResultStatus.NoCommonUpdate,
                consumerResults = consumerResults,
            )

            else -> TargetCheckResult(
                target = target,
                status = ResultStatus.UpToDate,
                consumerResults = consumerResults,
            )
        }
    }
}
