package com.project.analyzer.leak.impl

import com.project.analyzer.leak.BuildConfig

private const val DEFAULT_WATCH_DELAY_MS = 5_000L
private const val DEFAULT_ANALYSIS_COOLDOWN_MS = 60_000L
private const val DEFAULT_MAX_DUMPS = 3

internal data class LeakCanaryConfig(
    val enabled: Boolean,
    val watchDelayMillis: Long,
    val analysisCooldownMillis: Long,
    val maxStoredHeapDumps: Int,
    val retainHeapDumpOnNoLeaks: Boolean,
    val dumpDirectoryName: String,
) {

    companion object {

        fun fromSystemProperties(): LeakCanaryConfig {
            return LeakCanaryConfig(
                enabled = BuildConfig.IS_DEBUG,
                watchDelayMillis = DEFAULT_WATCH_DELAY_MS,
                analysisCooldownMillis = DEFAULT_ANALYSIS_COOLDOWN_MS,
                maxStoredHeapDumps = DEFAULT_MAX_DUMPS,
                retainHeapDumpOnNoLeaks = false,
                dumpDirectoryName = "leakcanary",
            )
        }
    }
}
