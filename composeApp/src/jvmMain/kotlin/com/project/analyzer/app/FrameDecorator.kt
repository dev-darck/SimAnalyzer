@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)

package com.project.analyzer.app

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.MutableWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.WindowScope
import com.project.analyzer.app.win.WindowsFrameController
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.utils.logger

@Composable
fun WindowScope.FrameDecorator(content: @Composable (FrameDecoratorState) -> Unit) {
    val paddingInsets = remember { MutableWindowInsets() }

    val winController = remember(window) {
        WindowsFrameController(
            window = window,
            onWindowInsetUpdate = { paddingInsets.insets = it },
        )
    }

    DisposableEffect(winController) {
        logger.info { "WindowsFrameController.install()" }
        winController.install()
        onDispose {
            logger.info { "WindowsFrameController.dispose()" }
            winController.dispose()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(paddingInsets),
        color = SimAnalyzerTheme.material.background,
    ) {
        content(
            FrameDecoratorState(
                win = winController,
            ),
        )
    }
}
