package com.project.analyzer.impl.compose

import com.project.analyzer.utils.logger.RATE_LIMITED
import com.project.analyzer.utils.logger.logger
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

internal object OverlayCompatibilityGuard {

    private val log = logger()
    private val lastLoggedConflict = AtomicReference<String?>(null)

    fun detectBlockingOverlay(): String? {
        val conflict = detectKnownConflict()
        if (conflict != null) {
            logConflictOnce(conflict)
        }
        return conflict
    }

    internal fun detectKnownConflict(): String? {
        if (!isEnabled()) return null
        return runCatching { detectConflictFromProcessSnapshot() }.getOrNull()
    }

    internal fun isEnabled(
        propertyValue: String? = System.getProperty(EXTERNAL_OVERLAY_GUARD_PROP),
        envValue: String? = System.getenv(EXTERNAL_OVERLAY_GUARD_ENV),
    ): Boolean {
        val propertyFlag = propertyValue?.parseBoolFlag()
        val envFlag = envValue?.parseBoolFlag()
        return propertyFlag ?: envFlag ?: true
    }

    internal fun resolveKnownConflict(command: String?, commandLine: String?): String? {
        val descriptor = buildString {
            command?.let {
                append(it)
                append(' ')
            }
            commandLine?.let { append(it) }
        }.lowercase(Locale.US)

        if (descriptor.isBlank()) return null

        return when {
            NVIDIA_OVERLAY_TOKENS.any(descriptor::contains) -> NVIDIA_OVERLAY_NAME
            else -> null
        }
    }

    private fun logConflictOnce(conflict: String) {
        if (!lastLoggedConflict.compareAndSet(null, conflict) && lastLoggedConflict.get() == conflict) {
            return
        }
        log.atWarn(RATE_LIMITED) {
            message = "$conflict is active. SimAnalyzer HUD overlay was not started to avoid severe rendering slowdown."
        }
    }

    private fun String.parseBoolFlag(): Boolean? {
        val normalized = trim().lowercase(Locale.US)
        return when (normalized) {
            "1", "true", "yes", "on" -> true
            "0", "false", "no", "off" -> false
            else -> null
        }
    }

    private fun detectConflictFromProcessSnapshot(): String? {
        val processHandleStream = ProcessHandle.allProcesses()
        try {
            val iterator = processHandleStream.iterator()
            while (iterator.hasNext()) {
                val processHandle = iterator.next()
                val conflict = resolveKnownConflict(
                    command = processHandle.info().command().orElse(null),
                    commandLine = processHandle.info().commandLine().orElse(null),
                )
                if (conflict != null) {
                    return conflict
                }
            }
            return null
        } finally {
            processHandleStream.close()
        }
    }

    private const val EXTERNAL_OVERLAY_GUARD_PROP = "simanalyzer.hud.externalOverlayGuard"
    private const val EXTERNAL_OVERLAY_GUARD_ENV = "SIMANALYZER_HUD_EXTERNAL_OVERLAY_GUARD"
    private const val NVIDIA_OVERLAY_NAME = "NVIDIA in-game overlay"
    private val NVIDIA_OVERLAY_TOKENS = listOf(
        "nvidia share.exe",
        "nvidia overlay.exe",
        "nvsphelper64.exe",
        "nvsphelper.exe",
        "shadowplay",
    )
}
