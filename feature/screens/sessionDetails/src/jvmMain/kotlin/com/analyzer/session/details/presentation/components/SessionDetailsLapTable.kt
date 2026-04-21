package com.analyzer.session.details.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_BEST
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_DELTA
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_DELTA_DESC
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_INCIDENTS
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_INCIDENTS_DESC
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_LAP
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_LAP_DESC
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_S1
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_S1_DESC
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_S2
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_S2_DESC
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_S3
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_S3_DESC
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_STATUS
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_STATUS_DESC
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_TOTAL_DESC
import com.analyzer.session.details.presentation.model.LapStatus
import com.analyzer.session.details.presentation.model.SessionDetailFilterKind
import com.analyzer.session.details.presentation.model.SessionDetailFilterOptionUi
import com.analyzer.session.details.presentation.model.SessionDetailFilterUiModel
import com.analyzer.session.details.presentation.model.SessionDetailHeaderUi
import com.analyzer.session.details.presentation.model.SessionDetailState
import com.analyzer.session.details.presentation.model.SessionDetailStatsUi
import com.analyzer.session.details.presentation.model.SessionLapRowUi
import com.project.analyzer.feature.screens.sessionDetails.Res.Res
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_table_delta
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_table_empty
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_table_incidents
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_table_lap
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_table_loading
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_table_s1
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_table_s2
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_table_s3
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_table_status
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_table_total_time
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.components.SortablePagedTable
import com.project.analyzer.ui.components.SortableTableColumn
import com.project.analyzer.ui.components.TableCell
import com.project.analyzer.ui.components.TableColumn
import com.project.analyzer.ui.components.TableColumnAlign
import com.project.analyzer.ui.components.TableRow
import com.project.analyzer.ui.components.TableSortMapping
import com.project.analyzer.ui.components.tableSortMappings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SessionDetailsLapTable(
    state: SessionDetailState,
    modifier: Modifier = Modifier,
    onPageChange: (Int) -> Unit = {},
    onSortChange: (String) -> Unit = {},
) {
    val dividerColor = SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.2f)
    val lapHeader = stringResource(Res.string.session_details_table_lap)
    val totalTimeHeader = stringResource(Res.string.session_details_table_total_time)
    val s1Header = stringResource(Res.string.session_details_table_s1)
    val s2Header = stringResource(Res.string.session_details_table_s2)
    val s3Header = stringResource(Res.string.session_details_table_s3)
    val incidentsHeader = stringResource(Res.string.session_details_table_incidents)
    val deltaHeader = stringResource(Res.string.session_details_table_delta)
    val statusHeader = stringResource(Res.string.session_details_table_status)
    val headerColumns = remember(
        lapHeader,
        totalTimeHeader,
        s1Header,
        s2Header,
        s3Header,
        incidentsHeader,
        deltaHeader,
        statusHeader,
    ) {
        sessionDetailsHeaderColumns(
            lapHeader = lapHeader,
            totalTimeHeader = totalTimeHeader,
            s1Header = s1Header,
            s2Header = s2Header,
            s3Header = s3Header,
            incidentsHeader = incidentsHeader,
            deltaHeader = deltaHeader,
            statusHeader = statusHeader,
        )
    }

    SortablePagedTable(
        isLoading = state.isLoading,
        isEmpty = state.visibleLaps.isEmpty(),
        loadingMessage = stringResource(Res.string.session_details_table_loading),
        emptyMessage = stringResource(Res.string.session_details_table_empty),
        errorMessage = state.error,
        page = state.page,
        pageCount = state.pageCount,
        onPageChange = onPageChange,
        columns = headerColumns,
        activeSort = SESSION_DETAILS_TABLE_SORTS.activeSort(state.sortFilter.selectedId),
        onSortColumnClick = { sortColumn ->
            onSortChange(SESSION_DETAILS_TABLE_SORTS.nextSortId(sortColumn, state.sortFilter.selectedId))
        },
        modifier = modifier,
        dividerColor = dividerColor,
        rowContent = {
            state.visibleLaps.forEachIndexed { index, lap ->
                SessionDetailsTableRow(
                    rowIndex = index,
                    lap = lap,
                )
                if (index != state.visibleLaps.lastIndex) {
                    HorizontalDivider(color = dividerColor)
                }
            }
        },
    )
}

