package com.project.analyzer.ui.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.analyzer.ui.scrollbar.AppScrollbarAdapter
import com.project.analyzer.ui.scrollbar.AppVerticalScrollbar
import kotlin.math.floor

private enum class ResponsiveSize {
    Compact,
    Medium,
    Expanded,
}

public enum class ResponsiveGridMode {
    Staggered,
    Grid,
}

@Composable
public fun ResponsiveScreen(
    modifier: Modifier = Modifier,
    scrollContainerModifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    verticalSpacing: Dp = 16.dp,
    horizontalSpacing: Dp = 16.dp,
    compactMaxWidth: Dp = 900.dp,
    mediumMaxWidth: Dp = 1250.dp,
    mediumColumns: Int = 2,
    expandedColumns: Int = 2,
    mediumMinCellSize: Dp = 320.dp,
    expandedMinCellSize: Dp = 380.dp,
    gridMode: ResponsiveGridMode = ResponsiveGridMode.Staggered,
    backgroundColor: Color = Color.Transparent,
    content: ResponsiveScope.() -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current

    val currentContent by rememberUpdatedState(content)

    val effectivePadding = remember(contentPadding, verticalSpacing, layoutDirection) {
        PaddingValues(
            start = contentPadding.calculateStartPadding(layoutDirection),
            end = contentPadding.calculateEndPadding(layoutDirection),
            top = contentPadding.calculateTopPadding() + verticalSpacing,
            bottom = contentPadding.calculateBottomPadding() + verticalSpacing,
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
    ) {
        val size = remember(maxWidth, compactMaxWidth, mediumMaxWidth) {
            when {
                maxWidth <= compactMaxWidth -> ResponsiveSize.Compact
                maxWidth <= mediumMaxWidth -> ResponsiveSize.Medium
                else -> ResponsiveSize.Expanded
            }
        }

        when (size) {
            ResponsiveSize.Compact -> {
                val listState = rememberLazyListState()
                ResponsiveScrollHost(adapter = AppScrollbarAdapter(rememberScrollbarAdapter(listState))) {
                    LazyColumn(
                        state = listState,
                        modifier = scrollContainerModifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                        contentPadding = effectivePadding,
                    ) {
                        ListScopeAdapter(this).currentContent()
                    }
                }
            }

            ResponsiveSize.Medium -> {
                ResponsiveGrid(
                    availableWidth = maxWidth,
                    columns = mediumColumns,
                    gridMode = gridMode,
                    minCellSize = mediumMinCellSize,
                    effectivePadding = effectivePadding,
                    verticalSpacing = verticalSpacing,
                    horizontalSpacing = horizontalSpacing,
                    scrollContainerModifier = scrollContainerModifier,
                    content = currentContent,
                )
            }

            ResponsiveSize.Expanded -> {
                ResponsiveGrid(
                    availableWidth = maxWidth,
                    columns = expandedColumns,
                    gridMode = gridMode,
                    minCellSize = expandedMinCellSize,
                    effectivePadding = effectivePadding,
                    verticalSpacing = verticalSpacing,
                    horizontalSpacing = horizontalSpacing,
                    scrollContainerModifier = scrollContainerModifier,
                    content = currentContent,
                )
            }
        }
    }
}

@Composable
private fun ResponsiveGrid(
    availableWidth: Dp,
    columns: Int,
    gridMode: ResponsiveGridMode,
    minCellSize: Dp,
    effectivePadding: PaddingValues,
    verticalSpacing: Dp,
    horizontalSpacing: Dp,
    scrollContainerModifier: Modifier,
    content: ResponsiveScope.() -> Unit,
) {
    val safeColumns = columns.coerceAtLeast(1)
    val resolvedColumns = resolveColumnCount(
        availableWidth = availableWidth,
        maxColumns = safeColumns,
        minCellSize = minCellSize,
        horizontalSpacing = horizontalSpacing,
        effectivePadding = effectivePadding,
    )

    when (gridMode) {
        ResponsiveGridMode.Staggered -> {
            val gridState = rememberLazyStaggeredGridState()
            LazyVerticalStaggeredGrid(
                state = gridState,
                modifier = scrollContainerModifier.fillMaxSize(),
                columns = StaggeredGridCells.Fixed(resolvedColumns),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                verticalItemSpacing = verticalSpacing,
                contentPadding = effectivePadding,
            ) {
                StaggeredGridScopeAdapter(this).content()
            }
        }

        ResponsiveGridMode.Grid -> {
            val gridState = rememberLazyGridState()
            ResponsiveScrollHost(adapter = AppScrollbarAdapter(rememberScrollbarAdapter(gridState))) {
                LazyVerticalGrid(
                    state = gridState,
                    modifier = scrollContainerModifier.fillMaxSize(),
                    columns = GridCells.Fixed(resolvedColumns),
                    horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                    verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                    contentPadding = effectivePadding,
                ) {
                    GridScopeAdapter(this).content()
                }
            }
        }
    }
}

private fun resolveColumnCount(
    availableWidth: Dp,
    maxColumns: Int,
    minCellSize: Dp,
    horizontalSpacing: Dp,
    effectivePadding: PaddingValues,
): Int {
    if (maxColumns <= 1) return 1

    val contentWidth = availableWidth -
        effectivePadding.calculateLeftPadding(layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr) -
        effectivePadding.calculateRightPadding(layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr)
    val laneSize = minCellSize + horizontalSpacing
    val fittedColumns = floor((contentWidth + horizontalSpacing) / laneSize)
        .toInt()
        .coerceAtLeast(1)

    return fittedColumns.coerceAtMost(maxColumns)
}

@Composable
private fun ResponsiveScrollHost(adapter: AppScrollbarAdapter, content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        AppVerticalScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(vertical = 4.dp),
            adapter = adapter,
        )
    }
}
