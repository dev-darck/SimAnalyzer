package com.project.analyzer.app.sidebar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.project.analyzer.navigation.api.Root

@Immutable
data class NavItem(val key: Root, val title: String, val icon: ImageVector)
