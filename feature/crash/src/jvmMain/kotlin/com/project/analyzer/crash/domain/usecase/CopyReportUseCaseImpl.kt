package com.project.analyzer.crash.domain.usecase

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

internal class CopyReportUseCaseImpl : CopyReportUseCase {

    override fun copyReport(text: String): Result<Unit> = runCatching {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }
}
