@file:OptIn(ExperimentalFoundationApi::class)

package com.analyzer.session.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.analyzer.session.domain.model.SESSION_LIST_SORT_BEST
import com.analyzer.session.domain.model.SESSION_LIST_SORT_BEST_DESC
import com.analyzer.session.domain.model.SESSION_LIST_SORT_CAR_ASC
import com.analyzer.session.domain.model.SESSION_LIST_SORT_CAR_DESC
import com.analyzer.session.domain.model.SESSION_LIST_SORT_GAME_ASC
import com.analyzer.session.domain.model.SESSION_LIST_SORT_GAME_DESC
import com.analyzer.session.domain.model.SESSION_LIST_SORT_LAPS_ASC
import com.analyzer.session.domain.model.SESSION_LIST_SORT_LAPS_DESC
import com.analyzer.session.domain.model.SESSION_LIST_SORT_NEWEST
import com.analyzer.session.domain.model.SESSION_LIST_SORT_OLDEST
import com.analyzer.session.domain.model.SESSION_LIST_SORT_TRACK_ASC
import com.analyzer.session.domain.model.SESSION_LIST_SORT_TRACK_DESC
import com.analyzer.session.presentation.model.FILTER_ALL_ID
import com.analyzer.session.presentation.model.SessionFilterKind
import com.analyzer.session.presentation.model.SessionFilterOptionUi
import com.analyzer.session.presentation.model.SessionFilterUiModel
import com.analyzer.session.presentation.model.SessionListIntent
import com.analyzer.session.presentation.model.SessionListState
import com.analyzer.session.presentation.model.SessionRowUi
import com.project.analyzer.feature.screens.session.impl.Res.*
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.components.SortablePagedTable
import com.project.analyzer.ui.components.SortableTableColumn
import com.project.analyzer.ui.components.TableCell
import com.project.analyzer.ui.components.TableColumn
import com.project.analyzer.ui.components.TableColumnAlign
import com.project.analyzer.ui.components.TableHeaderSortOrder
import com.project.analyzer.ui.components.TableRow
import com.project.analyzer.ui.components.TableSortMapping
import com.project.analyzer.ui.components.tableSortMappings
import com.project.analyzer.ui.modifier.onClick
import com.project.analyzer.ui.tooltip.Tooltip
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SessionScreenTable(
    state: SessionListState,
    onOpenDetails: (Long) -> Unit,
    onIntent: (SessionListIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showGame = state.gameFilter.options.size > 1
    val weights = sessionTableWeights(showGame)
    val dividerColor = SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.2f)

    val headerColumns = sessionTableHeaderColumns(showGame = showGame, weights = weights)
    SortablePagedTable(
        isLoading = state.isLoading,
        isEmpty = state.visibleSessions.isEmpty(),
        loadingMessage = stringResource(Res.string.session_table_loading),
        emptyMessage = stringResource(Res.string.session_table_empty),
        errorMessage = state.error,
        page = state.page,
        pageCount = state.pageCount,
        onPageChange = { onIntent(SessionListIntent.ChangePage(it)) },
        columns = headerColumns,
        activeSort = SESSION_TABLE_SORTS.activeSort(state.sortFilter.selectedId),
        onSortColumnClick = { sortColumn ->
            onIntent(
                SessionListIntent.ChangeSort(
                    SESSION_TABLE_SORTS.nextSortId(sortColumn, state.sortFilter.selectedId),
                ),
            )
        },
        modifier = modifier,
        dividerColor = dividerColor,
        rowContent = {
            state.visibleSessions.forEachIndexed { index, session ->
                SessionTableRow(
                    rowIndex = index,
                    session = session,
                    onOpenDetails = onOpenDetails,
                    onSave = { onIntent(SessionListIntent.SaveSession(it)) },
                    onDelete = { onIntent(SessionListIntent.DeleteSession(it)) },
                    weights = weights,
                    showGame = showGame,
                )
                if (index != state.visibleSessions.lastIndex) {
                    HorizontalDivider(color = dividerColor)
                }
            }
        },
    )
}

