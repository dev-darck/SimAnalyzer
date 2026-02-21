package com.project.analyzer.leak.impl

import com.project.analyzer.leak.BuildConfig

internal const val PROP_ENABLED = "simanalyzer.leakcanary.enabled"
private const val PROP_WATCH_DELAY_MS = "simanalyzer.leakcanary.watchDelayMillis"
private const val PROP_ANALYSIS_COOLDOWN_MS = "simanalyzer.leakcanary.analysisCooldownMillis"
private const val PROP_MAX_DUMPS = "simanalyzer.leakcanary.maxDumps"
private const val PROP_RETAIN_DUMPS = "simanalyzer.leakcanary.retainHeapDumps"

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
            val enabled = readBoolean(PROP_ENABLED, BuildConfig.IS_DEBUG)
            val watchDelayMillis = readLong(PROP_WATCH_DELAY_MS, DEFAULT_WATCH_DELAY_MS)
            val analysisCooldownMillis = readLong(PROP_ANALYSIS_COOLDOWN_MS, DEFAULT_ANALYSIS_COOLDOWN_MS)
            val maxDumps = readInt(PROP_MAX_DUMPS, DEFAULT_MAX_DUMPS)
            val retainDumps = readBoolean(PROP_RETAIN_DUMPS, false)

            return LeakCanaryConfig(
                enabled = enabled,
                watchDelayMillis = watchDelayMillis,
                analysisCooldownMillis = analysisCooldownMillis,
                maxStoredHeapDumps = maxDumps,
                retainHeapDumpOnNoLeaks = retainDumps,
                dumpDirectoryName = "leakcanary",
            )
        }

        private fun readBoolean(name: String, default: Boolean): Boolean {
            val value = System.getProperty(name)?.trim()?.lowercase()
            return when (value) {
                "true", "1", "yes", "y" -> true
                "false", "0", "no", "n" -> false
                null, "" -> default
                else -> default
            }
        }

        private fun readLong(name: String, default: Long): Long {
            val value = System.getProperty(name)?.trim()?.toLongOrNull()
            return if (value != null && value > 0) value else default
        }

        private fun readInt(name: String, default: Int): Int {
            val value = System.getProperty(name)?.trim()?.toIntOrNull()
            return if (value != null && value > 0) value else default
        }
    }
}
