package com.project.analyzer.utils

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration

public val logger: KLogger by lazy {
    KotlinLoggingConfiguration.loggerFactory.logger("SimAnalyzer")
}
