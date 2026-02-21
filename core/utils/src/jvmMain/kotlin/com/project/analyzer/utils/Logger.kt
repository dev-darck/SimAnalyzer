package com.project.analyzer.utils

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration

public val logger: KLogger by lazy {
    KotlinLoggingConfiguration.loggerFactory.logger("SimAnalyzer")
}

public class NsRateLimiter(private val intervalNs: Long) {
    private var nextNs: Long = 0L

    public fun shouldLog(nowNs: Long): Boolean {
        if (nowNs >= nextNs) {
            nextNs = nowNs + intervalNs
            return true
        }
        return false
    }
}
