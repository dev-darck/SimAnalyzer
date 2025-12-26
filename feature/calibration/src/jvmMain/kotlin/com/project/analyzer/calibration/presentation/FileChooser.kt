package com.project.analyzer.calibration.presentation

import java.awt.Component
import java.io.File
import javax.swing.JFileChooser

internal fun chooseSaveDirectory(parent: Component?): File? {
    val chooser = JFileChooser().apply {
        dialogTitle = "Chooser directory to save"
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        isAcceptAllFileFilterUsed = false
        isMultiSelectionEnabled = false
    }

    return when (chooser.showOpenDialog(parent)) {
        JFileChooser.APPROVE_OPTION -> chooser.selectedFile
        else -> null
    }
}
