package com.project.analyzer.crash.domain.usecase

import java.awt.Desktop
import java.io.File

class OpenLogsFolderUseCase {
    operator fun invoke(path: String): Result<Unit> = runCatching {
        val dir = File(path)
        dir.mkdirs()
        if (!Desktop.isDesktopSupported()) error("Desktop is not supported")
        Desktop.getDesktop().open(dir)
    }
}
