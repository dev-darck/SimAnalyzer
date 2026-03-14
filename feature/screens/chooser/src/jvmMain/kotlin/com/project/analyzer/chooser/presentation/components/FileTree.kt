package com.project.analyzer.chooser.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.analyzer.chooser.domain.model.TreeNode
import com.project.analyzer.feature.screens.chooser.Res.Res
import com.project.analyzer.feature.screens.chooser.Res.chooser_tree_collapse
import com.project.analyzer.feature.screens.chooser.Res.chooser_tree_expand
import com.project.analyzer.feature.screens.chooser.Res.chooser_tree_select_drive
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.onClick
import com.project.analyzer.ui.scrollbar.AppScrollbarAdapter
import com.project.analyzer.ui.scrollbar.AppVerticalScrollbar
import kotlinx.collections.immutable.PersistentList
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

private val CHEVRON_ANIM_DURATION_MS = 150.milliseconds.inWholeMilliseconds.toInt()

@Composable
fun FileTree(
    nodes: PersistentList<TreeNode>,
    currentDir: String,
    scrollToIndex: Int,
    onToggle: (path: String) -> Unit,
    onSelect: (path: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(scrollToIndex) {
        if (scrollToIndex >= 0 && scrollToIndex < nodes.size) {
            listState.animateScrollToItem(scrollToIndex)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (nodes.isEmpty()) {
            Text(
                text = stringResource(Res.string.chooser_tree_select_drive),
                color = SimAnalyzerTheme.material.onSurfaceVariant.copy(alpha = 0.5f),
                style = SimAnalyzerTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                items(items = nodes, key = { it.path }) { node ->
                    TreeRow(
                        node = node,
                        isSelected = node.path == currentDir,
                        onToggle = { onToggle(node.path) },
                        onSelect = { onSelect(node.path) },
                    )
                }
            }
        }

        AppVerticalScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(vertical = 4.dp),
            adapter = AppScrollbarAdapter(rememberScrollbarAdapter(listState)),
        )
    }
}

@Composable
private fun TreeRow(
    node: TreeNode,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onSelect: () -> Unit
) {
    val chevronAngle by animateFloatAsState(
        targetValue = if (node.expanded) 90f else 0f,
        animationSpec = tween(durationMillis = CHEVRON_ANIM_DURATION_MS),
        label = "chevron",
    )

    val background = if (isSelected) {
        SimAnalyzerTheme.material.primary.copy(alpha = 0.14f)
    } else {
        SimAnalyzerTheme.material.surface
    }

    val startPadding = (12 + node.depth * 16).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.corners.compact)
            .background(background)
            .onClick(onClick = {
                onToggle()
                onSelect()
            })
            .padding(start = startPadding, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
            when {
                node.loading -> CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 1.5.dp,
                    color = SimAnalyzerTheme.material.primary,
                )

                node.hasChildren -> Icon(
                    imageVector = Filled.ChevronRight,
                    contentDescription = if (node.expanded) {
                        stringResource(Res.string.chooser_tree_collapse)
                    } else {
                        stringResource(Res.string.chooser_tree_expand)
                    },
                    modifier = Modifier.size(16.dp).rotate(chevronAngle),
                    tint = SimAnalyzerTheme.material.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }

        Spacer(Modifier.width(4.dp))

        Icon(
            imageVector = if (node.expanded) Filled.FolderOpen else Filled.Folder,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = SimAnalyzerTheme.material.onSurfaceVariant.copy(alpha = 0.75f),
        )

        Spacer(Modifier.width(6.dp))

        Text(
            text = node.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = SimAnalyzerTheme.material.onSurface.copy(alpha = 0.85f),
            style = SimAnalyzerTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
    }
}
