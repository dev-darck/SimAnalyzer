package com.project.analyzer.chooser.domain.model

data class FsEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0L,
    val lastModified: Long = 0L,
)

