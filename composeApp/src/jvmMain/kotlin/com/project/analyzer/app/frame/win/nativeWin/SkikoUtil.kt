package com.project.analyzer.app.frame.win.nativeWin

import androidx.compose.ui.awt.ComposeWindow
import org.jetbrains.skiko.SkiaLayer
import java.awt.Container
import javax.swing.JComponent

private fun <T : JComponent> findComponent(container: Container, klass: Class<T>): T? {
    val seq = container.components.asSequence()
    return seq.filter { klass.isInstance(it) }.ifEmpty {
        seq.filterIsInstance<Container>().mapNotNull { findComponent(it, klass) }
    }.map { klass.cast(it) }.firstOrNull()
}

private inline fun <reified T : JComponent> Container.findComponent(): T? = findComponent(this, T::class.java)

internal fun ComposeWindow.findSkiaLayer(): SkiaLayer? = findComponent<SkiaLayer>()
