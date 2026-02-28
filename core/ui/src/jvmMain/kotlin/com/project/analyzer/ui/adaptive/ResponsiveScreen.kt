package com.project.analyzer.ui.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    verticalSpacing: Dp = 16.dp,
    horizontalSpacing: Dp = 16.dp,
    compactMaxWidth: Dp = 900.dp,
    mediumMaxWidth: Dp = 1250.dp,
    mediumColumns: Int = 2,
    expandedColumns: Int = 2,
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                    contentPadding = effectivePadding,
                ) {
                    ListScopeAdapter(this).currentContent()
                }
            }

            ResponsiveSize.Medium -> {
                ResponsiveGrid(
                    columns = mediumColumns,
                    gridMode = gridMode,
                    effectivePadding = effectivePadding,
                    verticalSpacing = verticalSpacing,
                    horizontalSpacing = horizontalSpacing,
                    content = currentContent,
                )
            }

            ResponsiveSize.Expanded -> {
                ResponsiveGrid(
                    columns = expandedColumns,
                    gridMode = gridMode,
                    effectivePadding = effectivePadding,
                    verticalSpacing = verticalSpacing,
                    horizontalSpacing = horizontalSpacing,
                    content = currentContent,
                )
            }
        }
    }
}

@Composable
private fun ResponsiveGrid(
    columns: Int,
    gridMode: ResponsiveGridMode,
    effectivePadding: PaddingValues,
    verticalSpacing: Dp,
    horizontalSpacing: Dp,
    content: ResponsiveScope.() -> Unit,
) {
    val safeColumns = columns.coerceAtLeast(1)

    when (gridMode) {
        ResponsiveGridMode.Staggered -> {
            LazyVerticalStaggeredGrid(
                modifier = Modifier.fillMaxSize(),
                columns = StaggeredGridCells.Fixed(safeColumns),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                verticalItemSpacing = verticalSpacing,
                contentPadding = effectivePadding,
            ) {
                StaggeredGridScopeAdapter(this).content()
            }
        }

        ResponsiveGridMode.Grid -> {
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                columns = GridCells.Fixed(safeColumns),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                contentPadding = effectivePadding,
            ) {
                GridScopeAdapter(this).content()
            }
        }
    }
}
