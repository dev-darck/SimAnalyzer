package com.project.analyzer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.analyzer.core.ui.Res.Res
import com.project.analyzer.core.ui.Res.paged_table_preview_best_lap
import com.project.analyzer.core.ui.Res.paged_table_preview_date
import com.project.analyzer.core.ui.Res.paged_table_preview_loading_sessions
import com.project.analyzer.core.ui.Res.paged_table_preview_monza
import com.project.analyzer.core.ui.Res.paged_table_preview_no_sessions
import com.project.analyzer.core.ui.Res.paged_table_preview_spa
import com.project.analyzer.core.ui.Res.paged_table_preview_suzuka
import com.project.analyzer.core.ui.Res.paged_table_preview_track
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.onClick
import org.jetbrains.compose.resources.stringResource

public data class TableColumn(
    val title: String,
    val weight: Float,
    val align: TableColumnAlign = TableColumnAlign.Start,
)

public enum class TableColumnAlign {
    Start,
    Center,
    End,
}

public enum class TableHeaderSortOrder {
    Asc,
    Desc,
}

public data class SortableTableColumn<SortKey>(val column: TableColumn, val sortKey: SortKey? = null)

public data class TableHeaderActiveSort<SortKey>(val sortKey: SortKey, val order: TableHeaderSortOrder)

public data class TableHeaderCellUi(
    val label: String,
    val weight: Float,
    val align: TableColumnAlign = TableColumnAlign.Start,
    val isSortable: Boolean = false,
    val sortOrder: TableHeaderSortOrder? = null,
)

public data class TableSortMapping<SortKey>(
    val sortKey: SortKey,
    val ascSortId: String,
    val descSortId: String,
    val defaultOrder: TableHeaderSortOrder = TableHeaderSortOrder.Asc,
)

public class TableSortMappings<SortKey>(mappings: List<TableSortMapping<SortKey>>) {

    private val mappingsByKey: Map<SortKey, TableSortMapping<SortKey>>
    private val activeSortById: Map<String, TableHeaderActiveSort<SortKey>>

    init {
        require(mappings.map(TableSortMapping<SortKey>::sortKey).distinct().size == mappings.size) {
            "Duplicate sortKey in TableSortMappings"
        }
        val sortIds = mappings.flatMap { listOf(it.ascSortId, it.descSortId) }
        require(sortIds.distinct().size == sortIds.size) {
            "Duplicate sortId in TableSortMappings"
        }

        mappingsByKey = mappings.associateBy(TableSortMapping<SortKey>::sortKey)
        activeSortById = buildMap {
            mappings.forEach { mapping ->
                put(
                    mapping.ascSortId,
                    TableHeaderActiveSort(
                        sortKey = mapping.sortKey,
                        order = TableHeaderSortOrder.Asc,
                    ),
                )
                put(
                    mapping.descSortId,
                    TableHeaderActiveSort(
                        sortKey = mapping.sortKey,
                        order = TableHeaderSortOrder.Desc,
                    ),
                )
            }
        }
    }

    public fun activeSort(sortId: String): TableHeaderActiveSort<SortKey>? = activeSortById[sortId]

    public fun nextSortId(sortKey: SortKey, currentSortId: String): String {
        val mapping = mappingsByKey[sortKey]
            ?: error("Unknown sort key: $sortKey")
        val active = activeSortById[currentSortId]
        return when {
            active?.sortKey != sortKey -> {
                if (mapping.defaultOrder == TableHeaderSortOrder.Asc) mapping.ascSortId else mapping.descSortId
            }

            active.order == TableHeaderSortOrder.Asc -> mapping.descSortId

            else -> mapping.ascSortId
        }
    }
}

public fun <SortKey> tableSortMappings(vararg mappings: TableSortMapping<SortKey>): TableSortMappings<SortKey> =
    TableSortMappings(mappings.toList())

public fun buildTableHeaderCells(
    columns: List<TableColumn>,
    sortableColumnIndices: Set<Int> = emptySet(),
    sortOrderByColumnIndex: Map<Int, TableHeaderSortOrder> = emptyMap(),
): List<TableHeaderCellUi> = columns.mapIndexed { index, column ->
    TableHeaderCellUi(
        label = column.title.uppercase(),
        weight = column.weight,
        align = column.align,
        isSortable = index in sortableColumnIndices,
        sortOrder = sortOrderByColumnIndex[index],
    )
}

