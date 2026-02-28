package com.project.analyzer.utils.logger

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import com.project.analyzer.utils.BuildConfig
import org.slf4j.LoggerFactory
import java.io.File

public object LogbackConfigurator {

    private var configured = false

    public fun configure(logsDir: File) {
        if (configured) return
        configured = true

        System.setProperty("LOG_DIR", logsDir.absolutePath)

        val mode = resolveMode()
        when (mode) {
            LogMode.DEBUG -> {
                System.setProperty("ROOT_LEVEL", "DEBUG")
                System.setProperty("CONSOLE_LEVEL", "DEBUG")
                System.setProperty("FILE_LEVEL", "DEBUG")
            }

            LogMode.PROD -> {
                System.setProperty("ROOT_LEVEL", "INFO")
                System.setProperty("CONSOLE_LEVEL", "OFF")
                System.setProperty("FILE_LEVEL", "INFO")
            }
        }

        val context = LoggerFactory.getILoggerFactory() as? LoggerContext ?: return
        context.reset()

        val configurator = JoranConfigurator().apply { this.context = context }

        val configUrl = LogbackConfigurator::class.java.getResource("/logback.xml")
        if (configUrl != null) {
            configurator.doConfigure(configUrl)
        }
    }

    private fun resolveMode(): LogMode {
        val override = System.getProperty(LOG_MODE_PROP)
            ?: System.getenv(LOG_MODE_ENV)
        return when (override?.trim()?.lowercase()) {
            "debug" -> LogMode.DEBUG
            "prod", "production", "release" -> LogMode.PROD
            else -> if (BuildConfig.IS_DEBUG) LogMode.DEBUG else LogMode.PROD
        }
    }

    private enum class LogMode {
        DEBUG,
        PROD,
    }

    private const val LOG_MODE_PROP = "simanalyzer.log.mode"
    private const val LOG_MODE_ENV = "SIMANALYZER_LOG_MODE"
}
