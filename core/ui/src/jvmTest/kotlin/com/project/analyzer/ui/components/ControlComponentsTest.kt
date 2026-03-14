@file:OptIn(ExperimentalTestApi::class)

package com.project.analyzer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.trackRecompositions
import com.project.analyzer.ui.modifier.uiTestTag
import com.project.analyzer.ui.testing.assertRecompositionCountAtMost
import com.project.analyzer.ui.textField.TextField
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

class ControlComponentsTest {

    @Test
    fun `button handles click with bounded recompositions`() = runDesktopComposeUiTest {
        val state = ClickState()

        setContent {
            SimAnalyzerTheme {
                Column {
                    Button(
                        text = "Apply",
                        onClick = { state.clicks += 1 },
                        modifier = Modifier
                            .uiTestTag(TestTags.Button)
                            .trackRecompositions(),
                    )
                    Text("clicks=${state.clicks}")
                }
            }
        }

        onNodeWithTag(TestTags.Button.value).assertRecompositionCountAtMost(1)

        onNodeWithText("Apply").performClick()
        waitForIdle()

        onNodeWithText("clicks=1").assertIsDisplayed()
        onNodeWithTag(TestTags.Button.value).assertRecompositionCountAtMost(3)
    }

    @Test
    fun `text field sanitizes input and stays bounded on recompositions`() = runDesktopComposeUiTest {
        val state = TextFieldState()

        setContent {
            SimAnalyzerTheme {
                TextField(
                    value = state.value,
                    onValueChange = { state.value = it },
                    valueSanitizer = { raw -> raw.filter(Char::isDigit) },
                    modifier = Modifier
                        .uiTestTag(TestTags.TextField)
                        .trackRecompositions(),
                    placeholder = "Enter digits",
                )
            }
        }

        onNodeWithTag(TestTags.TextField.value).assertRecompositionCountAtMost(1)

        onNodeWithTag(TestTags.TextFieldInput.value).performTextInput("a1b2")
        waitForIdle()

        onNodeWithTag(TestTags.TextFieldInput.value).assertTextEquals("12")
        onNodeWithTag(TestTags.TextField.value).assertRecompositionCountAtMost(4)
    }

    @Test
    fun `read only text field preserves value without extra recompositions`() = runDesktopComposeUiTest {
        setContent {
            SimAnalyzerTheme {
                TextField(
                    value = "Selected value",
                    readOnly = true,
                    modifier = Modifier
                        .uiTestTag(TestTags.TextField)
                        .trackRecompositions(),
                )
            }
        }

        onNodeWithTag(TestTags.TextField.value).assertRecompositionCountAtMost(1)
        onNodeWithTag(TestTags.TextFieldInput.value).assertTextEquals("Selected value")
        onNodeWithTag(TestTags.TextField.value).assertRecompositionCountAtMost(1)
    }

    @Test
    fun `pagination changes page with bounded recompositions`() = runDesktopComposeUiTest {
        val state = PaginationState(page = 2)

        setContent {
            SimAnalyzerTheme {
                Column {
                    Pagination(
                        page = state.page,
                        pageCount = 4,
                        onPageChange = { state.page = it },
                        modifier = Modifier
                            .uiTestTag(TestTags.Pagination)
                            .trackRecompositions(),
                    )
                    Text("page=${state.page}")
                }
            }
        }

        onNodeWithTag(TestTags.Pagination.value).assertRecompositionCountAtMost(1)

        onNodeWithText("4").performClick()
        waitForIdle()

        onNodeWithText("page=4").assertIsDisplayed()
        onNodeWithTag(TestTags.Pagination.value).assertRecompositionCountAtMost(4)
    }

    @Test
    fun `dropdown selects option and keeps recompositions bounded`() = runDesktopComposeUiTest {
        val state = DropdownState(selectedId = "acc")

        setContent {
            SimAnalyzerTheme {
                Column {
                    FilterDropdown(
                        filter = state.filterUi,
                        onSelect = { state.selectedId = it },
                        modifier = Modifier
                            .uiTestTag(TestTags.Dropdown)
                            .trackRecompositions(),
                        style = FilterDropdownStyle.Stacked,
                    )
                    Text("selected=${state.selectedId}")
                }
            }
        }

        onNodeWithTag(TestTags.Dropdown.value).assertRecompositionCountAtMost(1)

        onNodeWithText("Assetto Corsa Competizione").performClick()
        waitForIdle()

        onAllNodesWithTag(TestTags.ScrollbarVertical.value).assertCountEquals(1)
        onNodeWithText("iRacing").performClick()
        waitForIdle()

        onNodeWithText("selected=iracing").assertIsDisplayed()
        onNodeWithTag(TestTags.Dropdown.value).assertRecompositionCountAtMost(6)
    }

