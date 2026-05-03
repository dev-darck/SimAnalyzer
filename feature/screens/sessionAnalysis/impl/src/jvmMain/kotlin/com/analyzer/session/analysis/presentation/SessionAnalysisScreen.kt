package com.analyzer.session.analysis.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.analyzer.session.analysis.presentation.components.common.SessionAnalysisStudioPanel
import com.analyzer.session.analysis.presentation.components.layout.SessionAnalysisStudioLayout
import com.analyzer.session.analysis.presentation.model.SessionAnalysisBindSessionIntent
import com.analyzer.session.analysis.presentation.model.SessionAnalysisIntent
import com.analyzer.session.analysis.presentation.model.SessionAnalysisRefreshIntent
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSelectLapIntent
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSelectReferenceLapIntent
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSelectSessionIntent
import com.analyzer.session.analysis.presentation.model.SessionAnalysisState
import com.analyzer.session.analysis.presentation.preview.sessionAnalysisPreviewState
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_screen_building_message
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_screen_building_title
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_screen_empty_message
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_screen_empty_title
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_screen_unavailable_message
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_screen_unavailable_title
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_screen_updating_message
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_screen_updating_title
import com.project.analyzer.theme.SimAnalyzerTheme
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun SessionAnalysisScreen(
    sessionId: Long,
    initialSegmentId: Long? = null,
    initialLapNumber: Int? = null,
    initialReferenceSessionId: Long? = null,
    initialReferenceSegmentId: Long? = null,
    initialReferenceLapNumber: Int? = null,
) {
    val viewModel = metroViewModel<SessionAnalysisViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(
        sessionId,
        initialSegmentId,
        initialLapNumber,
        initialReferenceSessionId,
        initialReferenceSegmentId,
        initialReferenceLapNumber,
    ) {
        viewModel.dispatch(
            SessionAnalysisBindSessionIntent(
                sessionId = sessionId,
                segmentId = initialSegmentId,
                lapNumber = initialLapNumber,
                referenceSessionId = initialReferenceSessionId,
                referenceSegmentId = initialReferenceSegmentId,
                referenceLapNumber = initialReferenceLapNumber,
            ),
        )
    }

    SessionAnalysisContent(
        state = state,
        onIntent = viewModel::dispatch,
    )
}

@Composable
internal fun SessionAnalysisContent(
    state: SessionAnalysisState,
    modifier: Modifier = Modifier,
    onIntent: (SessionAnalysisIntent) -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SimAnalyzerTheme.material.background),
    ) {
        when {
            state.header != null -> {
                SessionAnalysisStudioLayout(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                    onRefresh = {
                        onIntent(SessionAnalysisRefreshIntent)
                    },
                    onSessionSelected = { segmentId ->
                        onIntent(SessionAnalysisSelectSessionIntent(segmentId))
                    },
                    onLapSelected = { lapNumber ->
                        onIntent(SessionAnalysisSelectLapIntent(lapNumber))
                    },
                    onReferenceLapSelected = { lapNumber ->
                        onIntent(SessionAnalysisSelectReferenceLapIntent(lapNumber))
                    },
                )
                if (state.isLoading) {
                    SessionAnalysisInlineLoadingOverlay(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(
                                top = SessionAnalysisUiTokens.overlayInset,
                                end = SessionAnalysisUiTokens.overlayInset,
                            ),
                    )
                }
            }

            state.isLoading -> {
                SessionAnalysisStatePanel(
                    title = stringResource(Res.string.session_analysis_screen_building_title),
                    message = stringResource(Res.string.session_analysis_screen_building_message),
                    loading = true,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            state.error != null -> {
                SessionAnalysisStatePanel(
                    title = stringResource(Res.string.session_analysis_screen_unavailable_title),
                    message = stringResource(Res.string.session_analysis_screen_unavailable_message),
                    loading = false,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            else -> {
                SessionAnalysisStatePanel(
                    title = stringResource(Res.string.session_analysis_screen_empty_title),
                    message = stringResource(Res.string.session_analysis_screen_empty_message),
                    loading = false,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun SessionAnalysisInlineLoadingOverlay(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(
                color = SimAnalyzerTheme.material.surface.copy(alpha = SessionAnalysisUiTokens.overlaySurfaceAlpha),
                shape = SimAnalyzerTheme.shapes.medium,
            )
            .padding(
                horizontal = SessionAnalysisUiTokens.panelPadding,
                vertical = SessionAnalysisUiTokens.compactPadding,
            ),
        horizontalArrangement = Arrangement.spacedBy(SessionAnalysisUiTokens.compactSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            color = SimAnalyzerTheme.material.primary,
            strokeWidth = SessionAnalysisUiTokens.progressStroke,
            modifier = Modifier
                .size(SessionAnalysisUiTokens.progressSize)
                .padding(end = SessionAnalysisUiTokens.progressEndPadding),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(SessionAnalysisUiTokens.microSpacing),
        ) {
            Text(
                text = stringResource(Res.string.session_analysis_screen_updating_title),
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.session_analysis_screen_updating_message),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun SessionAnalysisStatePanel(
    title: String,
    message: String,
    loading: Boolean,
    modifier: Modifier = Modifier,
) {
    SessionAnalysisStudioPanel(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(SessionAnalysisUiTokens.panelPadding),
            verticalArrangement = Arrangement.spacedBy(SessionAnalysisUiTokens.panelPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    color = SimAnalyzerTheme.material.primary,
                )
            }
            Text(
                text = title,
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = message,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview
@Composable
internal fun SessionAnalysisContentPreview() {
    SimAnalyzerTheme {
        SessionAnalysisContent(
            state = sessionAnalysisPreviewState(),
            modifier = Modifier.size(
                width = SessionAnalysisUiTokens.previewWidth,
                height = SessionAnalysisUiTokens.previewHeight,
            ),
        )
    }
}
