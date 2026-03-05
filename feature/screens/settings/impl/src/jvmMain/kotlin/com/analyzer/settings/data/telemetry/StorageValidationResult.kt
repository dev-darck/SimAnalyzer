package com.analyzer.settings.data.telemetry

sealed interface StorageValidationResult {
    data object Valid : StorageValidationResult
    data object Empty : StorageValidationResult
    data object NotAbsolutePath : StorageValidationResult
    data object NotADirectory : StorageValidationResult
    data object NotWritable : StorageValidationResult
    data object CannotCreate : StorageValidationResult
}
