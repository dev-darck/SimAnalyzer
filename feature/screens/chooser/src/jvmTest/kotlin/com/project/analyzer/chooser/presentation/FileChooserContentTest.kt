@file:OptIn(ExperimentalTestApi::class)

package com.project.analyzer.chooser.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.project.analyzer.chooser.SelectionMode
import com.project.analyzer.chooser.domain.model.File
import com.project.analyzer.chooser.domain.model.TreeNode
import com.project.analyzer.chooser.presentation.toUi
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.trackRecompositions
import com.project.analyzer.ui.modifier.uiTestTag
import com.project.analyzer.ui.testing.assertRecompositionCountAtMost
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import kotlin.test.assertEquals

class FileChooserContentTest {

    @Test
    fun `file chooser reacts to place selection with bounded recompositions`() = runDesktopComposeUiTest {
        val state = FileChooserStateHolder(
            value = FileUiState(
                currentDir = "C:\\",
                selected = "C:\\report.txt",
                selectedDrive = "C:\\",
                selectionMode = SelectionMode.FILE,
                drives = persistentListOf(File(label = "Local Disk (C:)", path = "C:\\").toUi()),
                places = persistentListOf(File(label = "Downloads", path = "C:\\Users\\Oleg\\Downloads").toUi()),
                treeNodes = persistentListOf(
                    TreeNode(path = "C:\\Users\\Oleg\\Downloads", name = "Downloads", depth = 0).toUi(),
                ),
            ),
        )
        var confirmedPath: String? = null

        setContent {
            SimAnalyzerTheme {
                Content(
                    uiState = state.value,
                    dispatch = { intent ->
                        when (intent) {
                            is FileChooserIntent.ClickPlace -> {
                                state.value = state.value.copy(
                                    currentDir = intent.place.path,
                                    selected = intent.place.path,
                                    selectedDrive = "C:\\",
                                )
                            }

                            is FileChooserIntent.SelectPath -> {
                                state.value = state.value.copy(selected = intent.path)
                            }

                            FileChooserIntent.ToggleHidden -> {
                                state.value = state.value.copy(showHidden = !state.value.showHidden)
                            }

                            else -> Unit
                        }
                    },
                    onConfirm = { confirmedPath = it },
                    modifier = Modifier
                        .uiTestTag(TestTags.FileChooser)
                        .trackRecompositions(),
                )
            }
        }

        onAllNodesWithText("Downloads").assertCountEquals(2)
        onAllNodesWithText("Downloads")[0].assertIsDisplayed()
        onNodeWithTag(TestTags.FileChooser.value).assertRecompositionCountAtMost(1)

        onAllNodesWithText("Downloads")[0].performClick()
        waitForIdle()

        onAllNodesWithText("C:\\Users\\Oleg\\Downloads").assertCountEquals(2)
        onNodeWithTag(TestTags.FileChooser.value).assertRecompositionCountAtMost(3)

        onNodeWithText("Select file").performClick()
        waitForIdle()

        runOnIdle {
            assertEquals("C:\\Users\\Oleg\\Downloads", confirmedPath)
        }
    }
}

private class FileChooserStateHolder(value: FileUiState) {

    var value by mutableStateOf(value)
}
