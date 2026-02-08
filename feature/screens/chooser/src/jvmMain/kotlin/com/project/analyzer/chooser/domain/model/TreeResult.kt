package com.project.analyzer.chooser.domain.model

data class TreeResult(
    val nodes: List<TreeNode>,
    val scrollToIndex: Int = -1,
)
