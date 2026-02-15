package com.project.analyzer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.analyzer.theme.SimAnalyzerTheme

@Composable
public fun Pagination(
    page: Int,
    pageCount: Int,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (pageCount <= 1) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.ChevronLeft,
            contentDescription = "Previous page",
            tint = if (page > 1) SimAnalyzerTheme.material.onSurfaceVariant
            else SimAnalyzerTheme.material.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier
                .size(18.dp)
                .clickable(enabled = page > 1) { onPageChange(page - 1) }
        )

        buildPageItems(page, pageCount).forEach { item ->
            if (item == null) {
                Text(
                    text = "...",
                    fontSize = 11.sp,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            } else {
                val isSelected = item == page
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isSelected) SimAnalyzerTheme.material.primary.copy(alpha = 0.2f)
                            else SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.15f)
                        )
                        .clickable { onPageChange(item) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.toString(),
                        fontSize = 11.sp,
                        color = if (isSelected) SimAnalyzerTheme.material.primary
                        else SimAnalyzerTheme.material.onSurfaceVariant
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = "Next page",
            tint = if (page < pageCount) SimAnalyzerTheme.material.onSurfaceVariant
            else SimAnalyzerTheme.material.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier
                .size(18.dp)
                .clickable(enabled = page < pageCount) { onPageChange(page + 1) }
        )
    }
}

private fun buildPageItems(page: Int, pageCount: Int): List<Int?> {
    if (pageCount <= 5) {
        return (1..pageCount).map { it }
    }

    val items = mutableListOf<Int?>()
    items.add(1)

    val windowStart = (page - 1).coerceAtLeast(2)
    val windowEnd = (page + 1).coerceAtMost(pageCount - 1)

    if (windowStart > 2) {
        items.add(null)
    }

    for (i in windowStart..windowEnd) {
        items.add(i)
    }

    if (windowEnd < pageCount - 1) {
        items.add(null)
    }

    items.add(pageCount)
    return items
}
