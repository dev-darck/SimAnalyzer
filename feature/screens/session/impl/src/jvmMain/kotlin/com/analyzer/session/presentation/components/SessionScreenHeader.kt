package com.analyzer.session.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.analyzer.session.presentation.model.FILTER_ALL_ID
import com.analyzer.session.presentation.model.SessionFilterKind
import com.analyzer.session.presentation.model.SessionFilterOptionUi
import com.analyzer.session.presentation.model.SessionFilterUiModel
import com.analyzer.session.presentation.model.SessionListIntent
import com.analyzer.session.presentation.model.SessionListState
import com.project.analyzer.feature.screens.session.impl.Res.Res
import com.project.analyzer.feature.screens.session.impl.Res.session_search_placeholder
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.components.FilterDropdown
import com.project.analyzer.ui.components.ResponsivePanelCard
import com.project.analyzer.ui.scrollbar.AppHorizontalScrollbar
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SessionScreenHeader(
    state: SessionListState,
    onIntent: (SessionListIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dividerColor = SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.35f)

    ResponsivePanelCard(modifier = modifier.fillMaxWidth()) {
        if (maxWidth < SESSION_HEADER_COMPACT_BREAKPOINT) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SessionFiltersStrip(
                    state = state,
                    dividerColor = dividerColor,
                    onIntent = onIntent,
                    modifier = Modifier.fillMaxWidth(),
                )
                SearchField(
                    value = state.searchQuery,
                    placeholder = stringResource(Res.string.session_search_placeholder),
                    onValueChange = { onIntent(SessionListIntent.ChangeSearch(it)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SessionFiltersStrip(
                    state = state,
                    dividerColor = dividerColor,
                    onIntent = onIntent,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                SearchField(
                    value = state.searchQuery,
                    placeholder = stringResource(Res.string.session_search_placeholder),
                    onValueChange = { onIntent(SessionListIntent.ChangeSearch(it)) },
                    modifier = Modifier.widthIn(min = 220.dp, max = 320.dp),
                )
            }
        }
    }
}

private val SESSION_HEADER_COMPACT_BREAKPOINT = 960.dp

@Composable
private fun SessionFiltersStrip(
    state: SessionListState,
    dividerColor: Color,
    onIntent: (SessionListIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterDropdown(
                filter = state.gameFilter.asDropdownFilter(),
                onSelect = { onIntent(SessionListIntent.ChangeGame(it)) },
            )
            FilterDropdown(
                filter = state.trackFilter.asDropdownFilter(),
                onSelect = { onIntent(SessionListIntent.ChangeTrack(it)) },
            )
            FilterDropdown(
                filter = state.carFilter.asDropdownFilter(),
                onSelect = { onIntent(SessionListIntent.ChangeCar(it)) },
            )
            FilterDropdown(
                filter = state.dateFilter.asDropdownFilter(),
                onSelect = { onIntent(SessionListIntent.ChangeDate(it)) },
            )
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .width(1.dp)
                    .background(dividerColor),
            )
            FilterDropdown(
                filter = state.sortFilter.asDropdownFilter(),
                onSelect = { onIntent(SessionListIntent.ChangeSort(it)) },
            )
        }
        AppHorizontalScrollbar(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(),
            adapter = rememberScrollbarAdapter(scrollState),
        )
    }
}

@Preview
@Composable
private fun SessionScreenHeaderPreview() {
    SimAnalyzerTheme {
        SessionScreenHeader(
            state = SessionListState(
                gameFilter = SessionFilterUiModel(
                    kind = SessionFilterKind.Game,
                    selectedId = FILTER_ALL_ID,
                    options = persistentListOf(
                        SessionFilterOptionUi(FILTER_ALL_ID),
                        SessionFilterOptionUi("acc", "ACC"),
                    ),
                ),
            ),
            onIntent = {},
        )
    }
}
