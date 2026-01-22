package com.project.analyzer.hud.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset

public interface HudPanel {

    public val id: String
    public val zIndex: Int get() = 0
    public val defaultOffset: IntOffset get() = IntOffset.Zero

    @Composable
    public fun DemoContent(modifier: Modifier) {
    }

    @Composable
    public fun Content(modifier: Modifier) {
    }
}
