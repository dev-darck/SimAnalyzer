@file:OptIn(ExperimentalTestApi::class)

package com.project.analyzer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.trackRecompositions
import com.project.analyzer.ui.modifier.uiTestTag
import com.project.analyzer.ui.testing.assertRecompositionCountAtMost
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

class PagedTableTest {

    @Test
    fun `sortable paged table forwards sort and page changes with bounded recompositions`() = runDesktopComposeUiTest {
        val state = SortableTableState()
        val mappings = tableSortMappings(
            TableSortMapping(
                sortKey = DemoSort.Date,
                ascSortId = "date-asc",
                descSortId = "date-desc",
            ),
            TableSortMapping(
                sortKey = DemoSort.Track,
                ascSortId = "track-asc",
                descSortId = "track-desc",
            ),
        )

        setContent {
            SimAnalyzerTheme {
                Column {
                    SortablePagedTable(
                        isLoading = false,
                        isEmpty = false,
                        loadingMessage = "Loading sessions",
                        emptyMessage = "No sessions",
                        errorMessage = null,
                        page = state.page,
                        pageCount = 3,
                        onPageChange = { state.page = it },
                        columns = persistentListOf(
                            SortableTableColumn(
                                column = TableColumn(title = "Date", weight = 0.45f),
                                sortKey = DemoSort.Date,
                            ),
                            SortableTableColumn(
                                column = TableColumn(title = "Track", weight = 0.55f),
                                sortKey = DemoSort.Track,
                            ),
                        ),
                        activeSort = mappings.activeSort(state.sortId),
                        onSortColumnClick = { clicked ->
                            state.sortId = mappings.nextSortId(clicked, state.sortId)
                        },
                        modifier = Modifier
                            .uiTestTag(TestTags.PagedTable)
                            .trackRecompositions(),
                        rowContent = {
                            TableRow(
                                rowIndex = 0,
                                modifier = Modifier.height(44.dp),
                            ) {
                                TableCell(text = "2025-10-24", weight = 0.45f)
                                TableCell(text = "Spa", weight = 0.55f)
                            }
                        },
                    )
                    Text("page=${state.page}")
                    Text("sort=${state.sortId}")
                }
            }
        }

        onNodeWithTag(TestTags.PagedTable.value).assertRecompositionCountAtMost(1)

        onNodeWithText("TRACK").performClick()
        waitForIdle()

        onNodeWithText("sort=track-asc").assertIsDisplayed()

        onNodeWithText("3").performClick()
        waitForIdle()

        onNodeWithText("page=3").assertIsDisplayed()
        onNodeWithTag(TestTags.PagedTable.value).assertRecompositionCountAtMost(6)
    }

    @Test
    fun `paged table shows empty state with bounded recompositions`() = runDesktopComposeUiTest {
        setContent {
            SimAnalyzerTheme {
                PagedTable(
                    isLoading = false,
                    isEmpty = true,
                    loadingMessage = "Loading rows",
                    emptyMessage = "No rows available",
                    errorMessage = null,
                    page = 1,
                    pageCount = 1,
                    onPageChange = {},
                    modifier = Modifier
                        .uiTestTag(TestTags.PagedTable)
                        .trackRecompositions(),
                    headerContent = {
                        TableHeader(
                            columns = persistentListOf(
                                TableColumn(title = "Date", weight = 0.5f),
                                TableColumn(title = "Track", weight = 0.5f),
                            ),
                        )
                    },
                )
            }
        }

        onNodeWithText("No rows available").assertIsDisplayed()
        onNodeWithTag(TestTags.PagedTable.value).assertRecompositionCountAtMost(1)
    }
}

private enum class DemoSort {
    Date,
    Track,
}

private class SortableTableState {

    var page by mutableIntStateOf(1)
    var sortId by mutableStateOf("date-asc")
}
