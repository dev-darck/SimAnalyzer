package com.project.analyzer.app.win

import androidx.compose.foundation.layout.WindowInsets
import com.project.analyzer.app.win.hittest.WindowsHitTestRegistry
import com.project.analyzer.app.win.nativeWin.ComposeWindowProcedure
import java.awt.Window

class WindowsFrameController(private val window: Window, private val onWindowInsetUpdate: (WindowInsets) -> Unit) {

    val hitTestRegistry: WindowsHitTestRegistry = WindowsHitTestRegistry()

    private var procedure: ComposeWindowProcedure? = null

    fun install() {
        if (procedure != null) return

        procedure = ComposeWindowProcedure(
            window = window,
            hitTest = { x, y -> hitTestRegistry.hitTest(x, y) },
            onWindowInsetUpdate = onWindowInsetUpdate,
        )
    }

    fun dispose() {
        procedure?.dispose()
        procedure = null
    }
}
