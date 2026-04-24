package com.project.analyzer.chooser.presentation

import com.project.analyzer.chooser.SelectionMode

internal sealed interface FileChooserIntent {

    data class Init(val startPath: String? = null, val selectionMode: SelectionMode = SelectionMode.FILE) :
        FileChooserIntent

    data class SelectPath(val path: String) : FileChooserIntent
    data class OpenDirectory(val drive: FileChooserLocationUi) : FileChooserIntent
    data class ClickPlace(val place: FileChooserLocationUi) : FileChooserIntent
    data class ClickDrive(val drive: FileChooserLocationUi) : FileChooserIntent
    data class ToggleExpand(val dir: String) : FileChooserIntent
    data object ToggleHidden : FileChooserIntent
    data object Refresh : FileChooserIntent
}