private val SESSION_DETAILS_TABLE_SORTS = tableSortMappings(
    TableSortMapping(
        sortKey = SessionDetailsSortColumn.Lap,
        ascSortId = SESSION_DETAIL_SORT_LAP,
        descSortId = SESSION_DETAIL_SORT_LAP_DESC,
    ),
    TableSortMapping(
        sortKey = SessionDetailsSortColumn.TotalTime,
        ascSortId = SESSION_DETAIL_SORT_BEST,
        descSortId = SESSION_DETAIL_SORT_TOTAL_DESC,
    ),
    TableSortMapping(
        sortKey = SessionDetailsSortColumn.S1,
        ascSortId = SESSION_DETAIL_SORT_S1,
        descSortId = SESSION_DETAIL_SORT_S1_DESC,
    ),
    TableSortMapping(
        sortKey = SessionDetailsSortColumn.S2,
        ascSortId = SESSION_DETAIL_SORT_S2,
        descSortId = SESSION_DETAIL_SORT_S2_DESC,
    ),
    TableSortMapping(
        sortKey = SessionDetailsSortColumn.S3,
        ascSortId = SESSION_DETAIL_SORT_S3,
        descSortId = SESSION_DETAIL_SORT_S3_DESC,
    ),
    TableSortMapping(
        sortKey = SessionDetailsSortColumn.Incidents,
        ascSortId = SESSION_DETAIL_SORT_INCIDENTS,
        descSortId = SESSION_DETAIL_SORT_INCIDENTS_DESC,
    ),
    TableSortMapping(
        sortKey = SessionDetailsSortColumn.Delta,
        ascSortId = SESSION_DETAIL_SORT_DELTA,
        descSortId = SESSION_DETAIL_SORT_DELTA_DESC,
    ),
    TableSortMapping(
        sortKey = SessionDetailsSortColumn.Status,
        ascSortId = SESSION_DETAIL_SORT_STATUS,
        descSortId = SESSION_DETAIL_SORT_STATUS_DESC,
    ),
)

private enum class SessionDetailsSortColumn {
    Lap,
    TotalTime,
    S1,
    S2,
    S3,
    Incidents,
    Delta,
    Status,
}

private fun sessionDetailsHeaderColumns(
    lapHeader: String,
    totalTimeHeader: String,
    s1Header: String,
    s2Header: String,
    s3Header: String,
    incidentsHeader: String,
    deltaHeader: String,
    statusHeader: String,
): ImmutableList<SortableTableColumn<SessionDetailsSortColumn>> = persistentListOf(
    SortableTableColumn(
        column = TableColumn(
            title = lapHeader,
            weight = 0.08f,
            align = TableColumnAlign.Center,
        ),
        sortKey = SessionDetailsSortColumn.Lap,
    ),
    SortableTableColumn(
        column = TableColumn(
            title = totalTimeHeader,
            weight = 0.18f,
            align = TableColumnAlign.Center,
        ),
        sortKey = SessionDetailsSortColumn.TotalTime,
    ),
    SortableTableColumn(
        column = TableColumn(
            title = s1Header,
            weight = 0.1f,
            align = TableColumnAlign.Center,
        ),
        sortKey = SessionDetailsSortColumn.S1,
    ),
    SortableTableColumn(
        column = TableColumn(
            title = s2Header,
            weight = 0.1f,
            align = TableColumnAlign.Center,
        ),
        sortKey = SessionDetailsSortColumn.S2,
    ),
    SortableTableColumn(
        column = TableColumn(
            title = s3Header,
            weight = 0.1f,
            align = TableColumnAlign.Center,
        ),
        sortKey = SessionDetailsSortColumn.S3,
    ),
    SortableTableColumn(
        column = TableColumn(
            title = incidentsHeader,
            weight = 0.12f,
            align = TableColumnAlign.Center,
        ),
        sortKey = SessionDetailsSortColumn.Incidents,
    ),
    SortableTableColumn(
        column = TableColumn(
            title = deltaHeader,
            weight = 0.18f,
            align = TableColumnAlign.Center,
        ),
        sortKey = SessionDetailsSortColumn.Delta,
    ),
    SortableTableColumn(
        column = TableColumn(
            title = statusHeader,
            weight = 0.14f,
            align = TableColumnAlign.Center,
        ),
        sortKey = SessionDetailsSortColumn.Status,
    ),
)

