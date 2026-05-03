package com.project.analyzer.chooser.presentation

import com.project.analyzer.chooser.domain.model.File
import com.project.analyzer.chooser.domain.model.TreeNode

internal data class FileChooserLocationUi(val label: String, val path: String)

internal data class FileChooserTreeNodeUi(
    val path: String,
    val name: String,
    val depth: Int,
    val expanded: Boolean = false,
    val loading: Boolean = false,
    val hasChildren: Boolean = true,
)

internal fun File.toUi(): FileChooserLocationUi = FileChooserLocationUi(
    label = label,
    path = path,
)

internal fun TreeNode.toUi(): FileChooserTreeNodeUi = FileChooserTreeNodeUi(
    path = path,
    name = name,
    depth = depth,
    expanded = expanded,
    loading = loading,
    hasChildren = hasChildren,
)
