package com.analyzer.session.analysis.domain.usecase

import com.analyzer.session.analysis.domain.model.SessionAnalysisWorkspaceData

/**
 * Screen-level contract that turns a session selection into fully prepared workspace data.
 */
internal interface SessionAnalysisUseCase {

    suspend fun loadWorkspaceShell(sessionId: Long, forceRefresh: Boolean = false): SessionAnalysisWorkspaceData?

    suspend fun enrichWorkspace(workspaceData: SessionAnalysisWorkspaceData): SessionAnalysisWorkspaceData
}
