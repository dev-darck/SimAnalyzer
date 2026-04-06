package com.project.analyzer.impl.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
public fun rememberOverlayCompatibilityConflict(overlayRequested: Boolean): String? = remember(overlayRequested) {
    if (!overlayRequested) {
        null
    } else {
        OverlayCompatibilityGuard.detectBlockingOverlay()
    }
}
