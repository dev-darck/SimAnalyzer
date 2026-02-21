package com.project.analyzer.chooser.domain

import com.project.analyzer.chooser.domain.model.TreeNode
import com.project.analyzer.chooser.domain.model.TreeResult
import dev.zacsweers.metro.Inject

@Inject
class TreeManager {

    var root: String = ""
        private set

    private val expandedPaths = mutableSetOf<String>()
    private val childrenByPath = mutableMapOf<String, List<String>>()
    private val parentByPath = mutableMapOf<String, String>()
    private val loadingPaths = mutableSetOf<String>()
    private val nameByPath = mutableMapOf<String, String>()

    fun setRoot(path: String, displayName: String) {
        root = path
        expandedPaths.clear()
        childrenByPath.clear()
        parentByPath.clear()
        loadingPaths.clear()
        nameByPath.clear()

        expandedPaths += path
        nameByPath[path] = displayName
    }

    fun setChildren(parentPath: String, children: List<String>) {
        childrenByPath[parentPath] = children
        loadingPaths -= parentPath

        for (child in children) {
            parentByPath[child] = parentPath
            if (child !in nameByPath) {
                nameByPath[child] = extractName(child)
            }
        }
    }

    fun hasChildrenCached(path: String): Boolean = path in childrenByPath

    fun isExpanded(path: String): Boolean = path in expandedPaths

    fun expand(path: String) {
        collapseSiblings(path)
        expandedPaths += path
    }

    fun collapse(path: String) {
        expandedPaths -= path
        collapseDescendants(path)
    }

    fun expandBranch(segments: List<String>) {
        for (segment in segments) {
            collapseSiblings(segment)
            expandedPaths += segment
        }
    }

    fun invalidateCache() {
        childrenByPath.clear()
        parentByPath.clear()
    }

    fun expandedSnapshot(): Set<String> = expandedPaths.toSet()

    fun flatten(focusPath: String? = null): TreeResult {
        if (root.isEmpty()) return TreeResult(emptyList())

        val result = mutableListOf<TreeNode>()
        walk(root, depth = 0, result)

        val scrollIndex = if (focusPath != null) {
            result.indexOfFirst { it.path == focusPath }.coerceAtLeast(-1)
        } else {
            -1
        }

        return TreeResult(nodes = result, scrollToIndex = scrollIndex)
    }

    private fun collapseSiblings(path: String): Set<String> {
        val parent = parentByPath[path] ?: return emptySet()
        val siblings = childrenByPath[parent] ?: return emptySet()
        val collapsed = mutableSetOf<String>()

        for (sibling in siblings) {
            if (sibling != path && sibling in expandedPaths) {
                expandedPaths -= sibling
                collapseDescendants(sibling)
                collapsed += sibling
            }
        }
        return collapsed
    }

    private fun collapseDescendants(path: String) {
        val children = childrenByPath[path] ?: return
        for (child in children) {
            if (expandedPaths.remove(child)) {
                collapseDescendants(child)
            }
        }
    }

    private fun walk(path: String, depth: Int, out: MutableList<TreeNode>) {
        val isExpanded = path in expandedPaths
        val isLoading = path in loadingPaths
        val children = childrenByPath[path]
        val hasChildren = children == null || children.isNotEmpty()

        out += TreeNode(
            path = path,
            name = nameByPath[path] ?: extractName(path),
            depth = depth,
            expanded = isExpanded,
            loading = isLoading,
            hasChildren = hasChildren,
        )

        if (isExpanded && children != null) {
            for (child in children) {
                walk(child, depth + 1, out)
            }
        }
    }

    private fun extractName(path: String): String =
        path.substringAfterLast('/').substringAfterLast('\\').ifEmpty { path }
}
