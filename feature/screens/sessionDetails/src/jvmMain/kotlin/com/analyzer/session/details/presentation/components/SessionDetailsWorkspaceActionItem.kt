package com.analyzer.session.details.presentation.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

internal data class SessionDetailsWorkspaceActionItem(
    val label: String,
    val contentDescription: String,
    val icon: ImageVector,
    val enabled: Boolean,
    val containerColor: Color,
    val contentColor: Color,
    val onClick: () -> Unit,
)
