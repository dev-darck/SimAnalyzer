package com.project.analyzer.crash.domain.usecase

import java.awt.Desktop
import java.io.File

class OpenFileUseCase {

    operator fun invoke(path: String): Result<Unit> = runCatching {
        val file = File(path)
        if (!Desktop.isDesktopSupported()) error("Desktop is not supported")
        Desktop.getDesktop().open(file)
    }
}
