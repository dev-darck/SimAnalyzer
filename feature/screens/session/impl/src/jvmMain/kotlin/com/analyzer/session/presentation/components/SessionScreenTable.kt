@file:OptIn(ExperimentalFoundationApi::class)

package com.analyzer.session.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.project.analyzer.feature.screens.session.impl.Res.Res
import com.project.analyzer.feature.screens.session.impl.Res.session_action_delete
import com.project.analyzer.feature.screens.session.impl.Res.session_action_open_details
import com.project.analyzer.feature.screens.session.impl.Res.session_action_save
import com.project.analyzer.feature.screens.session.impl.Res.session_action_saved
import com.project.analyzer.feature.screens.session.impl.Res.session_table_empty
import com.project.analyzer.feature.screens.session.impl.Res.session_table_header_best_lap
import com.project.analyzer.feature.screens.session.impl.Res.session_table_header_car_model
import com.project.analyzer.feature.screens.session.impl.Res.session_table_header_date
import com.project.analyzer.feature.screens.session.impl.Res.session_table_header_game
import com.project.analyzer.feature.screens.session.impl.Res.session_table_header_laps
import com.project.analyzer.feature.screens.session.impl.Res.session_table_header_map
import com.project.analyzer.feature.screens.session.impl.Res.session_table_header_track
import com.project.analyzer.feature.screens.session.impl.Res.session_table_loading
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.components.SortablePagedTable
import com.project.analyzer.ui.components.SortableTableColumn
import com.project.analyzer.ui.components.TableColumn
import com.project.analyzer.ui.components.TableColumnAlign
import com.project.analyzer.ui.components.TableHeaderSortOrder
import com.project.analyzer.ui.components.TableRow
import com.project.analyzer.ui.components.TableSortMapping
import com.project.analyzer.ui.components.TrackMap
import com.project.analyzer.ui.components.tableSortMappings
import com.project.analyzer.ui.modifier.onClick
import com.project.analyzer.ui.tooltip.Tooltip
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SessionScreenTable(
    state: SessionListState,
    onOpenDetails: (Long) -> Unit,
    onIntent: (SessionListIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val weights = remember { sessionTableWeights() }
    val dividerColor = SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.2f)
    val dateHeader = stringResource(Res.string.session_table_header_date)
    val gameHeader = stringResource(Res.string.session_table_header_game)
    val trackHeader = stringResource(Res.string.session_table_header_track)
    val mapHeader = stringResource(Res.string.session_table_header_map)
    val carHeader = stringResource(Res.string.session_table_header_car_model)
    val lapsHeader = stringResource(Res.string.session_table_header_laps)
    val bestLapHeader = stringResource(Res.string.session_table_header_best_lap)
    val headerColumns = remember(
        weights,
        dateHeader,
        gameHeader,
        trackHeader,
        mapHeader,
        carHeader,
        lapsHeader,
        bestLapHeader,
    ) {
        sessionTableHeaderColumns(
            showGame = true,
            weights = weights,
            dateHeader = dateHeader,
            gameHeader = gameHeader,
            trackHeader = trackHeader,
            mapHeader = mapHeader,
            carHeader = carHeader,
            lapsHeader = lapsHeader,
            bestLapHeader = bestLapHeader,
        )
    }
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
                    showGame = true,
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

private fun sessionTableHeaderColumns(
    showGame: Boolean,
    weights: SessionTableWeights,
    dateHeader: String,
    gameHeader: String,
    trackHeader: String,
    mapHeader: String,
    carHeader: String,
    lapsHeader: String,
    bestLapHeader: String,
): ImmutableList<SortableTableColumn<SessionTableSortColumn>> = buildList {
    add(
        SortableTableColumn(
            column = TableColumn(
                title = dateHeader,
                weight = weights.date,
                align = TableColumnAlign.Start,
            ),
            sortKey = SessionTableSortColumn.Date,
        ),
    )
    if (showGame) {
        add(
            SortableTableColumn(
                column = TableColumn(
                    title = gameHeader,
                    weight = weights.game,
                    align = TableColumnAlign.Center,
                ),
                sortKey = SessionTableSortColumn.Game,
            ),
        )
    }
    add(
        SortableTableColumn(
            column = TableColumn(
                title = trackHeader,
                weight = weights.track,
                align = TableColumnAlign.Center,
            ),
            sortKey = SessionTableSortColumn.Track,
        ),
    )
    add(
        SortableTableColumn(
            column = TableColumn(
                title = mapHeader,
                weight = weights.map,
                align = TableColumnAlign.Center,
            ),
        ),
    )
    add(
        SortableTableColumn(
            column = TableColumn(
                title = carHeader,
                weight = weights.car,
                align = TableColumnAlign.Center,
            ),
            sortKey = SessionTableSortColumn.Car,
        ),
    )
    add(
        SortableTableColumn(
            column = TableColumn(
                title = lapsHeader,
                weight = weights.laps,
                align = TableColumnAlign.Center,
            ),
            sortKey = SessionTableSortColumn.Laps,
        ),
    )
    add(
        SortableTableColumn(
            column = TableColumn(
                title = bestLapHeader,
                weight = weights.best,
                align = TableColumnAlign.Center,
            ),
            sortKey = SessionTableSortColumn.BestLap,
        ),
    )
    add(
        SortableTableColumn(
            column = TableColumn(
                title = "",
                weight = weights.actions,
                align = TableColumnAlign.Center,
            ),
        ),
    )
}.toImmutableList()

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
            CenteredSessionCell(
                text = session.gameLabel,
                weight = weights.game,
            )
        }
        CenteredSessionCell(
            text = session.trackLabel,
            weight = weights.track,
        )
        Box(
            modifier = Modifier.weight(weights.map),
            contentAlignment = Alignment.Center,
        ) {
            TrackMap(
                trackMap = session.trackMap,
                scale = 1.12f,
                lineColor = SimAnalyzerTheme.material.primary,
                padding = 2.dp,
                modifier = Modifier
                    .width(66.dp)
                    .height(32.dp),
            )
        }
        CenteredSessionCell(
            text = session.carLabel,
            weight = weights.car,
        )
        CenteredSessionCell(
            text = session.lapsLabel,
            weight = weights.laps,
        )
        CenteredSessionCell(
            text = session.bestLapLabel,
            weight = weights.best,
            textStyle = SimAnalyzerTheme.typography.labelMedium,
        )

        Row(
            modifier = Modifier
                .weight(weights.actions)
                .fillMaxWidth()
                .padding(end = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (session.isSaved) {
                SessionActionButton(
                    icon = Icons.Filled.Bookmark,
                    tint = SimAnalyzerTheme.extended.teal,
                    tooltip = stringResource(Res.string.session_action_saved),
                    onClickEnabled = false,
                )
            } else {
                SessionActionButton(
                    icon = Icons.Outlined.BookmarkBorder,
                    tint = SimAnalyzerTheme.extended.teal,
                    tooltip = stringResource(Res.string.session_action_save),
                    onClick = { onSave(session.sessionId) },
                )
            }
            SessionActionButton(
                icon = Icons.Outlined.Delete,
                tint = SimAnalyzerTheme.extended.red,
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
private fun RowScope.CenteredSessionCell(
    text: String,
    weight: Float,
    textStyle: TextStyle = SimAnalyzerTheme.typography.labelMedium,
    color: Color = SimAnalyzerTheme.material.onSurface,
) {
    Box(
        modifier = Modifier.weight(weight),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = textStyle,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SessionActionButton(
    icon: ImageVector,
    tint: Color,
    tooltip: String,
    onClickEnabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    Tooltip(tooltip = tooltip) {
        val clickable = onClickEnabled
        Box(
            modifier = Modifier
                .requiredSize(30.dp)
                .then(if (clickable) Modifier.onClick(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.requiredSize(18.dp),
            )
        }
    }
}

private data class SessionTableWeights(
    val date: Float,
    val game: Float,
    val track: Float,
    val map: Float,
    val car: Float,
    val laps: Float,
    val best: Float,
    val actions: Float,
)

private fun sessionTableWeights(): SessionTableWeights = SessionTableWeights(
    date = .15f,
    game = .08f,
    track = .15f,
    map = .11f,
    car = .15f,
    laps = .08f,
    best = .10f,
    actions = .18f,
)

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
                    options = persistentListOf(
                        SessionFilterOptionUi(FILTER_ALL_ID),
                        SessionFilterOptionUi("acc", "ACC"),
                    ),
                ),
                page = 1,
                pageCount = 9,
                visibleSessions = persistentListOf(),
            ),
            onOpenDetails = {},
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun SessionScreenTablePreview() {
    val sessions = persistentListOf(
        SessionRowUi(
            sessionId = 1L,
            dateLabel = "Oct 24, 2025",
            timeLabel = "20:40",
            gameId = "acc",
            trackId = "spa",
            gameLabel = "ACC",
            sessionTypeLabel = "Qualifying",
            trackLabel = "Location",
            carLabel = "Car Name",
            lapsLabel = "0",
            bestLapLabel = "0:00.000",
            isSaved = true,
            trackMap = previewTrackMapData(),
        ),
        SessionRowUi(
            sessionId = 2L,
            dateLabel = "Oct 24, 2025",
            timeLabel = "20:40",
            gameId = "acc",
            trackId = "spa",
            gameLabel = "ACC",
            sessionTypeLabel = "Race",
            trackLabel = "Location",
            carLabel = "Car Name",
            lapsLabel = "0",
            bestLapLabel = "0:00.000",
            isSaved = false,
            trackMap = previewTrackMapData(),
        ),
    )

    SimAnalyzerTheme {
        SessionScreenTable(
            state = SessionListState(
                isLoading = false,
                gameFilter = SessionFilterUiModel(
                    kind = SessionFilterKind.Game,
                    selectedId = FILTER_ALL_ID,
                    options = persistentListOf(
                        SessionFilterOptionUi(FILTER_ALL_ID),
                        SessionFilterOptionUi("acc", "ACC"),
                    ),
                ),
                page = 1,
                pageCount = 9,
                visibleSessions = sessions,
            ),
            onOpenDetails = {},
            onIntent = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
