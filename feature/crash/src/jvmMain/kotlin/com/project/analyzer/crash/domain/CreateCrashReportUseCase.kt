package com.project.analyzer.crash.domain

import org.slf4j.LoggerFactory
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CreateCrashReportUseCase {

    private val logger = LoggerFactory.getLogger("CrashReporter")

    private val timeFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        .withZone(ZoneId.systemDefault())

    operator fun invoke(
        throwable: Throwable,
        thread: Thread,
        title: String = "Application crash",
        appVersion: String? = null,
    ): CrashReport {
        val logDirFile = resolveLogDir().apply { mkdirs() }

        val fileStamp = DateTimeFormatter
            .ofPattern("yyyy-MM-dd_HH-mm-ss.SSS")
            .withZone(ZoneId.systemDefault())
            .format(Instant.now())

        val crashFile = File(logDirFile, "crash-$fileStamp.txt")

        val report = build(
            throwable = throwable,
            thread = thread,
            title = title,
            appVersion = appVersion,
            logDir = logDirFile.absolutePath,
            savedReportPath = crashFile.absolutePath,
        )

        runCatching {
            crashFile.writeText(report.fullText, StandardCharsets.UTF_8)
        }.onFailure { e ->
            logger.warn("Failed to write crash report file: ${crashFile.absolutePath}", e)
        }

        logger.error("CRASH REPORT:\n{}", report.fullText)
        logger.error("CRASH THROWABLE (structured stacktrace):", throwable)

        return report
    }

    private fun build(
        throwable: Throwable,
        thread: Thread,
        title: String,
        appVersion: String?,
        logDir: String,
        savedReportPath: String?,
    ): CrashReport {
        val time = timeFormatter.format(Instant.now())

        val os = buildString {
            append(System.getProperty("os.name") ?: "Unknown OS")
            append(" ")
            append(System.getProperty("os.version") ?: "?")
            append(" (")
            append(System.getProperty("os.arch") ?: "?")
            append(")")
        }

        val java = buildString {
            append(System.getProperty("java.vm.name") ?: "JVM")
            append(" / ")
            append(
                System.getProperty("java.runtime.version")
                    ?: System.getProperty("java.version")
                    ?: "?",
            )
        }

        val stacktrace = throwable.stackTraceToStringSafe()

        val fullText = buildString {
            appendLine("=== $title ===")
            appendLine("Time: $time")
            appendLine("Thread: ${thread.name}")
            if (!appVersion.isNullOrBlank()) appendLine("App: $appVersion")
            appendLine("OS: $os")
            appendLine("Java: $java")
            if (logDir.isNotBlank()) appendLine("Logs: $logDir")
            if (!savedReportPath.isNullOrBlank()) appendLine("Saved report: $savedReportPath")
            appendLine()
            appendLine("--- Exception ---")
            appendLine(throwable::class.qualifiedName ?: throwable::class.simpleName ?: "Throwable")
            appendLine(throwable.message ?: "")
            appendLine()
            appendLine("--- Stacktrace ---")
            appendLine(stacktrace)
        }

        return CrashReport(
            title = title,
            time = time,
            threadName = thread.name,
            os = os,
            java = java,
            appVersion = appVersion,
            stacktrace = stacktrace,
            fullText = fullText,
            logDir = logDir,
            savedReportPath = savedReportPath,
        )
    }

    private fun Throwable.stackTraceToStringSafe(): String = try {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        printStackTrace(pw)
        pw.flush()
        sw.toString()
    } catch (_: Throwable) {
        toString()
    }

    private fun resolveLogDir(): File {
        val fromProp = System.getProperty("LOG_DIR")?.trim().orEmpty()
        val fromEnv = System.getenv("LOG_DIR")?.trim().orEmpty()
        val path = when {
            fromProp.isNotEmpty() -> fromProp
            fromEnv.isNotEmpty() -> fromEnv
            else -> "./logs"
        }
        return File(path)
    }
}
