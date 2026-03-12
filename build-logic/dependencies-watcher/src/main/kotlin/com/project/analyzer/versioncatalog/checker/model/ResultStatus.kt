package com.project.analyzer.versioncatalog.checker.model

internal enum class ResultStatus {
    UpToDate,
    UpdateAvailable,
    NoCommonUpdate,
    FetchFailed,
}
