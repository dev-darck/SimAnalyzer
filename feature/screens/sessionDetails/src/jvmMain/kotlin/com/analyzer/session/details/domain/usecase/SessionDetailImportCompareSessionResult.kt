package com.analyzer.session.details.domain.usecase

internal sealed interface SessionDetailImportCompareSessionResult {

    data class Imported(val sessionId: Long, val destinationPath: String) : SessionDetailImportCompareSessionResult

    data class AlreadyAvailable(val sessionId: Long) : SessionDetailImportCompareSessionResult

    data class Rejected(val reason: String) : SessionDetailImportCompareSessionResult

    data class Failure(val reason: String) : SessionDetailImportCompareSessionResult
}
