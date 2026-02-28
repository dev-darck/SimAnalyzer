package com.project.analyzer.chooser.domain.useCase

import com.project.analyzer.chooser.data.FileSystemRepository
import com.project.analyzer.chooser.domain.TreeManager
import com.project.analyzer.chooser.domain.model.File
import com.project.analyzer.chooser.domain.model.SidebarData
import com.project.analyzer.chooser.domain.model.TreeResult
import dev.zacsweers.metro.Inject
import java.nio.file.Path

@Inject
internal class FileChooserUseCaseImpl(
    private val repository: FileSystemRepository,
    private val treeManager: TreeManager,
) : FileChooserUseCase {

    override suspend fun loadSidebar(): SidebarData {
        val info = repository.loadSideBars()
        return SidebarData(
            places = info.shortcutFiles.map { File(it.displayName, it.path) },
            drives = info.drives.map { File(it.displayName, it.path) },
        )
    }

    override suspend fun openDrive(path: String, label: String, showHidden: Boolean): TreeResult {
        treeManager.setRoot(path, label)
        loadChildrenInto(path, showHidden)
        return treeManager.flatten(focusPath = path)
    }

    override suspend fun toggleExpand(path: String, showHidden: Boolean): TreeResult {
        if (treeManager.isExpanded(path)) {
            treeManager.collapse(path)
            return treeManager.flatten(focusPath = path)
        }

        treeManager.expand(path)

        if (!treeManager.hasChildrenCached(path)) {
            loadChildrenInto(path, showHidden)
        }

        return treeManager.flatten(focusPath = path)
    }

    override suspend fun openPath(
        targetPath: String,
        drivePath: String,
        driveLabel: String,
        showHidden: Boolean,
    ): TreeResult {
        treeManager.setRoot(drivePath, driveLabel)

        val segments = buildPathSegments(drivePath, targetPath)

        loadChildrenInto(drivePath, showHidden)

        for (i in 1 until segments.size) {
            val segment = segments[i]
            if (!treeManager.hasChildrenCached(segments[i - 1])) {
                loadChildrenInto(segments[i - 1], showHidden)
            }
            loadChildrenInto(segment, showHidden)
        }

        treeManager.expandBranch(segments)

        return treeManager.flatten(focusPath = targetPath)
    }

    override suspend fun refreshTree(showHidden: Boolean): TreeResult {
        val expanded = treeManager.expandedSnapshot()
        treeManager.invalidateCache()

        if (treeManager.root.isNotEmpty()) {
            loadChildrenInto(treeManager.root, showHidden)
        }
        for (path in expanded) {
            if (path != treeManager.root) {
                loadChildrenInto(path, showHidden)
            }
        }

        return treeManager.flatten()
    }

    override fun currentTree(): TreeResult = treeManager.flatten()

    private suspend fun loadChildrenInto(parentPath: String, showHidden: Boolean) {
        val childPaths = repository
            .listSubdirectories(Path.of(parentPath), showHidden)
            .map { it.toAbsolutePath().toString() }

        treeManager.setChildren(parentPath, childPaths)
    }

    private fun buildPathSegments(root: String, target: String): List<String> {
        val rootPath = Path.of(root)
        val targetPath = Path.of(target)

        if (rootPath == targetPath) return listOf(root)

        val relative = rootPath.relativize(targetPath)
        val segments = mutableListOf(rootPath.toString())
        var current = rootPath

        for (i in 0 until relative.nameCount) {
            current = current.resolve(relative.getName(i))
            segments += current.toString()
        }

        return segments
    }
}
