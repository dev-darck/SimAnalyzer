package com.analyzer.session.analysis.domain.usecase

import com.analyzer.session.analysis.domain.model.SessionAnalysisWorkspaceData
import com.analyzer.session.analysis.domain.model.SessionAnalysisWorkspaceRequest

/**
 * Screen-level contract that turns a session selection into fully prepared workspace data.
 */
internal interface SessionAnalysisUseCase {

    suspend fun loadWorkspaceShell(
        request: SessionAnalysisWorkspaceRequest,
        forceRefresh: Boolean = false,
    ): SessionAnalysisWorkspaceData?

    suspend fun enrichWorkspace(workspaceData: SessionAnalysisWorkspaceData): SessionAnalysisWorkspaceData
}
