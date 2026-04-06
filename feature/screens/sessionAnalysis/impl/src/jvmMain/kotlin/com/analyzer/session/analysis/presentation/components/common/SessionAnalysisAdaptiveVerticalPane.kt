package com.analyzer.session.analysis.presentation.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.analyzer.session.analysis.presentation.components.layout.model.SessionAnalysisPaneLayoutMode
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_graph_title
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_hero_ideal
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_navigator_info_reference
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.scrollbar.AppScrollbarAdapter
import com.project.analyzer.ui.scrollbar.AppVerticalScrollbar
import org.jetbrains.compose.resources.stringResource

/**
 * Gives the workspace one vertical pane primitive that can switch between bounded and free layouts.
 */
@Composable
internal fun SessionAnalysisAdaptiveVerticalPane(
    layoutMode: SessionAnalysisPaneLayoutMode,
    modifier: Modifier = Modifier,
    itemSpacing: Dp,
    contentPadding: PaddingValues = PaddingValues(vertical = 4.dp),
    boundedScrollbarPadding: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (layoutMode == SessionAnalysisPaneLayoutMode.Bounded) {
        val listState = rememberLazyListState()
        Box(
            modifier = modifier.fillMaxSize(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(itemSpacing),
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(itemSpacing),
                        content = content,
                    )
                }
            }
            AppVerticalScrollbar(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(vertical = boundedScrollbarPadding),
                adapter = AppScrollbarAdapter(rememberScrollbarAdapter(listState)),
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
        content = content,
    )
}

@Preview
@Composable
internal fun SessionAnalysisAdaptiveVerticalPanePreview() {
    SimAnalyzerTheme {
        SessionAnalysisAdaptiveVerticalPane(
            layoutMode = SessionAnalysisPaneLayoutMode.Bounded,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            itemSpacing = 12.dp,
        ) {
            Text(
                text = stringResource(Res.string.session_analysis_graph_title),
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(Res.string.session_analysis_navigator_info_reference),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(Res.string.session_analysis_hero_ideal),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
        }
    }
}
