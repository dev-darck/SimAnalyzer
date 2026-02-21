package com.project.analyzer.utils.logger

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import com.project.analyzer.utils.AppPaths
import com.project.analyzer.utils.BuildConfig
import org.slf4j.LoggerFactory

public object LogbackConfigurator {

    private var configured = false

    public fun configure() {
        if (configured) return
        configured = true

        val logsDir = AppPaths.logsDir
        System.setProperty("LOG_DIR", logsDir.absolutePath)

        System.setProperty("CONSOLE_LEVEL", if (BuildConfig.IS_DEBUG) "DEBUG" else "OFF")
        System.setProperty("FILE_LEVEL", if (BuildConfig.IS_DEBUG) "DEBUG" else "INFO")

        val context = LoggerFactory.getILoggerFactory() as? LoggerContext ?: return
        context.reset()

        val configurator = JoranConfigurator().apply { this.context = context }

        val configUrl = LogbackConfigurator::class.java.getResource("/logback.xml")
        if (configUrl != null) {
            configurator.doConfigure(configUrl)
        }
    }
}
