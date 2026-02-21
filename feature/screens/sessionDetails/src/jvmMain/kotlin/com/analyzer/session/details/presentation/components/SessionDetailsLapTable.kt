package com.analyzer.session.details.presentation.components

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.analyzer.session.details.presentation.model.DropdownFilterUi
import com.analyzer.session.details.presentation.model.DropdownOptionUi
import com.analyzer.session.details.presentation.model.LapStatus
import com.analyzer.session.details.presentation.model.SessionDetailHeaderUi
import com.analyzer.session.details.presentation.model.SessionDetailState
import com.analyzer.session.details.presentation.model.SessionDetailStatsUi
import com.analyzer.session.details.presentation.model.SessionLapRowUi
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.components.Pagination

@Composable
internal fun SessionDetailsLapTable(
    state: SessionDetailState,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dividerColor = SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.2f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.shapes.large)
            .background(SimAnalyzerTheme.material.surface)
            .border(
                1.dp,
                SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.25f),
                SimAnalyzerTheme.shapes.large,
            ),
    ) {
        SessionDetailsTableHeader()
        HorizontalDivider(color = dividerColor)

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = "Loading laps...",
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        } else if (state.visibleLaps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = state.error ?: "No laps recorded yet.",
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        } else {
            Column {
                state.visibleLaps.forEachIndexed { index, lap ->
                    SessionDetailsTableRow(lap = lap, isEven = index % 2 == 0)
                    if (index != state.visibleLaps.lastIndex) {
                        HorizontalDivider(color = dividerColor)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Pagination(
                page = state.page,
                pageCount = state.pageCount,
                onPageChange = onPageChange,
            )
        }
    }
}

@Composable
private fun SessionDetailsTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SimAnalyzerTheme.material.secondary)
            .height(44.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TableHeaderCell(text = "Lap", weight = 0.08f)
        TableHeaderCell(text = "Total Time", weight = 0.16f)
        TableHeaderCell(text = "S1", weight = 0.1f)
        TableHeaderCell(text = "S2", weight = 0.1f)
        TableHeaderCell(text = "S3", weight = 0.1f)
        TableHeaderCell(text = "Incidents", weight = 0.12f)
        TableHeaderCell(text = "Delta", weight = 0.14f)
        TableHeaderCell(text = "Status", weight = 0.2f)
    }
}

@Composable
private fun RowScope.TableHeaderCell(text: String, weight: Float) {
    Text(
        text = text.uppercase(),
        color = SimAnalyzerTheme.extended.onPrimaryContainer50,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.4.sp,
        modifier = Modifier.weight(weight),
    )
}

@Composable
private fun SessionDetailsTableRow(lap: SessionLapRowUi, isEven: Boolean) {
    val baseColor = if (isEven) {
        SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.18f)
    } else {
        SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.12f)
    }
    val rowColor = when (lap.status) {
        LapStatus.BestLap -> SimAnalyzerTheme.extended.purple.copy(alpha = 0.2f)
        LapStatus.Invalid -> SimAnalyzerTheme.extended.red.copy(alpha = 0.12f)
        else -> baseColor
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(rowColor)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TableCell(
            text = lap.lapLabel,
            weight = 0.08f,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
        )
        TableCell(
            text = lap.totalTime,
            weight = 0.16f,
            color = if (lap.status ==
                LapStatus.BestLap
            ) {
                SimAnalyzerTheme.extended.purple
            } else {
                SimAnalyzerTheme.material.onSurface
            },
            fontWeight = FontWeight.SemiBold,
        )
        TableCell(
            text = lap.s1,
            weight = 0.1f,
            color = if (lap.status ==
                LapStatus.BestLap
            ) {
                SimAnalyzerTheme.extended.purple
            } else {
                SimAnalyzerTheme.material.onSurface
            },
        )
        TableCell(
            text = lap.s2,
            weight = 0.1f,
            color = if (lap.status ==
                LapStatus.BestLap
            ) {
                SimAnalyzerTheme.extended.purple
            } else {
                SimAnalyzerTheme.material.onSurface
            },
        )
        TableCell(
            text = lap.s3,
            weight = 0.1f,
            color = if (lap.status ==
                LapStatus.BestLap
            ) {
                SimAnalyzerTheme.extended.purple
            } else {
                SimAnalyzerTheme.material.onSurface
            },
        )
        TableCell(
            text = lap.incidents,
            weight = 0.12f,
            color = if (lap.status ==
                LapStatus.Invalid
            ) {
                SimAnalyzerTheme.extended.red
            } else {
                SimAnalyzerTheme.material.onSurfaceVariant
            },
        )
        TableCell(
            text = lap.delta,
            weight = 0.14f,
            color = deltaColor(delta = lap.delta, isPositive = lap.deltaIsPositive),
        )
        Box(
            modifier = Modifier.weight(0.2f),
            contentAlignment = Alignment.CenterStart,
        ) {
            StatusChip(status = lap.status)
        }
    }
}

@Composable
private fun RowScope.TableCell(
    text: String,
    weight: Float,
    color: Color = SimAnalyzerTheme.material.onSurface,
    fontWeight: FontWeight = FontWeight.Medium,
) {
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        fontWeight = fontWeight,
        modifier = Modifier.weight(weight),
    )
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
                sortFilter = DropdownFilterUi(
                    label = "Sort by",
                    selectedId = "lap",
                    selectedLabel = "Lap",
                    options = listOf(DropdownOptionUi("lap", "Lap")),
                ),
                showFilter = DropdownFilterUi(
                    label = "Show",
                    selectedId = "all",
                    selectedLabel = "All laps",
                    options = listOf(DropdownOptionUi("all", "All laps")),
                ),
                page = 1,
                pageCount = 4,
                visibleLaps = listOf(
                    SessionLapRowUi(
                        lapNumber = 1,
                        lapLabel = "1",
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
