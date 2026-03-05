package com.project.analyzer.crash.domain.usecase

internal interface OpenFileUseCase {

    fun openFile(path: String): Result<Unit>
}
