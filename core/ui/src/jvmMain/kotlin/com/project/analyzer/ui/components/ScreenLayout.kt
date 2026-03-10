package com.project.analyzer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.analyzer.core.ui.Res.Res
import com.project.analyzer.core.ui.Res.screen_layout_preview_distance
import com.project.analyzer.core.ui.Res.screen_layout_preview_distance_value
import com.project.analyzer.core.ui.Res.screen_layout_preview_favorite_car
import com.project.analyzer.core.ui.Res.screen_layout_preview_favorite_car_value
import com.project.analyzer.core.ui.Res.screen_layout_preview_sessions
import com.project.analyzer.core.ui.Res.screen_layout_preview_sessions_value
import com.project.analyzer.core.ui.Res.screen_layout_preview_track
import com.project.analyzer.core.ui.Res.screen_layout_preview_track_value
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.scrollbar.AppVerticalScrollbar
import org.jetbrains.compose.resources.stringResource

@Composable
public fun ScrollableScreenColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    verticalSpacing: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SimAnalyzerTheme.material.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(contentPadding)
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            content = content,
        )
        AppVerticalScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(vertical = 4.dp),
            adapter = rememberScrollbarAdapter(scrollState),
        )
    }
}

@Composable
public fun FillWidthContent(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier.fillMaxWidth()) {
        content()
    }
}

@Preview
@Composable
private fun ScreenLayoutPreview() {
    SimAnalyzerTheme {
        ScrollableScreenColumn {
            StatsRow(
                stats = listOf(
                    StatItem(
                        title = stringResource(Res.string.screen_layout_preview_distance),
                        value = stringResource(Res.string.screen_layout_preview_distance_value),
                    ),
                    StatItem(
                        title = stringResource(Res.string.screen_layout_preview_sessions),
                        value = stringResource(Res.string.screen_layout_preview_sessions_value),
                    ),
                    StatItem(
                        title = stringResource(Res.string.screen_layout_preview_track),
                        value = stringResource(Res.string.screen_layout_preview_track_value),
                    ),
                ),
            )
            FillWidthContent {
                ResponsivePanelCard {
                    SessionStatCard(
                        title = stringResource(Res.string.screen_layout_preview_favorite_car),
                        value = stringResource(Res.string.screen_layout_preview_favorite_car_value),
                    )
                }
            }
        }
    }
}