@Composable
@Suppress("LongParameterList")
public fun PagedTable(
    isLoading: Boolean,
    isEmpty: Boolean,
    loadingMessage: String,
    emptyMessage: String,
    errorMessage: String?,
    page: Int,
    pageCount: Int,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showPagination: Boolean = true,
    dividerColor: Color = SimAnalyzerTheme.chrome.dividerSubtle,
    paginationBackgroundColor: Color = SimAnalyzerTheme.material.secondaryContainer,
    headerContent: @Composable ColumnScope.() -> Unit = {},
    rowContent: @Composable ColumnScope.() -> Unit = {},
) {
    val shape = SimAnalyzerTheme.shapes.large
    val borderColor = SimAnalyzerTheme.chrome.borderSubtle

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SimAnalyzerTheme.material.surface)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape,
            ),
    ) {
        headerContent()
        HorizontalDivider(color = dividerColor)

        when {
            isLoading -> EmptyStateMessage(loadingMessage)
            isEmpty -> EmptyStateMessage(errorMessage ?: emptyMessage)
            else -> Column(content = rowContent)
        }

        if (showPagination && pageCount > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(paginationBackgroundColor)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Pagination(
                    page = page,
                    pageCount = pageCount,
                    onPageChange = onPageChange,
                )
            }
        }
    }
}

@Composable
public fun TableHeader(
    columns: List<TableColumn>,
    modifier: Modifier = Modifier,
    backgroundColor: Color = SimAnalyzerTheme.material.secondaryContainer,
    height: Dp = 48.dp,
    contentPadding: Dp = 16.dp,
    sortableColumnIndices: Set<Int> = emptySet(),
    sortOrderByColumnIndex: Map<Int, TableHeaderSortOrder> = emptyMap(),
    onColumnClick: ((Int) -> Unit)? = null,
) {
    val headerCells = remember(columns, sortableColumnIndices, sortOrderByColumnIndex) {
        buildTableHeaderCells(
            columns = columns,
            sortableColumnIndices = sortableColumnIndices,
            sortOrderByColumnIndex = sortOrderByColumnIndex,
        )
    }

    TableHeader(
        cells = headerCells,
        modifier = modifier,
        backgroundColor = backgroundColor,
        height = height,
        contentPadding = contentPadding,
        onColumnClick = onColumnClick,
    )
}