private val SESSION_TABLE_SORTS = tableSortMappings(
    TableSortMapping(
        sortKey = SessionTableSortColumn.Date,
        ascSortId = SESSION_LIST_SORT_OLDEST,
        descSortId = SESSION_LIST_SORT_NEWEST,
        defaultOrder = TableHeaderSortOrder.Desc,
    ),
    TableSortMapping(
        sortKey = SessionTableSortColumn.Game,
        ascSortId = SESSION_LIST_SORT_GAME_ASC,
        descSortId = SESSION_LIST_SORT_GAME_DESC,
    ),
    TableSortMapping(
        sortKey = SessionTableSortColumn.Track,
        ascSortId = SESSION_LIST_SORT_TRACK_ASC,
        descSortId = SESSION_LIST_SORT_TRACK_DESC,
    ),
    TableSortMapping(
        sortKey = SessionTableSortColumn.Car,
        ascSortId = SESSION_LIST_SORT_CAR_ASC,
        descSortId = SESSION_LIST_SORT_CAR_DESC,
    ),
    TableSortMapping(
        sortKey = SessionTableSortColumn.Laps,
        ascSortId = SESSION_LIST_SORT_LAPS_ASC,
        descSortId = SESSION_LIST_SORT_LAPS_DESC,
    ),
    TableSortMapping(
        sortKey = SessionTableSortColumn.BestLap,
        ascSortId = SESSION_LIST_SORT_BEST,
        descSortId = SESSION_LIST_SORT_BEST_DESC,
    ),
)

@Composable
private fun sessionTableHeaderColumns(
    showGame: Boolean,
    weights: SessionTableWeights,
): List<SortableTableColumn<SessionTableSortColumn>> = buildList {
    add(
        SortableTableColumn(
            column = TableColumn(title = stringResource(Res.string.session_table_header_date), weight = weights.date),
            sortKey = SessionTableSortColumn.Date,
        ),
    )
    if (showGame) {
        add(
            SortableTableColumn(
                column = TableColumn(
                    title = stringResource(Res.string.session_table_header_game),
                    weight = weights.game,
                ),
                sortKey = SessionTableSortColumn.Game,
            ),
        )
    }
    add(
        SortableTableColumn(
            column = TableColumn(title = stringResource(Res.string.session_table_header_track), weight = weights.track),
            sortKey = SessionTableSortColumn.Track,
        ),
    )
    add(
        SortableTableColumn(
            column = TableColumn(
                title = stringResource(Res.string.session_table_header_car_model),
                weight = weights.car,
            ),
            sortKey = SessionTableSortColumn.Car,
        ),
    )
    add(
        SortableTableColumn(
            column = TableColumn(
                title = stringResource(Res.string.session_table_header_laps),
                weight = weights.laps,
                align = TableColumnAlign.Center,
            ),
            sortKey = SessionTableSortColumn.Laps,
        ),
    )
    add(
        SortableTableColumn(
            column = TableColumn(
                title = stringResource(Res.string.session_table_header_best_lap),
                weight = weights.best,
                align = TableColumnAlign.Center,
            ),
            sortKey = SessionTableSortColumn.BestLap,
        ),
    )
    add(
        SortableTableColumn(
            column = TableColumn(
                title = stringResource(Res.string.session_table_header_actions),
                weight = weights.actions,
                align = TableColumnAlign.Center,
            ),
        ),
    )
}

private enum class SessionTableSortColumn {
    Date,
    Game,
    Track,
    Car,
    Laps,
    BestLap,
}

@Composable
@Suppress("LongParameterList")
private fun SessionTableRow(
    rowIndex: Int,
    session: SessionRowUi,
    onOpenDetails: (Long) -> Unit,
    onSave: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    weights: SessionTableWeights,
    showGame: Boolean,
) {
    TableRow(
        rowIndex = rowIndex,
        modifier = Modifier
            .height(68.dp)
            .onClick { onOpenDetails(session.sessionId) },
    ) {
        Box(
            modifier = Modifier.weight(weights.date),
            contentAlignment = Alignment.CenterStart,
        ) {
            Column {
                Text(
                    text = session.dateLabel,
                    color = SimAnalyzerTheme.material.onSurface,
                    style = SimAnalyzerTheme.typography.labelMedium,
                )
                Text(
                    text = session.timeLabel,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    style = SimAnalyzerTheme.typography.labelSmall,
                )
            }
        }

        if (showGame) {
            TableCell(
                text = session.gameLabel,
                weight = weights.game,
                align = TableColumnAlign.Start,
            )
        }
        TableCell(
            text = session.trackLabel,
            weight = weights.track,
            align = TableColumnAlign.Start,
        )
        TableCell(
            text = session.carLabel,
            weight = weights.car,
            align = TableColumnAlign.Start,
        )
        TableCell(
            text = session.lapsLabel,
            weight = weights.laps,
            align = TableColumnAlign.Center,
        )
        TableCell(
            text = session.bestLapLabel,
            weight = weights.best,
            align = TableColumnAlign.Center,
            textStyle = SimAnalyzerTheme.typography.labelMedium,
        )

        Row(
            modifier = Modifier.weight(weights.actions),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (session.isSaved) {
                SessionActionButton(
                    icon = Icons.Filled.Bookmark,
                    tint = SimAnalyzerTheme.extended.lightGreen,
                    tooltip = stringResource(Res.string.session_action_saved),
                )
            } else {
                SessionActionButton(
                    icon = Icons.Outlined.BookmarkBorder,
                    tint = SimAnalyzerTheme.material.primary,
                    tooltip = stringResource(Res.string.session_action_save),
                    onClick = { onSave(session.sessionId) },
                )
            }
            SessionActionButton(
                icon = Icons.Outlined.Delete,
                tint = SimAnalyzerTheme.material.error,
                tooltip = stringResource(Res.string.session_action_delete),
                onClick = { onDelete(session.sessionId) },
            )
            SessionActionButton(
                icon = Icons.Filled.ChevronRight,
                tint = SimAnalyzerTheme.material.primary,
                tooltip = stringResource(Res.string.session_action_open_details),
                onClick = { onOpenDetails(session.sessionId) },
            )
        }
    }
}

