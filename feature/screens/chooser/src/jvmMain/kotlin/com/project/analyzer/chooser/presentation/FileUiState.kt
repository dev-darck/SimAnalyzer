package com.project.analyzer.chooser.presentation

import com.project.analyzer.chooser.SelectionMode
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

internal data class FileUiState(
    val currentDir: String = "",
    val selected: String = "",
    val selectedDrive: String = "",
    val selectionMode: SelectionMode = SelectionMode.FILE,

    val drives: PersistentList<FileChooserLocationUi> = persistentListOf(),
    val places: PersistentList<FileChooserLocationUi> = persistentListOf(),

    val treeNodes: PersistentList<FileChooserTreeNodeUi> = persistentListOf(),

    val scrollToIndex: Int = -1,

    val showHidden: Boolean = false,

    val error: String? = null,
)

internal val FileUiState.canConfirm: Boolean
    get() = when (selectionMode) {
        SelectionMode.FILE -> selected.isNotEmpty()
        SelectionMode.DIRECTORY -> true
    }

internal val FileUiState.confirmPath: String
    get() = when (selectionMode) {
        SelectionMode.FILE -> selected
        SelectionMode.DIRECTORY -> selected.ifEmpty { currentDir }
    }

internal val FileUiState.displaySelectedPath: String
    get() = selected.ifEmpty {
        if (selectionMode == SelectionMode.DIRECTORY) currentDir else ""
    }
