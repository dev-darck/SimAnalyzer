@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.analyzer.session.analysis.presentation

import com.analyzer.session.analysis.domain.model.SessionAnalysisWorkspaceData
import com.analyzer.session.analysis.domain.model.SessionAnalysisWorkspaceRequest
import com.analyzer.session.analysis.domain.usecase.SessionAnalysisUseCase
import com.analyzer.session.analysis.presentation.model.SessionAnalysisBindSessionIntent
import com.analyzer.session.analysis.presentation.model.SessionAnalysisError
import com.analyzer.session.analysis.presentation.model.SessionAnalysisScreenMode
import com.analyzer.session.analysis.presentation.model.SessionAnalysisRefreshIntent
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSelectLapIntent
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSelectReferenceLapIntent
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSelectSessionIntent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionAnalysisViewModelTest {

    @Test
    fun `bindSession publishes shell state before enrichment completes`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val shellWorkspace = sessionAnalysisWorkspaceData()
            val enrichGate = CompletableDeferred<Unit>()
            val useCase = FakeSessionAnalysisUseCase(
                shellWorkspace = shellWorkspace,
                enrichedWorkspace = shellWorkspace,
                enrichGate = enrichGate,
            )
            val viewModel = SessionAnalysisViewModel(useCase, dispatcher)

            viewModel.dispatch(SessionAnalysisBindSessionIntent(77L))
            advanceUntilIdle()

            assertNotNull(viewModel.state.value.header)
            assertTrue(viewModel.state.value.isLoading)
            assertEquals(SessionAnalysisScreenMode.Analysis, viewModel.state.value.screenMode)
            assertEquals(1, useCase.shellRequests.size)
            assertEquals(1, useCase.enrichRequests.size)

            enrichGate.complete(Unit)
            advanceUntilIdle()

            assertFalse(viewModel.state.value.isLoading)
            assertNotNull(viewModel.state.value.summary)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `bindSession skips reload when the same session is already loaded`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val workspace = sessionAnalysisWorkspaceData()
            val useCase = FakeSessionAnalysisUseCase(
                shellWorkspace = workspace,
                enrichedWorkspace = workspace,
            )
            val viewModel = SessionAnalysisViewModel(useCase, dispatcher)

            viewModel.dispatch(SessionAnalysisBindSessionIntent(77L))
            advanceUntilIdle()
            viewModel.dispatch(SessionAnalysisBindSessionIntent(77L))
            advanceUntilIdle()

            assertEquals(1, useCase.shellRequests.size)
            assertEquals(1, useCase.enrichRequests.size)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `refresh forces reload for the currently bound session`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val workspace = sessionAnalysisWorkspaceData()
            val useCase = FakeSessionAnalysisUseCase(
                shellWorkspace = workspace,
                enrichedWorkspace = workspace,
            )
            val viewModel = SessionAnalysisViewModel(useCase, dispatcher)

            viewModel.dispatch(SessionAnalysisBindSessionIntent(77L))
            advanceUntilIdle()
            viewModel.dispatch(SessionAnalysisRefreshIntent)
            advanceUntilIdle()

            assertEquals(
                listOf(
                    SessionAnalysisWorkspaceRequest(sessionId = 77L) to false,
                    SessionAnalysisWorkspaceRequest(sessionId = 77L) to true,
                ),
                useCase.shellRequests,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `selectSession switches the active segment`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val workspace = sessionAnalysisWorkspaceData()
            val useCase = FakeSessionAnalysisUseCase(
                shellWorkspace = workspace,
                enrichedWorkspace = workspace,
            )
            val viewModel = SessionAnalysisViewModel(useCase, dispatcher)

            viewModel.dispatch(SessionAnalysisBindSessionIntent(77L))
            advanceUntilIdle()
            viewModel.dispatch(SessionAnalysisSelectSessionIntent(101L))
            advanceUntilIdle()

            assertEquals(101L, viewModel.state.value.selectedSegmentId)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `selectLap switches the active lap`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val workspace = sessionAnalysisWorkspaceData()
            val useCase = FakeSessionAnalysisUseCase(
                shellWorkspace = workspace,
                enrichedWorkspace = workspace,
            )
            val viewModel = SessionAnalysisViewModel(useCase, dispatcher)

            viewModel.dispatch(SessionAnalysisBindSessionIntent(77L))
            advanceUntilIdle()
            viewModel.dispatch(SessionAnalysisSelectLapIntent(1))
            advanceUntilIdle()

            assertEquals(1, viewModel.state.value.selectedLapNumber)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `selectReferenceLap keeps a custom reference selection`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val workspace = sessionAnalysisWorkspaceData()
            val useCase = FakeSessionAnalysisUseCase(
                shellWorkspace = workspace,
                enrichedWorkspace = workspace,
            )
            val viewModel = SessionAnalysisViewModel(useCase, dispatcher)

            viewModel.dispatch(SessionAnalysisBindSessionIntent(77L))
            advanceUntilIdle()
            viewModel.dispatch(SessionAnalysisSelectLapIntent(1))
            advanceUntilIdle()
            viewModel.dispatch(SessionAnalysisSelectReferenceLapIntent(2))
            advanceUntilIdle()

            assertEquals(2, viewModel.state.value.referenceLapNumber)
            assertTrue(viewModel.state.value.referenceLapIsCustom)
            assertEquals(SessionAnalysisScreenMode.Comparison, viewModel.state.value.screenMode)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `bindSession applies initial lap comparison selection`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val workspace = sessionAnalysisWorkspaceData()
            val useCase = FakeSessionAnalysisUseCase(
                shellWorkspace = workspace,
                enrichedWorkspace = workspace,
            )
            val viewModel = SessionAnalysisViewModel(useCase, dispatcher)

            viewModel.dispatch(
                SessionAnalysisBindSessionIntent(
                    sessionId = 77L,
                    segmentId = 101L,
                    lapNumber = 2,
                    referenceLapNumber = 1,
                ),
            )
            advanceUntilIdle()

            assertEquals(101L, viewModel.state.value.selectedSegmentId)
            assertEquals(2, viewModel.state.value.selectedLapNumber)
            assertEquals(1, viewModel.state.value.referenceLapNumber)
            assertTrue(viewModel.state.value.referenceLapIsCustom)
            assertEquals(SessionAnalysisScreenMode.Comparison, viewModel.state.value.screenMode)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `bindSession forwards external reference session selection`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val workspace = sessionAnalysisWorkspaceData()
            val externalReferenceReport = sessionAnalysisWorkspaceData(sessionId = 88L).report
            val useCase = FakeSessionAnalysisUseCase(
                shellWorkspace = workspace.copy(referenceReport = externalReferenceReport),
                enrichedWorkspace = workspace.copy(referenceReport = externalReferenceReport),
            )
            val viewModel = SessionAnalysisViewModel(useCase, dispatcher)

            viewModel.dispatch(
                SessionAnalysisBindSessionIntent(
                    sessionId = 77L,
                    segmentId = 101L,
                    lapNumber = 2,
                    referenceSessionId = 88L,
                    referenceSegmentId = 202L,
                    referenceLapNumber = 1,
                ),
            )
            advanceUntilIdle()

            assertEquals(
                listOf(
                    SessionAnalysisWorkspaceRequest(
                        sessionId = 77L,
                        referenceSessionId = 88L,
                    ) to false,
                ),
                useCase.shellRequests,
            )
            assertEquals(SessionAnalysisScreenMode.Comparison, viewModel.state.value.screenMode)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `bindSession shows unavailable error when shell workspace is missing`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val useCase = FakeSessionAnalysisUseCase(
                shellWorkspace = null,
                enrichedWorkspace = sessionAnalysisWorkspaceData(),
            )
            val viewModel = SessionAnalysisViewModel(useCase, dispatcher)

            viewModel.dispatch(SessionAnalysisBindSessionIntent(77L))
            advanceUntilIdle()

            assertEquals(SessionAnalysisError.Unavailable, viewModel.state.value.error)
            assertNull(viewModel.state.value.header)
            assertFalse(viewModel.state.value.isLoading)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `refresh preserves the current state when shell reload fails`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val workspace = sessionAnalysisWorkspaceData()
            val useCase = FakeSessionAnalysisUseCase(
                shellWorkspace = workspace,
                enrichedWorkspace = workspace,
            )
            val viewModel = SessionAnalysisViewModel(useCase, dispatcher)

            viewModel.dispatch(SessionAnalysisBindSessionIntent(77L))
            advanceUntilIdle()
            useCase.shellError = IllegalStateException("reload failed")

            viewModel.dispatch(SessionAnalysisRefreshIntent)
            advanceUntilIdle()

            assertNotNull(viewModel.state.value.header)
            assertFalse(viewModel.state.value.isLoading)
            assertNull(viewModel.state.value.error)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `enrichment failure keeps shell state visible and stops loading`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val workspace = sessionAnalysisWorkspaceData()
            val useCase = FakeSessionAnalysisUseCase(
                shellWorkspace = workspace,
                enrichedWorkspace = workspace,
                enrichError = IllegalStateException("boom"),
            )
            val viewModel = SessionAnalysisViewModel(useCase, dispatcher)

            viewModel.dispatch(SessionAnalysisBindSessionIntent(77L))
            advanceUntilIdle()

            assertNotNull(viewModel.state.value.header)
            assertFalse(viewModel.state.value.isLoading)
            assertNull(viewModel.state.value.summary)
            assertNull(viewModel.state.value.error)
        } finally {
            Dispatchers.resetMain()
        }
    }
}

private class FakeSessionAnalysisUseCase(
    private val shellWorkspace: SessionAnalysisWorkspaceData?,
    private val enrichedWorkspace: SessionAnalysisWorkspaceData,
    private val enrichGate: CompletableDeferred<Unit>? = null,
    var shellError: Throwable? = null,
    var enrichError: Throwable? = null,
) : SessionAnalysisUseCase {

    val shellRequests = mutableListOf<Pair<SessionAnalysisWorkspaceRequest, Boolean>>()
    val enrichRequests = mutableListOf<SessionAnalysisWorkspaceData>()

    override suspend fun loadWorkspaceShell(
        request: SessionAnalysisWorkspaceRequest,
        forceRefresh: Boolean,
    ): SessionAnalysisWorkspaceData? {
        shellRequests += request to forceRefresh
        shellError?.let { throw it }
        return shellWorkspace
    }

    override suspend fun enrichWorkspace(workspaceData: SessionAnalysisWorkspaceData): SessionAnalysisWorkspaceData {
        enrichRequests += workspaceData
        enrichGate?.await()
        enrichError?.let { throw it }
        return enrichedWorkspace
    }
}
