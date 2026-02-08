package com.project.analyzer.chooser.data.model

data class SidebarInfo(
    val default: String,
    val shortcutFiles: List<Candidate>,
    val drives: List<Candidate>
)