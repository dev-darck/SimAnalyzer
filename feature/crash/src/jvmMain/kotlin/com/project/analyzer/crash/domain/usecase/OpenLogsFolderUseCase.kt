package com.project.analyzer.crash.domain.usecase

internal interface OpenLogsFolderUseCase {

    fun openLogsFolder(path: String): Result<Unit>
}
