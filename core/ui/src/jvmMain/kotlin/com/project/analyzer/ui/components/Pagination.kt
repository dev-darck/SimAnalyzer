package com.project.analyzer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.analyzer.core.ui.Res.Res
import com.project.analyzer.core.ui.Res.pagination_next_page
import com.project.analyzer.core.ui.Res.pagination_previous_page
import com.project.analyzer.theme.SimAnalyzerTheme
import org.jetbrains.compose.resources.stringResource

public sealed interface PaginationItemUi {

    public data object Ellipsis : PaginationItemUi

    public data class Page(val value: Int, val isSelected: Boolean) : PaginationItemUi
}

public data class PaginationUi(
    val currentPage: Int,
    val pageCount: Int,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    val items: List<PaginationItemUi>,
)

public fun buildPaginationUi(page: Int, pageCount: Int): PaginationUi {
    val safePageCount = pageCount.coerceAtLeast(0)
    val safePage = when {
        safePageCount == 0 -> 0
        else -> page.coerceIn(1, safePageCount)
    }

    return PaginationUi(
        currentPage = safePage,
        pageCount = safePageCount,
        canGoBack = safePage > 1,
        canGoForward = safePage in 1 until safePageCount,
        items = buildPaginationItems(
            page = safePage,
            pageCount = safePageCount,
        ),
    )
}

@Composable
public fun Pagination(page: Int, pageCount: Int, onPageChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val paginationUi = remember(page, pageCount) { buildPaginationUi(page = page, pageCount = pageCount) }
    Pagination(
        pagination = paginationUi,
        onPageChange = onPageChange,
        modifier = modifier,
    )
}

@Composable
public fun Pagination(pagination: PaginationUi, onPageChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    if (pagination.pageCount <= 1) return

    val pageItemShape = SimAnalyzerTheme.corners.compact

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.ChevronLeft,
            contentDescription = stringResource(Res.string.pagination_previous_page),
            tint = if (pagination.canGoBack) {
                SimAnalyzerTheme.material.onSurfaceVariant
            } else {
                SimAnalyzerTheme.material.onSurfaceVariant.copy(alpha = 0.3f)
            },
            modifier = Modifier
                .size(18.dp)
                .clickable(enabled = pagination.canGoBack) { onPageChange(pagination.currentPage - 1) },
        )

        pagination.items.forEach { item ->
            when (item) {
                PaginationItemUi.Ellipsis -> {
                    Text(
                        text = "...",
                        style = SimAnalyzerTheme.typography.labelSmall,
                        color = SimAnalyzerTheme.material.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 2.dp),
                    )
                }

                is PaginationItemUi.Page -> {
                    Box(
                        modifier = Modifier
                            .clip(pageItemShape)
                            .background(
                                if (item.isSelected) {
                                    SimAnalyzerTheme.material.primary.copy(alpha = 0.2f)
                                } else {
                                    SimAnalyzerTheme.chrome.fillMuted
                                },
                            )
                            .clickable(enabled = !item.isSelected) { onPageChange(item.value) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = item.value.toString(),
                            style = SimAnalyzerTheme.typography.labelSmall,
                            color = if (item.isSelected) {
                                SimAnalyzerTheme.material.primary
                            } else {
                                SimAnalyzerTheme.material.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }

        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = stringResource(Res.string.pagination_next_page),
            tint = if (pagination.canGoForward) {
                SimAnalyzerTheme.material.onSurfaceVariant
            } else {
                SimAnalyzerTheme.material.onSurfaceVariant.copy(alpha = 0.3f)
            },
            modifier = Modifier
                .size(18.dp)
                .clickable(enabled = pagination.canGoForward) { onPageChange(pagination.currentPage + 1) },
        )
    }
}

private fun buildPaginationItems(page: Int, pageCount: Int): List<PaginationItemUi> {
    if (pageCount <= 0) return emptyList()

    if (pageCount <= 5) {
        return (1..pageCount).map { item ->
            PaginationItemUi.Page(
                value = item,
                isSelected = item == page,
            )
        }
    }

    val items = mutableListOf<PaginationItemUi>()
    items.add(PaginationItemUi.Page(value = 1, isSelected = page == 1))

    val windowStart = (page - 1).coerceAtLeast(2)
    val windowEnd = (page + 1).coerceAtMost(pageCount - 1)

    if (windowStart > 2) {
        items.add(PaginationItemUi.Ellipsis)
    }

    for (i in windowStart..windowEnd) {
        items.add(PaginationItemUi.Page(value = i, isSelected = i == page))
    }

    if (windowEnd < pageCount - 1) {
        items.add(PaginationItemUi.Ellipsis)
    }

    items.add(PaginationItemUi.Page(value = pageCount, isSelected = page == pageCount))
    return items
}

@Preview(name = "Pagination Middle")
@Composable
private fun PaginationPreview() {
    SimAnalyzerTheme {
        Pagination(
            page = 4,
            pageCount = 9,
            onPageChange = {},
        )
    }
}

@Preview(name = "Pagination Edge")
@Composable
private fun PaginationEdgePreview() {
    SimAnalyzerTheme {
        Pagination(
            page = 1,
            pageCount = 3,
            onPageChange = {},
        )
    }
}
