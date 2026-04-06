package com.project.analyzer.impl.compose

import com.project.analyzer.utils.logger.RATE_LIMITED
import java.util.Locale
import com.project.analyzer.utils.logger.logger as sharedLogger

private val overlayRenderCompatibilityLog = sharedLogger

public fun installOverlayRenderCompatibilityDefaults(
    osName: String = System.getProperty("os.name").orEmpty(),
    renderApiProperty: String? = System.getProperty(SKIKO_RENDER_API_PROP),
    renderApiEnv: String? = System.getenv(SKIKO_RENDER_API_ENV),
) {
    val conflict = OverlayCompatibilityGuard.detectKnownConflict() ?: return
    val renderApi = resolveOverlayRenderApiCompatibility(
        osName = osName,
        renderApiProperty = renderApiProperty,
        renderApiEnv = renderApiEnv,
        conflict = conflict,
    ) ?: return

    System.setProperty(SKIKO_RENDER_API_PROP, renderApi)
    overlayRenderCompatibilityLog.atInfo(RATE_LIMITED) {
        message = "Applied Skiko $renderApi compatibility mode because $conflict was detected on Windows."
    }
}

internal fun resolveOverlayRenderApiCompatibility(
    osName: String,
    renderApiProperty: String?,
    renderApiEnv: String?,
    conflict: String?,
): String? {
    if (conflict == null) return null
    if (!osName.lowercase(Locale.US).contains("windows")) return null
    if (!renderApiProperty.isNullOrBlank() || !renderApiEnv.isNullOrBlank()) return null

    return SKIKO_RENDER_API_OPENGL
}

private const val SKIKO_RENDER_API_PROP = "skiko.renderApi"
private const val SKIKO_RENDER_API_ENV = "SKIKO_RENDER_API"
private const val SKIKO_RENDER_API_OPENGL = "OPENGL"
