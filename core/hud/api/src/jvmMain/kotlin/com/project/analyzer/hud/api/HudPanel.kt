package com.project.analyzer.hud.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset

@Stable
public interface HudPanel {

    public val id: String
    public val description: String get() = ""
    public val zIndex: Int get() = 0
    public val defaultAnchor: HudAnchor get() = HudAnchor.TopLeft
    public val defaultMarginPx: IntOffset get() = IntOffset(16, 16)
    public val hasSettings: Boolean get() = false
    public val isDevOnly: Boolean get() = false

    @Composable
    public fun DemoContent(modifier: Modifier) {
    }

    @Composable
    public fun Content(modifier: Modifier) {
    }

    @Composable
    public fun SettingsContent(modifier: Modifier) {
    }
}
