package com.project.analyzer.chooser.presentation

import com.project.analyzer.chooser.SelectionMode
import com.project.analyzer.chooser.domain.model.File
import com.project.analyzer.chooser.domain.model.FsEntry
import com.project.analyzer.chooser.domain.model.TreeNode

data class FileUiState(
    val currentDir: String = "",
    val selected: String = "",
    val selectedDrive: String = "",
    val selectionMode: SelectionMode = SelectionMode.FILE,

    val drives: List<File> = emptyList(),
    val places: List<File> = emptyList(),

    val treeNodes: List<TreeNode> = emptyList(),

    val scrollToIndex: Int = -1,

    val entries: List<FsEntry> = emptyList(),

    val showHidden: Boolean = false,

    val error: String? = null,
)

val FileUiState.canConfirm: Boolean
    get() = when (selectionMode) {
        SelectionMode.FILE -> selected.isNotEmpty()
        SelectionMode.DIRECTORY -> true
    }

val FileUiState.confirmPath: String
    get() = when (selectionMode) {
        SelectionMode.FILE -> selected
        SelectionMode.DIRECTORY -> selected.ifEmpty { currentDir }
    }

val FileUiState.displaySelectedPath: String
    get() = selected.ifEmpty {
        if (selectionMode == SelectionMode.DIRECTORY) currentDir else ""
    }
