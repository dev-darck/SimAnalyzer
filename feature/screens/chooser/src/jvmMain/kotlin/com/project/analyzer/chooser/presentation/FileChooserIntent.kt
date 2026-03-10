package com.project.analyzer.chooser.presentation

import com.project.analyzer.chooser.SelectionMode
import com.project.analyzer.chooser.domain.model.File
import com.project.analyzer.chooser.domain.model.FsEntry

sealed interface FileChooserIntent {

    data class Init(val startPath: String? = null, val selectionMode: SelectionMode = SelectionMode.FILE) :
        FileChooserIntent

    data class SelectEntry(val entry: FsEntry) : FileChooserIntent
    data class SelectPath(val path: String) : FileChooserIntent
    data class OpenDirectory(val drive: File) : FileChooserIntent
    data class ClickPlace(val place: File) : FileChooserIntent
    data class ClickDrive(val drive: File) : FileChooserIntent
    data class ToggleExpand(val dir: String) : FileChooserIntent
    data object ToggleHidden : FileChooserIntent
    data object Refresh : FileChooserIntent
}
