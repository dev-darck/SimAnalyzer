package com.project.analyzer.impl.compose

import androidx.compose.ui.unit.IntOffset

sealed interface HudIntent {
    data class Show(val id: String) : HudIntent
    data class Hide(val id: String) : HudIntent
    data class Toggle(val id: String) : HudIntent
    data class Restart(val id: String) : HudIntent
    data object HideAll : HudIntent

    data class SavePosition(val id: String, val offset: IntOffset) : HudIntent
}
