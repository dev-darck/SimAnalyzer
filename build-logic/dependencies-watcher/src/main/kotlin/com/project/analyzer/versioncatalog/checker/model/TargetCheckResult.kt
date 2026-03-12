package com.project.analyzer.versioncatalog.checker.model

internal data class TargetCheckResult(
    val target: VersionTarget,
    val status: ResultStatus,
    val latestCommonVersion: String? = null,
    val consumerResults: List<ConsumerCheckResult>,
)
