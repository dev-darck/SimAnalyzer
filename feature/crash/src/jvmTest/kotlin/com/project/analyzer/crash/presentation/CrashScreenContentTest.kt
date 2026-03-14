@file:OptIn(ExperimentalTestApi::class)

package com.project.analyzer.crash.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.project.analyzer.crash.domain.CrashReport
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.trackRecompositions
import com.project.analyzer.ui.modifier.uiTestTag
import com.project.analyzer.ui.testing.assertRecompositionCountAtMost
import org.junit.Test
import kotlin.test.assertIs

class CrashScreenContentTest {

    @Test
    fun `crash screen switches tabs and dispatches actions with bounded recompositions`() = runDesktopComposeUiTest {
        val state = CrashEventState()
        val report = CrashReport(
            time = "2026-03-14 12:00:00",
            threadName = "main",
            stacktrace = "STACKTRACE-42",
            fullText = "FULL-REPORT-42",
            appVersion = "1.0.0",
            os = "Windows 11",
            java = "21",
        )

        setContent {
            SimAnalyzerTheme {
                CrashScreenContent(
                    report = report,
                    snackbarHostState = SnackbarHostState(),
                    onEvent = { state.lastEvent = it },
                    onExit = {},
                    modifier = Modifier
                        .uiTestTag(TestTags.CrashScreen)
                        .trackRecompositions(),
                )
            }
        }

        onNodeWithText("main").assertIsDisplayed()
        onNodeWithTag(TestTags.CrashScreen.value).assertRecompositionCountAtMost(1)

        onNodeWithText("Copy report").performClick()
        waitForIdle()

        runOnIdle {
            assertIs<CrashScreenUiEvent.CopyReport>(state.lastEvent)
        }
        onNodeWithTag(TestTags.CrashScreen.value).assertRecompositionCountAtMost(2)

        onNodeWithText("Full Report").performClick()
        waitForIdle()

        onNodeWithText("FULL-REPORT-42").assertIsDisplayed()
        onNodeWithTag(TestTags.CrashScreen.value).assertRecompositionCountAtMost(4)
    }
}

private class CrashEventState {

    var lastEvent: CrashScreenUiEvent? = null
}