    @Test
    fun `info bar action click stays bounded on recompositions`() = runDesktopComposeUiTest {
        val state = ClickState()

        setContent {
            SimAnalyzerTheme {
                Column {
                    InfoBar(
                        title = "Warning",
                        message = "Low disk space",
                        showAction = true,
                        action = {
                            Text(
                                text = "Retry",
                                modifier = Modifier.clickable { state.clicks += 1 },
                            )
                        },
                        modifier = Modifier
                            .uiTestTag(TestTags.InfoBar)
                            .trackRecompositions(),
                    )
                    Text("actionClicks=${state.clicks}")
                }
            }
        }

        onNodeWithText("Warning").assertIsDisplayed()
        onNodeWithText("Low disk space").assertIsDisplayed()
        onNodeWithTag(TestTags.InfoBar.value).assertRecompositionCountAtMost(1)

        onNodeWithText("Retry").performClick()
        waitForIdle()

        onNodeWithText("actionClicks=1").assertIsDisplayed()
        onNodeWithTag(TestTags.InfoBar.value).assertRecompositionCountAtMost(2)
    }

    @Test
    fun `info bar snackbar host renders action flow with bounded recompositions`() = runDesktopComposeUiTest {
        val state = SnackbarResultState()

        setContent {
            val hostState = remember { SnackbarHostState() }

            LaunchedEffect(hostState) {
                state.result = hostState.showSnackbar(
                    visuals = InfoBarSnackbarVisuals(
                        title = "Saved",
                        message = "Profile updated",
                        severity = InfoBarSeverity.Success,
                        actionLabel = "Undo",
                    ),
                ).name
            }

            SimAnalyzerTheme {
                Column {
                    InfoBarSnackbarHost(
                        hostState = hostState,
                        modifier = Modifier
                            .uiTestTag(TestTags.InfoBar)
                            .trackRecompositions(),
                    )
                    Text("result=${state.result}")
                }
            }
        }

        waitForIdle()

        onNodeWithText("Saved").assertIsDisplayed()
        onNodeWithText("Profile updated").assertIsDisplayed()
        onNodeWithText("Undo").assertIsDisplayed()
        onNodeWithTag(TestTags.InfoBar.value).assertRecompositionCountAtMost(3)

        onNodeWithText("Undo").performClick()
        waitForIdle()

        onNodeWithText("result=ActionPerformed").assertIsDisplayed()
        onNodeWithTag(TestTags.InfoBar.value).assertRecompositionCountAtMost(4)
    }
}

private class ClickState {

    var clicks by mutableIntStateOf(0)
}

private class TextFieldState {

    var value by mutableStateOf("")
}

private class PaginationState(page: Int) {

    var page by mutableIntStateOf(page)
}

private class DropdownState(selectedId: String) {

    var selectedId by mutableStateOf(selectedId)

    val filterUi: DropdownFilterUi
        get() = DropdownFilterUi(
            label = "Game",
            selectedId = selectedId,
            selectedLabel = options.first { it.id == selectedId }.label,
            options = options,
        )

    private val options = persistentListOf(
        DropdownOptionUi(id = "all", label = "All games"),
        DropdownOptionUi(id = "acc", label = "Assetto Corsa Competizione"),
        DropdownOptionUi(id = "ac", label = "Assetto Corsa"),
        DropdownOptionUi(id = "iracing", label = "iRacing"),
        DropdownOptionUi(id = "rf2", label = "rFactor 2"),
        DropdownOptionUi(id = "lmuu", label = "Le Mans Ultimate"),
        DropdownOptionUi(id = "wrc", label = "EA Sports WRC"),
        DropdownOptionUi(id = "f1", label = "F1 24"),
        DropdownOptionUi(id = "ams2", label = "Automobilista 2"),
    )
}

private class SnackbarResultState {

    var result by mutableStateOf("Pending")
}
