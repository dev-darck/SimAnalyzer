@file:OptIn(ExperimentalFoundationApi::class)

package com.analyzer.session.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.analyzer.session.presentation.model.DropdownFilterUi
import com.analyzer.session.presentation.model.DropdownOptionUi
import com.analyzer.session.presentation.model.FILTER_ALL_ID
import com.analyzer.session.presentation.model.SessionListIntent
import com.analyzer.session.presentation.model.SessionListState
import com.analyzer.session.presentation.model.SessionRowUi
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.components.Pagination
import com.project.analyzer.ui.modifier.onClick
import com.project.analyzer.ui.tooltip.Tooltip

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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.shapes.large)
            .background(SimAnalyzerTheme.material.surface)
            .border(
                width = 1.dp,
                color = SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.25f),
                shape = SimAnalyzerTheme.shapes.large,
            ),
    ) {
        SessionTableHeader(weights = weights, showGame = showGame)
        HorizontalDivider(color = dividerColor)

        if (state.isLoading) {
            EmptyStateMessage("Loading sessions...")
        } else if (state.visibleSessions.isEmpty()) {
            EmptyStateMessage(state.error ?: "No sessions recorded yet.")
        } else {
            Column {
                state.visibleSessions.forEachIndexed { index, session ->
                    SessionTableRow(
                        session = session,
                        onOpenDetails = onOpenDetails,
                        onSave = { onIntent(SessionListIntent.SaveSession(it)) },
                        onDelete = { onIntent(SessionListIntent.DeleteSession(it)) },
                        weights = weights,
                        showGame = showGame,
                        isEven = index % 2 == 0,
                    )
                    if (index != state.visibleSessions.lastIndex) {
                        HorizontalDivider(color = dividerColor)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SimAnalyzerTheme.material.secondaryContainer)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Pagination(
                page = state.page,
                pageCount = state.pageCount,
                onPageChange = { onIntent(SessionListIntent.ChangePage(it)) },
            )
        }
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
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun SessionTableHeader(weights: SessionTableWeights, showGame: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SimAnalyzerTheme.material.secondary)
            .height(44.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderCell(text = "Date", weight = weights.date, align = TableCellAlign.Start)
        if (showGame) {
            HeaderCell(text = "Game", weight = weights.game, align = TableCellAlign.Start)
        }
        HeaderCell(text = "Track", weight = weights.track, align = TableCellAlign.Start)
        HeaderCell(text = "Car Model", weight = weights.car, align = TableCellAlign.Start)
        HeaderCell(text = "Laps", weight = weights.laps, align = TableCellAlign.Center)
        HeaderCell(text = "Best Lap", weight = weights.best, align = TableCellAlign.Center)
        HeaderCell(text = "Actions", weight = weights.actions, align = TableCellAlign.Center)
    }
}

@Composable
private fun RowScope.HeaderCell(text: String, weight: Float, align: TableCellAlign) {
    val contentAlignment = when (align) {
        TableCellAlign.Start -> Alignment.CenterStart
        TableCellAlign.Center -> Alignment.Center
        TableCellAlign.End -> Alignment.CenterEnd
    }
    val textAlign = when (align) {
        TableCellAlign.Start -> TextAlign.Start
        TableCellAlign.Center -> TextAlign.Center
        TableCellAlign.End -> TextAlign.End
    }

    Box(
        modifier = Modifier.weight(weight),
        contentAlignment = contentAlignment,
    ) {
        Text(
            text = text.uppercase(),
            color = SimAnalyzerTheme.extended.onPrimaryContainer50,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp,
            textAlign = textAlign,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SessionTableRow(
    session: SessionRowUi,
    onOpenDetails: (Long) -> Unit,
    onSave: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    weights: SessionTableWeights,
    showGame: Boolean,
    isEven: Boolean,
) {
    val rowColor = if (isEven) {
        SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.18f)
    } else {
        SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.12f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .background(rowColor)
            .onClick { onOpenDetails(session.sessionId) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(weights.date)) {
            Text(
                text = session.dateLabel,
                color = SimAnalyzerTheme.material.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = session.timeLabel,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                fontSize = 10.sp,
            )
        }

        if (showGame) {
            TableTextCell(
                text = session.gameLabel,
                weight = weights.game,
                align = TableCellAlign.Start,
            )
        }
        TableTextCell(
            text = session.trackLabel,
            weight = weights.track,
            align = TableCellAlign.Start,
        )
        TableTextCell(
            text = session.carLabel,
            weight = weights.car,
            align = TableCellAlign.Start,
        )
        TableTextCell(
            text = session.lapsLabel,
            weight = weights.laps,
            align = TableCellAlign.Center,
        )
        TableTextCell(
            text = session.bestLapLabel,
            weight = weights.best,
            align = TableCellAlign.Center,
            fontWeight = FontWeight.SemiBold,
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
                    tooltip = "Saved",
                )
            } else {
                SessionActionButton(
                    icon = Icons.Outlined.BookmarkBorder,
                    tint = SimAnalyzerTheme.material.primary,
                    tooltip = "Save session",
                    onClick = { onSave(session.sessionId) },
                )
            }
            SessionActionButton(
                icon = Icons.Outlined.Delete,
                tint = SimAnalyzerTheme.material.error,
                tooltip = "Delete session",
                onClick = { onDelete(session.sessionId) },
            )
            SessionActionButton(
                icon = Icons.Filled.ChevronRight,
                tint = SimAnalyzerTheme.material.primary,
                tooltip = "Open details",
                onClick = { onOpenDetails(session.sessionId) },
            )
        }
    }
}

@Composable
private fun RowScope.TableTextCell(
    text: String,
    weight: Float,
    align: TableCellAlign,
    fontWeight: FontWeight = FontWeight.Medium,
) {
    val contentAlignment = when (align) {
        TableCellAlign.Start -> Alignment.CenterStart
        TableCellAlign.Center -> Alignment.Center
        TableCellAlign.End -> Alignment.CenterEnd
    }
    val textAlign = when (align) {
        TableCellAlign.Start -> TextAlign.Start
        TableCellAlign.Center -> TextAlign.Center
        TableCellAlign.End -> TextAlign.End
    }

    Box(
        modifier = Modifier.weight(weight),
        contentAlignment = contentAlignment,
    ) {
        Text(
            text = text,
            color = SimAnalyzerTheme.material.onSurface,
            fontSize = 12.sp,
            fontWeight = fontWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign,
        )
    }
}

@Composable
private fun SessionActionButton(icon: ImageVector, tint: Color, tooltip: String, onClick: (() -> Unit)? = null) {
    Tooltip(tooltip = tooltip) {
        val clickAction = onClick
        val clickable = clickAction != null
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
                .then(if (clickable) Modifier.onClick { clickAction.invoke() } else Modifier),
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

private enum class TableCellAlign {
    Start,
    Center,
    End,
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
        date = 0.18f,
        game = 0.11f,
        track = 0.17f,
        car = 0.18f,
        laps = 0.08f,
        best = 0.1f,
        actions = 0.18f,
    )
} else {
    SessionTableWeights(
        date = 0.22f,
        game = 0f,
        track = 0.21f,
        car = 0.21f,
        laps = 0.08f,
        best = 0.12f,
        actions = 0.16f,
    )
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
                gameFilter = DropdownFilterUi(
                    label = "Game",
                    selectedId = FILTER_ALL_ID,
                    selectedLabel = "All",
                    options = listOf(
                        DropdownOptionUi(FILTER_ALL_ID, "All"),
                        DropdownOptionUi("acc", "ACC"),
                    ),
                ),
                page = 1,
                pageCount = 9,
                sessions = sessions,
                visibleSessions = sessions,
            ),
            onOpenDetails = {},
            onIntent = {},
        )
    }
}
