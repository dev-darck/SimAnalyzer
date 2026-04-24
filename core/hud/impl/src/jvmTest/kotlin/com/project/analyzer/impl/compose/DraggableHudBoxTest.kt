@file:OptIn(ExperimentalTestApi::class)

package com.project.analyzer.impl.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.project.analyzer.game.api.GameWindowDetector
import com.project.analyzer.hud.api.HudStoredPosition
import com.project.analyzer.impl.setup.game.OverlayController
import com.project.analyzer.impl.setup.region.HitRegions
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.trackRecompositions
import com.project.analyzer.ui.modifier.uiTestTag
import com.project.analyzer.ui.testing.assertRecompositionCountAtMost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Test

class DraggableHudBoxTest {

    @Test
    fun `draggable hud box registers hit region with bounded recompositions`() = runDesktopComposeUiTest {
        val hitRegions = FakeHitRegions()
        val overlayController = OverlayController(
            gameDetector = FakeGameWindowDetector(),
            hitRegions = hitRegions,
            coroutineDispatcher = Dispatchers.Unconfined,
            swingDispatcher = Dispatchers.Unconfined,
        )
        var savedPosition: HudStoredPosition? = null

        setContent {
            SimAnalyzerTheme {
                DraggableHudBox(
                    panelId = "fuel",
                    offset = IntOffset.Zero,
                    containerSize = IntSize(width = 400, height = 240),
                    panelSize = IntSize(width = 160, height = 72),
                    hitRegions = hitRegions,
                    overlayController = overlayController,
                    onIntent = { intent ->
                        if (intent is HudIntent.SavePosition) {
                            savedPosition = intent.position
                        }
                    },
                    inputLocked = false,
                    onToggleInputLock = {},
                    modifier = Modifier
                        .uiTestTag(TestTags.DraggableHud)
                        .trackRecompositions(),
                ) {
                    Box(Modifier.size(width = 160.dp, height = 72.dp)) {
                        Text("HUD")
                    }
                }
            }
        }

        waitForIdle()

        onNodeWithText("HUD").assertIsDisplayed()
        onNodeWithTag(TestTags.DraggableHud.value).assertRecompositionCountAtMost(1)
        runOnIdle {
            check(hitRegions.snapshot().isNotEmpty()) {
                "Expected draggable HUD box to publish at least one hit region."
            }
            check(savedPosition == null) {
                "Position should not be saved without drag interaction."
            }
        }
    }
}

private class FakeGameWindowDetector : GameWindowDetector {

    override fun setOverlayHwnd(hwnd: com.sun.jna.platform.win32.WinDef.HWND?) = Unit

    override fun observeGameWindow(): Flow<com.project.analyzer.game.api.GameWindowInfo?> = emptyFlow()
}

private class FakeHitRegions : HitRegions {

    private val regions = linkedMapOf<String, IntRect>()
    private val changes = MutableStateFlow(emptyList<IntRect>())

    override fun snapshot(): List<IntRect> = regions.values.toList()

    override fun observeChanges(): Flow<List<IntRect>> = changes

    override fun put(key: String, rect: IntRect) {
        regions[key] = rect
        changes.value = snapshot()
    }

    override fun remove(key: String) {
        regions.remove(key)
        changes.value = snapshot()
    }

    override fun clear() {
        regions.clear()
        changes.value = emptyList()
    }
}
