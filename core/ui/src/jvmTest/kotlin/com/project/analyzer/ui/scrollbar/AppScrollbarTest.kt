@file:OptIn(ExperimentalTestApi::class)

package com.project.analyzer.ui.scrollbar

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.TestTags.VisibilityName
import com.project.analyzer.ui.modifier.trackRecompositions
import com.project.analyzer.ui.testing.assertRecompositionCountAtMost
import kotlinx.coroutines.runBlocking
import org.junit.Test

class AppScrollbarTest {

    @Test
    fun `vertical scrollbar reveals on scroll and auto hides`() = runDesktopComposeUiTest {
        lateinit var scrollState: ScrollState

        setContent {
            scrollState = rememberScrollState()
            VerticalScrollbarTestContent(scrollState = scrollState)
        }
        waitForIdle()
        mainClock.autoAdvance = false

        val scrollbar = onNodeWithTag(TestTags.ScrollbarVertical.value)
        scrollbar.assertVisibility(expected = false)
        scrollbar.assertRecompositionCountAtMost(2)

        runOnIdle {
            runBlocking {
                scrollState.scrollTo(180)
            }
        }
        mainClock.advanceTimeBy(160)
        waitForIdle()

        scrollbar.assertVisibility(expected = true)
        scrollbar.assertRecompositionCountAtMost(12)

        mainClock.advanceTimeBy(1300)
        waitForIdle()

        scrollbar.assertVisibility(expected = false)
        scrollbar.assertRecompositionCountAtMost(30)
    }

    @Test
    fun `horizontal scrollbar reveals on hover and hides on exit`() = runDesktopComposeUiTest {
        setContent {
            HorizontalScrollbarTestContent()
        }
        waitForIdle()
        mainClock.autoAdvance = false

        val scrollbar = onNodeWithTag(TestTags.ScrollbarHorizontal.value)
        scrollbar.assertVisibility(expected = false)
        scrollbar.assertRecompositionCountAtMost(2)

        scrollbar.performMouseInput {
            enter(center)
        }
        mainClock.advanceTimeBy(160)
        waitForIdle()

        scrollbar.assertVisibility(expected = true)
        scrollbar.assertRecompositionCountAtMost(12)

        scrollbar.performMouseInput {
            exit()
        }
        mainClock.advanceTimeBy(320)
        waitForIdle()

        scrollbar.assertVisibility(expected = false)
        scrollbar.assertRecompositionCountAtMost(30)
    }
}

@Composable
private fun VerticalScrollbarTestContent(scrollState: ScrollState) {
    val adapter = AppScrollbarAdapter(rememberScrollbarAdapter(scrollState))

    SimAnalyzerTheme {
        Box(
            modifier = Modifier
                .width(240.dp)
                .height(180.dp)
                .background(SimAnalyzerTheme.material.background),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(18) { index ->
                    Text(
                        text = "Vertical item ${index + 1}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.24f))
                            .padding(8.dp),
                        style = SimAnalyzerTheme.typography.bodyMedium,
                    )
                }
            }

            AppVerticalScrollbar(
                adapter = adapter,
                modifier = Modifier
                    .trackRecompositions()
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun HorizontalScrollbarTestContent() {
    val scrollState = rememberScrollState()
    val adapter = AppScrollbarAdapter(rememberScrollbarAdapter(scrollState))

    SimAnalyzerTheme {
        Box(
            modifier = Modifier
                .width(280.dp)
                .height(120.dp)
                .background(SimAnalyzerTheme.material.background),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(16.dp)
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(10) { index ->
                    Text(
                        text = "Horizontal item ${index + 1}",
                        modifier = Modifier
                            .width(120.dp)
                            .background(SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.24f))
                            .padding(8.dp),
                        style = SimAnalyzerTheme.typography.bodyMedium,
                    )
                }
            }

            AppHorizontalScrollbar(
                adapter = adapter,
                modifier = Modifier
                    .trackRecompositions()
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 12.dp),
            )
        }
    }
}

private fun SemanticsNodeInteraction.assertVisibility(expected: Boolean): SemanticsNodeInteraction {
    val actual = fetchSemanticsNode(
        errorMessageOnFail = "Expected scrollbar node to exist before reading visibility semantics.",
    ).config.getOrNull(AppScrollbarVisibilityKey)
        ?: error(
            "$VisibilityName semantics is not available for this scrollbar.",
        )

    if (actual != expected) {
        throw AssertionError("Expected scrollbar visibility=$expected, but was $actual.")
    }

    return this
}
