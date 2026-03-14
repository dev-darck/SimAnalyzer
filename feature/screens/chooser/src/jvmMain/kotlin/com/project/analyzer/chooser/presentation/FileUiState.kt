package com.project.analyzer.chooser.presentation

import com.project.analyzer.chooser.SelectionMode
import com.project.analyzer.chooser.domain.model.File
import com.project.analyzer.chooser.domain.model.FsEntry
import com.project.analyzer.chooser.domain.model.TreeNode
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

data class FileUiState(
    val currentDir: String = "",
    val selected: String = "",
    val selectedDrive: String = "",
    val selectionMode: SelectionMode = SelectionMode.FILE,

    val drives: PersistentList<File> = persistentListOf(),
    val places: PersistentList<File> = persistentListOf(),

    val treeNodes: PersistentList<TreeNode> = persistentListOf(),

    val scrollToIndex: Int = -1,

    val entries: PersistentList<FsEntry> = persistentListOf(),

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