@Composable
private fun SessionActionButton(icon: ImageVector, tint: Color, tooltip: String, onClick: (() -> Unit)? = null) {
    Tooltip(tooltip = tooltip) {
        val clickable = onClick != null
        val background = tint.copy(alpha = if (clickable) 0.18f else 0.1f)
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(30.dp)
                .clip(SimAnalyzerTheme.shapes.small)
                .background(background)
                .border(
                    width = 1.dp,
                    color = tint.copy(alpha = if (clickable) 0.45f else 0.2f),
                    shape = SimAnalyzerTheme.shapes.small,
                )
                .then(if (clickable) Modifier.onClick { onClick.invoke() } else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private data class SessionTableWeights(
    val date: Float,
    val game: Float,
    val track: Float,
    val car: Float,
    val laps: Float,
    val best: Float,
    val actions: Float,
)

private fun sessionTableWeights(showGame: Boolean): SessionTableWeights = if (showGame) {
    SessionTableWeights(
        date = 0.17f,
        game = 0.11f,
        track = 0.18f,
        car = 0.20f,
        laps = 0.08f,
        best = 0.11f,
        actions = 0.15f,
    )
} else {
    SessionTableWeights(
        date = 0.19f,
        game = 0f,
        track = 0.22f,
        car = 0.22f,
        laps = 0.08f,
        best = 0.11f,
        actions = 0.18f,
    )
}

@Preview
@Composable
private fun SessionScreenEmptyPreview() {
    SimAnalyzerTheme {
        SessionScreenTable(
            state = SessionListState(
                isLoading = false,
                gameFilter = SessionFilterUiModel(
                    kind = SessionFilterKind.Game,
                    selectedId = FILTER_ALL_ID,
                    options = listOf(
                        SessionFilterOptionUi(FILTER_ALL_ID),
                        SessionFilterOptionUi("acc", "ACC"),
                    ),
                ),
                page = 1,
                pageCount = 9,
                sessions = emptyList(),
                visibleSessions = emptyList(),
            ),
            onOpenDetails = {},
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun SessionScreenTablePreview() {
    val sessions = listOf(
        SessionRowUi(
            sessionId = 1L,
            dateLabel = "Oct 24, 2025",
            timeLabel = "20:40",
            gameLabel = "ACC",
            sessionTypeLabel = "Qualifying",
            trackLabel = "Location",
            carLabel = "Car Name",
            lapsLabel = "0",
            bestLapLabel = "0:00.000",
            isSaved = true,
        ),
        SessionRowUi(
            sessionId = 2L,
            dateLabel = "Oct 24, 2025",
            timeLabel = "20:40",
            gameLabel = "ACC",
            sessionTypeLabel = "Race",
            trackLabel = "Location",
            carLabel = "Car Name",
            lapsLabel = "0",
            bestLapLabel = "0:00.000",
            isSaved = false,
        ),
    )

    SimAnalyzerTheme {
        SessionScreenTable(
            state = SessionListState(
                isLoading = false,
                gameFilter = SessionFilterUiModel(
                    kind = SessionFilterKind.Game,
                    selectedId = FILTER_ALL_ID,
                    options = listOf(
                        SessionFilterOptionUi(FILTER_ALL_ID),
                        SessionFilterOptionUi("acc", "ACC"),
                    ),
                ),
                page = 1,
                pageCount = 9,
                sessions = sessions,
                visibleSessions = sessions,
            ),
            onOpenDetails = {},
            onIntent = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