@Composable
public fun TableHeader(
    cells: List<TableHeaderCellUi>,
    modifier: Modifier = Modifier,
    backgroundColor: Color = SimAnalyzerTheme.material.secondaryContainer,
    height: Dp = 48.dp,
    contentPadding: Dp = 16.dp,
    onColumnClick: ((Int) -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .height(height)
            .padding(horizontal = contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cells.forEachIndexed { index, cell ->
            TableHeaderCell(
                text = cell.label,
                weight = cell.weight,
                align = cell.align,
                isSortable = cell.isSortable,
                sortOrder = cell.sortOrder,
                onClick = if (onColumnClick != null && cell.isSortable) {
                    { onColumnClick(index) }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
public fun RowScope.TableHeaderCell(
    text: String,
    weight: Float,
    align: TableColumnAlign,
    isSortable: Boolean = false,
    sortOrder: TableHeaderSortOrder? = null,
    onClick: (() -> Unit)? = null,
) {
    val contentAlignment = when (align) {
        TableColumnAlign.Start -> Alignment.CenterStart
        TableColumnAlign.Center -> Alignment.Center
        TableColumnAlign.End -> Alignment.CenterEnd
    }
    val textAlign = when (align) {
        TableColumnAlign.Start -> TextAlign.Start
        TableColumnAlign.Center -> TextAlign.Center
        TableColumnAlign.End -> TextAlign.End
    }

    Box(
        modifier = Modifier
            .weight(weight)
            .then(
                if (onClick != null) {
                    Modifier.onClick(onClick = onClick)
                } else {
                    Modifier
                },
            ),
        contentAlignment = contentAlignment,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (isSortable) 4.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                color = if (sortOrder != null) {
                    SimAnalyzerTheme.material.primary
                } else {
                    SimAnalyzerTheme.extended.onPrimaryContainer50
                },
                style = SimAnalyzerTheme.typography.labelSmall,
                textAlign = textAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isSortable) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = when (sortOrder) {
                        TableHeaderSortOrder.Asc -> Icons.Filled.ArrowUpward
                        TableHeaderSortOrder.Desc -> Icons.Filled.ArrowDownward
                        null -> Icons.Filled.UnfoldMore
                    },
                    contentDescription = null,
                    tint = if (sortOrder != null) {
                        SimAnalyzerTheme.material.primary
                    } else {
                        SimAnalyzerTheme.extended.onPrimaryContainer50.copy(alpha = 0.65f)
                    },
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

@Composable
@Suppress("LongParameterList")
public fun <SortKey> SortablePagedTable(
    isLoading: Boolean,
    isEmpty: Boolean,
    loadingMessage: String,
    emptyMessage: String,
    errorMessage: String?,
    page: Int,
    pageCount: Int,
    onPageChange: (Int) -> Unit,
    columns: List<SortableTableColumn<SortKey>>,
    activeSort: TableHeaderActiveSort<SortKey>?,
    onSortColumnClick: (SortKey) -> Unit,
    modifier: Modifier = Modifier,
    showPagination: Boolean = true,
    dividerColor: Color = SimAnalyzerTheme.chrome.dividerSubtle,
    paginationBackgroundColor: Color = SimAnalyzerTheme.material.secondaryContainer,
    headerBackgroundColor: Color = SimAnalyzerTheme.material.secondaryContainer,
    headerHeight: Dp = 48.dp,
    headerContentPadding: Dp = 16.dp,
    rowContent: @Composable ColumnScope.() -> Unit = {},
) {
    val headerPresentation = remember(columns, activeSort) {
        buildSortableTableHeaderPresentation(
            columns = columns,
            activeSort = activeSort,
        )
    }

    PagedTable(
        isLoading = isLoading,
        isEmpty = isEmpty,
        loadingMessage = loadingMessage,
        emptyMessage = emptyMessage,
        errorMessage = errorMessage,
        page = page,
        pageCount = pageCount,
        onPageChange = onPageChange,
        modifier = modifier,
        showPagination = showPagination,
        dividerColor = dividerColor,
        paginationBackgroundColor = paginationBackgroundColor,
        headerContent = {
            TableHeader(
                cells = headerPresentation.headerCells,
                backgroundColor = headerBackgroundColor,
                height = headerHeight,
                contentPadding = headerContentPadding,
                onColumnClick = { columnIndex ->
                    headerPresentation.sortKeyByColumnIndex[columnIndex]?.let(onSortColumnClick)
                },
            )
        },
        rowContent = rowContent,
    )
}

@Composable
public fun TableRow(
    rowIndex: Int,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val baseColor = SimAnalyzerTheme.chrome.tableRowEven
    val rowColor = backgroundColor ?: if (rowIndex % 2 == 0) {
        baseColor
    } else {
        SimAnalyzerTheme.chrome.tableRowOdd
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(rowColor)
            .padding(horizontal = contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
public fun RowScope.TableCell(
    text: String,
    weight: Float,
    align: TableColumnAlign = TableColumnAlign.Start,
    color: Color = SimAnalyzerTheme.material.onSurface,
    textStyle: TextStyle = SimAnalyzerTheme.typography.labelMedium,
) {
    val contentAlignment = when (align) {
        TableColumnAlign.Start -> Alignment.CenterStart
        TableColumnAlign.Center -> Alignment.Center
        TableColumnAlign.End -> Alignment.CenterEnd
    }
    val textAlign = when (align) {
        TableColumnAlign.Start -> TextAlign.Start
        TableColumnAlign.Center -> TextAlign.Center
        TableColumnAlign.End -> TextAlign.End
    }

    Box(
        modifier = Modifier.weight(weight),
        contentAlignment = contentAlignment,
    ) {
        Text(
            text = text,
            style = textStyle,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign,
        )
    }
}

@Composable
private fun EmptyStateMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.labelLarge,
        )
    }
}

private data class SortableTableHeaderPresentation<SortKey>(
    val headerCells: List<TableHeaderCellUi>,
    val sortKeyByColumnIndex: Map<Int, SortKey>,
)

private fun <SortKey> buildSortableTableHeaderPresentation(
    columns: List<SortableTableColumn<SortKey>>,
    activeSort: TableHeaderActiveSort<SortKey>?,
): SortableTableHeaderPresentation<SortKey> {
    val sortKeyByColumnIndex = columns.mapIndexedNotNull { index, column ->
        column.sortKey?.let { index to it }
    }.toMap()

    val sortOrderByColumnIndex = activeSort
        ?.let { sort ->
            val activeIndex = columns.indexOfFirst { it.sortKey == sort.sortKey }
            if (activeIndex >= 0) mapOf(activeIndex to sort.order) else emptyMap()
        }
        ?: emptyMap()

    return SortableTableHeaderPresentation(
        headerCells = buildTableHeaderCells(
            columns = columns.map(SortableTableColumn<SortKey>::column),
            sortableColumnIndices = sortKeyByColumnIndex.keys,
            sortOrderByColumnIndex = sortOrderByColumnIndex,
        ),
        sortKeyByColumnIndex = sortKeyByColumnIndex,
    )
}

private enum class PagedTablePreviewSort {
    Date,
    Track,
    BestLap,
}

@Preview(name = "Sortable Table")
@Composable
private fun SortablePagedTablePreview() {
    val rows = listOf(
        Triple("Oct 24, 2025", stringResource(Res.string.paged_table_preview_spa), "1:42.381"),
        Triple("Oct 23, 2025", stringResource(Res.string.paged_table_preview_monza), "1:47.812"),
        Triple("Oct 22, 2025", stringResource(Res.string.paged_table_preview_suzuka), "2:00.193"),
    )
    val columns = listOf(
        SortableTableColumn(
            column = TableColumn(title = stringResource(Res.string.paged_table_preview_date), weight = 0.34f),
            sortKey = PagedTablePreviewSort.Date,
        ),
        SortableTableColumn(
            column = TableColumn(title = stringResource(Res.string.paged_table_preview_track), weight = 0.40f),
            sortKey = PagedTablePreviewSort.Track,
        ),
        SortableTableColumn(
            column = TableColumn(
                title = stringResource(Res.string.paged_table_preview_best_lap),
                weight = 0.26f,
                align = TableColumnAlign.End,
            ),
            sortKey = PagedTablePreviewSort.BestLap,
        ),
    )

    SimAnalyzerTheme {
        SortablePagedTable(
            isLoading = false,
            isEmpty = false,
            loadingMessage = stringResource(Res.string.paged_table_preview_loading_sessions),
            emptyMessage = stringResource(Res.string.paged_table_preview_no_sessions),
            errorMessage = null,
            page = 2,
            pageCount = 5,
            onPageChange = {},
            columns = columns,
            activeSort = TableHeaderActiveSort(
                sortKey = PagedTablePreviewSort.BestLap,
                order = TableHeaderSortOrder.Asc,
            ),
            onSortColumnClick = {},
            rowContent = {
                rows.forEachIndexed { index, row ->
                    TableRow(
                        rowIndex = index,
                        modifier = Modifier.height(44.dp),
                    ) {
                        TableCell(
                            text = row.first,
                            weight = 0.34f,
                        )
                        TableCell(
                            text = row.second,
                            weight = 0.40f,
                        )
                        TableCell(
                            text = row.third,
                            weight = 0.26f,
                            align = TableColumnAlign.End,
                        )
                    }
                    if (index != rows.lastIndex) {
                        HorizontalDivider(
                            color = SimAnalyzerTheme.chrome.dividerSubtle,
                        )
                    }
                }
            },
        )
    }
}

@Preview(name = "Table Empty")
@Composable
private fun PagedTableEmptyPreview() {
    val columns = listOf(
        TableColumn(title = stringResource(Res.string.paged_table_preview_date), weight = 0.34f),
        TableColumn(title = stringResource(Res.string.paged_table_preview_track), weight = 0.40f),
        TableColumn(
            title = stringResource(Res.string.paged_table_preview_best_lap),
            weight = 0.26f,
            align = TableColumnAlign.End,
        ),
    )

    SimAnalyzerTheme {
        PagedTable(
            isLoading = false,
            isEmpty = true,
            loadingMessage = stringResource(Res.string.paged_table_preview_loading_sessions),
            emptyMessage = stringResource(Res.string.paged_table_preview_no_sessions),
            errorMessage = null,
            page = 1,
            pageCount = 1,
            onPageChange = {},
            headerContent = {
                TableHeader(columns = columns)
            },
        )
    }
}
