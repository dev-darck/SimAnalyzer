package com.project.analyzer.ui.adaptive

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

public interface ResponsiveScope {

    public fun item(
        key: Any? = null,
        isContentFull: Boolean = false,
        content: @Composable (isLinear: Boolean) -> Unit
    )

    public fun spacer(height: Dp)
}

internal class ListScopeAdapter(
    private val scope: LazyListScope
) : ResponsiveScope {

    override fun item(key: Any?, isContentFull: Boolean, content: @Composable (isLinear: Boolean) -> Unit) {
        if (key != null) scope.item(key = key) { content(true) } else scope.item { content(true) }
    }

    override fun spacer(height: Dp) {
        scope.item { Spacer(modifier = Modifier.height(height = height)) }
    }
}

internal class GridScopeAdapter(
    private val scope: LazyStaggeredGridScope,
) : ResponsiveScope {

    override fun item(key: Any?, isContentFull: Boolean, content: @Composable (isLinear: Boolean) -> Unit) {
        val span = if (isContentFull) StaggeredGridItemSpan.FullLine else StaggeredGridItemSpan.SingleLane

        if (key != null) {
            scope.item(key = key, span = span) { content(span == StaggeredGridItemSpan.FullLine) }
        } else {
            scope.item(span = span) { content(span == StaggeredGridItemSpan.FullLine) }
        }
    }

    override fun spacer(height: Dp) {
        scope.item(span = StaggeredGridItemSpan.FullLine) {
            Spacer(Modifier.height(height))
        }
    }
}
