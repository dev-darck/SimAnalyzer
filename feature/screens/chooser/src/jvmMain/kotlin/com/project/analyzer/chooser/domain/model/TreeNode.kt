package com.project.analyzer.chooser.domain.model

data class TreeNode(
    val path: String,
    val name: String,
    val depth: Int,
    val expanded: Boolean = false,
    val loading: Boolean = false,
    val hasChildren: Boolean = true,
)
