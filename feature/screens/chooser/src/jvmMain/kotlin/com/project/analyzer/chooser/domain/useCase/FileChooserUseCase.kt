package com.project.analyzer.chooser.domain.useCase

import com.project.analyzer.chooser.domain.model.SidebarData
import com.project.analyzer.chooser.domain.model.TreeResult

interface FileChooserUseCase {

    suspend fun loadSidebar(): SidebarData

    suspend fun openDrive(path: String, label: String, showHidden: Boolean): TreeResult

    suspend fun toggleExpand(path: String, showHidden: Boolean): TreeResult

    suspend fun openPath(
        targetPath: String,
        drivePath: String,
        driveLabel: String,
        showHidden: Boolean,
    ): TreeResult

    suspend fun refreshTree(showHidden: Boolean): TreeResult

    fun currentTree(): TreeResult
}