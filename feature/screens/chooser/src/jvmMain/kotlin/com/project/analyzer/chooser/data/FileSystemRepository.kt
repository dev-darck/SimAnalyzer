package com.project.analyzer.chooser.data

import com.project.analyzer.chooser.data.model.SidebarInfo
import java.nio.file.Path

interface FileSystemRepository {

    suspend fun loadSideBars(): SidebarInfo

    suspend fun listSubdirectories(
        dir: Path,
        showHidden: Boolean,
    ): List<Path>

    suspend fun resolvePath(text: String): Path?
}