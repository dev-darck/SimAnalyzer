package com.project.analyzer.versioncatalog.checker.report

import com.project.analyzer.versioncatalog.checker.model.CommandMode
import com.project.analyzer.versioncatalog.checker.model.ConsumerCheckResult
import com.project.analyzer.versioncatalog.checker.model.ResultStatus
import com.project.analyzer.versioncatalog.checker.model.TargetCheckResult
import com.project.analyzer.versioncatalog.checker.model.VersionComparator
import java.io.File
import java.nio.charset.StandardCharsets

internal class DependencyWatcherReporter(
    private val diagnostics: List<String>,
) {

    fun printSummary(catalogPath: String, results: List<TargetCheckResult>) {
        println("Version catalog: $catalogPath")
        println("Checked ${results.size} target(s)")
        println()

        results.forEach { result ->
            when (result.status) {
                ResultStatus.UpToDate -> println("[UP_TO_DATE] ${result.target.key} = ${result.target.currentVersion}")
                ResultStatus.UpdateAvailable -> println(
                    "[UPDATE_AVAILABLE] ${result.target.key}: ${result.target.currentVersion} -> ${result.latestCommonVersion}"
                )

                ResultStatus.NoCommonUpdate -> println("[NO_COMMON_UPDATE] ${result.target.key} = ${result.target.currentVersion}")
                ResultStatus.FetchFailed -> println("[FETCH_FAILED] ${result.target.key} = ${result.target.currentVersion}")
            }

            result.consumerResults.forEach { consumerResult ->
                println(
                    "  - ${consumerResult.consumer.displayName} -> ${consumerResult.consumer.lookup.coordinate} " +
                        "(${buildConsumerSuffix(consumerResult)})"
                )
            }
            println()
        }

        if (diagnostics.isNotEmpty()) {
            println("Diagnostics:")
            diagnostics.forEach { diagnostic ->
                println("  - $diagnostic")
            }
            println()
        }

        val updates = results.count { it.status == ResultStatus.UpdateAvailable }
        val conflicts = results.count { it.status == ResultStatus.NoCommonUpdate }
        val fetchFailures = results.count { it.status == ResultStatus.FetchFailed }
        println("Summary: updates=$updates, noCommonUpdate=$conflicts, fetchFailed=$fetchFailures, diagnostics=${diagnostics.size}")
    }

    fun writeGithubOutputs(results: List<TargetCheckResult>) {
        val outputPath = System.getenv("GITHUB_OUTPUT") ?: return
        val outputFile = File(outputPath)
        val updates = results.count { it.status == ResultStatus.UpdateAvailable }
        val conflicts = results.count { it.status == ResultStatus.NoCommonUpdate }
        val fetchFailures = results.count { it.status == ResultStatus.FetchFailed }
        val needsAttention = updates > 0 || conflicts > 0 || fetchFailures > 0 || diagnostics.isNotEmpty()

        outputFile.appendText(
            buildString {
                appendLine("checked_targets=${results.size}")
                appendLine("updates_available=$updates")
                appendLine("no_common_updates=$conflicts")
                appendLine("fetch_failures=$fetchFailures")
                appendLine("diagnostics_count=${diagnostics.size}")
                appendLine("needs_attention=$needsAttention")
            },
            StandardCharsets.UTF_8,
        )
    }

    fun failIfNeeded(command: CommandMode, results: List<TargetCheckResult>) {
        if (command != CommandMode.Verify) return

        val hasFailures = diagnostics.isNotEmpty() || results.any { result ->
            result.status != ResultStatus.UpToDate
        }
        if (hasFailures) {
            error("Version catalog verification failed.")
        }
    }

    private fun buildConsumerSuffix(consumerResult: ConsumerCheckResult): String =
        when {
            consumerResult.fetchError != null -> "fetch failed: ${consumerResult.fetchError}"
            consumerResult.latestVersion == null -> "latest unavailable"
            consumerResult.newerVersions.isEmpty() -> "latest ${consumerResult.latestVersion}"

            else -> {
                val latestConsumerVersion =
                    VersionComparator.max(consumerResult.newerVersions) ?: consumerResult.latestVersion
                "latest ${consumerResult.latestVersion}, next common candidate $latestConsumerVersion"
            }
        }
}
