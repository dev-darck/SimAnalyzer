package com.project.analyzer.crash.domain.usecase

internal interface CopyReportUseCase {

    fun copyReport(text: String): Result<Unit>
}
