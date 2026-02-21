package com.project.analyzer.chooser.data.model

data class Candidate(
    val displayName: String,
    val description: String,
    val isDrive: Boolean,
    val isFileSystem: Boolean,
    val path: String,
)
