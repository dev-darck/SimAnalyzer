@file:Suppress("WildcardImport", "NoWildcardImports")

package com.analyzer.trackmap.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.analyzer.trackmap.presentation.components.TrackMapSectionCard
import com.analyzer.trackmap.presentation.model.TrackMapLibraryCardUi
import com.project.analyzer.feature.screens.trackMapLibrary.Res.Res
import com.project.analyzer.feature.screens.trackMapLibrary.Res.track_map_library_empty_subtitle
import com.project.analyzer.feature.screens.trackMapLibrary.Res.track_map_library_empty_title
import com.project.analyzer.feature.screens.trackMapLibrary.Res.track_map_library_subtitle
import com.project.analyzer.feature.screens.trackMapLibrary.Res.track_map_library_title
import com.project.analyzer.feature.screens.trackMapLibrary.Res.track_map_library_total
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.scrollbar.AppScrollbarAdapter
import com.project.analyzer.ui.scrollbar.AppVerticalScrollbar
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TrackMapLibraryListContent(
    modifier: Modifier = Modifier,
    items: ImmutableList<TrackMapLibraryCardUi>,
    onOpenEditor: (TrackMapLibraryCardUi) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SimAnalyzerTheme.material.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TrackMapSectionCard(
            title = stringResource(Res.string.track_map_library_title),
            subtitle = stringResource(Res.string.track_map_library_subtitle),
        ) {
            Text(
                text = stringResource(Res.string.track_map_library_total, items.size),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
        }

        if (items.isEmpty()) {
            TrackMapSectionCard(
                title = stringResource(Res.string.track_map_library_empty_title),
                subtitle = stringResource(Res.string.track_map_library_empty_subtitle),
            )
        } else {
            TrackMapLibraryItemsList(
                items = items,
                onOpenEditor = onOpenEditor,
            )
        }
    }
}

@Composable
private fun ColumnScope.TrackMapLibraryItemsList(
    items: ImmutableList<TrackMapLibraryCardUi>,
    onOpenEditor: (TrackMapLibraryCardUi) -> Unit,
) {
    val listState = rememberLazyListState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
    ) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize(),
        ) {
            items(
                items = items,
                key = TrackMapLibraryCardUi::mapKey,
            ) { item ->
                TrackMapLibraryCard(
                    card = item,
                    onOpenEditor = { onOpenEditor(item) },
                )
            }
        }
        AppVerticalScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(vertical = 4.dp),
            adapter = AppScrollbarAdapter(rememberScrollbarAdapter(listState)),
        )
    }
}