@Composable
private fun SessionDetailsTableRow(rowIndex: Int, lap: SessionLapRowUi) {
    val baseColor = if (rowIndex % 2 == 0) {
        SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.18f)
    } else {
        SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.12f)
    }
    val rowColor = when (lap.status) {
        LapStatus.BestLap -> SimAnalyzerTheme.extended.purple.copy(alpha = 0.2f)
        LapStatus.Invalid -> SimAnalyzerTheme.extended.red.copy(alpha = 0.12f)
        else -> baseColor
    }

    TableRow(
        rowIndex = rowIndex,
        backgroundColor = rowColor,
        modifier = Modifier.height(56.dp),
    ) {
        TableCell(
            text = lap.lapLabel,
            weight = 0.08f,
            align = TableColumnAlign.Center,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
        )
        TableCell(
            text = lap.totalTime,
            weight = 0.18f,
            align = TableColumnAlign.Center,
            color = if (lap.status == LapStatus.BestLap) {
                SimAnalyzerTheme.extended.purple
            } else {
                SimAnalyzerTheme.material.onSurface
            },
            textStyle = SimAnalyzerTheme.typography.labelMedium,
        )
        TableCell(
            text = lap.s1,
            weight = 0.1f,
            align = TableColumnAlign.Center,
            color = if (lap.status == LapStatus.BestLap) {
                SimAnalyzerTheme.extended.purple
            } else {
                SimAnalyzerTheme.material.onSurface
            },
        )
        TableCell(
            text = lap.s2,
            weight = 0.1f,
            align = TableColumnAlign.Center,
            color = if (lap.status == LapStatus.BestLap) {
                SimAnalyzerTheme.extended.purple
            } else {
                SimAnalyzerTheme.material.onSurface
            },
        )
        TableCell(
            text = lap.s3,
            weight = 0.1f,
            align = TableColumnAlign.Center,
            color = if (lap.status == LapStatus.BestLap) {
                SimAnalyzerTheme.extended.purple
            } else {
                SimAnalyzerTheme.material.onSurface
            },
        )
        TableCell(
            text = lap.incidents,
            weight = 0.12f,
            align = TableColumnAlign.Center,
            color = if (lap.status == LapStatus.Invalid) {
                SimAnalyzerTheme.extended.red
            } else {
                SimAnalyzerTheme.material.onSurfaceVariant
            },
        )
        TableCell(
            text = lap.delta,
            weight = 0.18f,
            align = TableColumnAlign.Center,
            color = deltaColor(delta = lap.delta, isPositive = lap.deltaIsPositive),
        )
        Box(
            modifier = Modifier.weight(0.14f),
            contentAlignment = Alignment.Center,
        ) {
            StatusChip(status = lap.status)
        }
    }
}

@Composable
private fun deltaColor(delta: String, isPositive: Boolean): Color {
    if (delta == "--") return SimAnalyzerTheme.material.onSurfaceVariant
    return if (isPositive) SimAnalyzerTheme.extended.red else SimAnalyzerTheme.extended.lightGreen
}

@Preview
@Composable
private fun SessionDetailsLapTablePreview() {
    SimAnalyzerTheme {
        SessionDetailsLapTable(
            state = SessionDetailState(
                header = SessionDetailHeaderUi(),
                stats = SessionDetailStatsUi(),
                sortFilter = SessionDetailFilterUiModel(
                    kind = SessionDetailFilterKind.Sort,
                    selectedId = "lap",
                    options = persistentListOf(SessionDetailFilterOptionUi("lap")),
                ),
                showFilter = SessionDetailFilterUiModel(
                    kind = SessionDetailFilterKind.Show,
                    selectedId = "all",
                    options = persistentListOf(SessionDetailFilterOptionUi("all")),
                ),
                page = 1,
                pageCount = 4,
                visibleLaps = persistentListOf(
                    SessionLapRowUi(
                        lapNumber = 1,
                        lapLabel = "1",
                        sessionTypeLabel = "Qualifying",
                        totalTimeMs = 121_253,
                        totalTime = "2:01.253",
                        s1 = "39.012",
                        s2 = "41.532",
                        s3 = "40.709",
                        incidents = "0",
                        delta = "-0.000",
                        deltaIsPositive = false,
                        status = LapStatus.BestLap,
                    ),
                    SessionLapRowUi(
                        lapNumber = 2,
                        lapLabel = "2",
                        sessionTypeLabel = "Qualifying",
                        totalTimeMs = 122_918,
                        totalTime = "2:02.918",
                        s1 = "39.412",
                        s2 = "41.962",
                        s3 = "41.544",
                        incidents = "1",
                        delta = "+1.665",
                        deltaIsPositive = true,
                        status = LapStatus.Dirty,
                    ),
                ),
            ),
            onPageChange = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
