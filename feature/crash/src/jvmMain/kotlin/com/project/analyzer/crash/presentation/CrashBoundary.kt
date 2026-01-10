@file:OptIn(ExperimentalAtomicApi::class, ExperimentalComposeUiApi::class)

package com.project.analyzer.crash.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.window.LocalWindowExceptionHandlerFactory
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.WindowExceptionHandlerFactory
import com.project.analyzer.crash.domain.CrashReport
import com.project.analyzer.crash.domain.CreateCrashReportUseCase
import java.awt.Dimension
import java.awt.Window
import javax.swing.JDialog
import javax.swing.SwingUtilities
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.system.exitProcess

private val crashDialogShown = AtomicBoolean(false)

@Composable
fun CrashBoundary(
    appVersion: String? = "dev",
    content: @Composable () -> Unit
) {
    val createCrashReport = remember { CreateCrashReportUseCase() }

    CompositionLocalProvider(
        LocalWindowExceptionHandlerFactory provides WindowExceptionHandlerFactory { window ->
            WindowExceptionHandler { throwable ->
                val report = createCrashReport(
                    throwable = throwable,
                    thread = Thread.currentThread(),
                    title = "SimAnalyzer crashed (WindowExceptionHandler)",
                    appVersion = appVersion
                )

                if (!crashDialogShown.compareAndSet(
                        expectedValue = false,
                        newValue = true
                    )
                ) return@WindowExceptionHandler

                window.isVisible = false

                if (SwingUtilities.isEventDispatchThread()) {
                    showCrashDialog(
                        owner = window,
                        crashReport = report,
                    )
                } else {
                    SwingUtilities.invokeLater {
                        showCrashDialog(
                            owner = window,
                            crashReport = report,
                        )
                    }
                }
            }
        }
    ) {
        content()
    }
}

private fun showCrashDialog(
    owner: Window,
    crashReport: CrashReport,
) {
    val b = owner.graphicsConfiguration.bounds

    val desiredW = 980
    val desiredH = 720

    val maxW = (b.width * 0.90).toInt().coerceAtLeast(640)
    val maxH = (b.height * 0.90).toInt().coerceAtLeast(480)

    val finalW = desiredW.coerceAtMost(maxW)
    val finalH = desiredH.coerceAtMost(maxH)

    val dialog = JDialog(owner).apply {
        title = "SimAnalyzer crashed"
        isModal = true
        isResizable = true
        isAlwaysOnTop = true

        defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE

        setSize(finalW, finalH)

        val x = b.x + (b.width - finalW) / 2
        val y = b.y + (b.height - finalH) / 2
        setLocation(x, y)
    }

    val composePanel = ComposePanel().apply {
        preferredSize = Dimension(finalW, finalH)

        setContent {
            CrashScreen(
                crashReport = crashReport,
                onExit = { exitProcess(1) },
            )
        }
    }

    dialog.contentPane = composePanel

    dialog.isVisible = true
    exitProcess(1)
}
