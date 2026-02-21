package com.project.analyzer.ui.modifier

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier

public fun Modifier.onClick(enabled: Boolean = true, onClick: () -> Unit = {}): Modifier = clickable(
    enabled = enabled,
    onClick = onClick,
    indication = null,
    interactionSource = MutableInteractionSource(),
)
