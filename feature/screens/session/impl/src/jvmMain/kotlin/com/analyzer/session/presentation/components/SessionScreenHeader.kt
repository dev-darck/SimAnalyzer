package com.analyzer.session.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.analyzer.session.presentation.model.DropdownFilterUi
import com.analyzer.session.presentation.model.DropdownOptionUi
import com.analyzer.session.presentation.model.FILTER_ALL_ID
import com.analyzer.session.presentation.model.SessionListIntent
import com.analyzer.session.presentation.model.SessionListState
import com.project.analyzer.theme.SimAnalyzerTheme

@Composable
internal fun SessionScreenHeader(
    state: SessionListState,
    onIntent: (SessionListIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dividerColor = SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.35f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.shapes.large)
            .background(SimAnalyzerTheme.material.surface)
            .border(
                width = 1.dp,
                color = SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.25f),
                shape = SimAnalyzerTheme.shapes.large
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (maxWidth < 1080.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SessionFiltersStrip(
                    state = state,
                    dividerColor = dividerColor,
                    onIntent = onIntent,
                    modifier = Modifier.fillMaxWidth()
                )
                SearchField(
                    value = state.searchQuery,
                    placeholder = "Search sessions...",
                    onValueChange = { onIntent(SessionListIntent.ChangeSearch(it)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SessionFiltersStrip(
                    state = state,
                    dividerColor = dividerColor,
                    onIntent = onIntent,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                SearchField(
                    value = state.searchQuery,
                    placeholder = "Search sessions...",
                    onValueChange = { onIntent(SessionListIntent.ChangeSearch(it)) },
                    modifier = Modifier.widthIn(min = 220.dp, max = 320.dp)
                )
            }
        }
    }
}

@Composable
private fun SessionFiltersStrip(
    state: SessionListState,
    dividerColor: Color,
    onIntent: (SessionListIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterDropdown(
            filter = state.gameFilter,
            onSelect = { onIntent(SessionListIntent.ChangeGame(it)) }
        )
        FilterDropdown(
            filter = state.trackFilter,
            onSelect = { onIntent(SessionListIntent.ChangeTrack(it)) }
        )
        FilterDropdown(
            filter = state.carFilter,
            onSelect = { onIntent(SessionListIntent.ChangeCar(it)) }
        )
        FilterDropdown(
            filter = state.dateFilter,
            onSelect = { onIntent(SessionListIntent.ChangeDate(it)) }
        )
        Box(
            modifier = Modifier
                .height(28.dp)
                .width(1.dp)
                .background(dividerColor)
        )
        FilterDropdown(
            filter = state.sortFilter,
            onSelect = { onIntent(SessionListIntent.ChangeSort(it)) }
        )
    }
}

@Preview
@Composable
private fun SessionScreenHeaderPreview() {
    SimAnalyzerTheme {
        SessionScreenHeader(
            state = SessionListState(
                gameFilter = DropdownFilterUi(
                    label = "Game",
                    selectedId = FILTER_ALL_ID,
                    selectedLabel = "All",
                    options = listOf(
                        DropdownOptionUi(FILTER_ALL_ID, "All"),
                        DropdownOptionUi("acc", "ACC")
                    )
                )
            ),
            onIntent = {}
        )
    }
}
