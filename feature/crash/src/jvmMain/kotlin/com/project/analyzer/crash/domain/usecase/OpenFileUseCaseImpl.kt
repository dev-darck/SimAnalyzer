package com.project.analyzer.crash.domain.usecase

import java.awt.Desktop
import java.io.File

internal class OpenFileUseCaseImpl : OpenFileUseCase {

    override fun openFile(path: String): Result<Unit> = runCatching {
        val file = File(path)
        if (!Desktop.isDesktopSupported()) error("Desktop is not supported")
        Desktop.getDesktop().open(file)
    }
}
