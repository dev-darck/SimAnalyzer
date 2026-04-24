package com.project.analyzer.crash.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.analyzer.feature.crash.Res.Res
import com.project.analyzer.feature.crash.Res.crash_code_panel_lines
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.scrollbar.AppHorizontalScrollbar
import com.project.analyzer.ui.scrollbar.AppScrollbarAdapter
import com.project.analyzer.ui.scrollbar.AppVerticalScrollbar
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CrashCodePanel(text: String, title: String) {
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()
    val lineCount = remember(text) {
        if (text.isBlank()) {
            0
        } else {
            text.count { it == '\n' } + 1
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.42f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = SimAnalyzerTheme.material.onSurfaceVariant,
            )
            Text(
                text = stringResource(Res.string.crash_code_panel_lines, title, lineCount),
                style = SimAnalyzerTheme.typography.labelMedium,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
            )
        }

        HorizontalDivider(color = SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.5f))

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.24f),
        ) {
            SelectionContainer {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = text,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(vScroll)
                            .horizontalScroll(hScroll)
                            .padding(16.dp)
                            .padding(end = 10.dp, bottom = 10.dp),
                        color = SimAnalyzerTheme.material.onSurface,
                        style = SimAnalyzerTheme.typography.bodySmall.copy(fontFamily = SimAnalyzerTheme.fonts.mono),
                        softWrap = false,
                    )
                    AppVerticalScrollbar(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(vertical = 6.dp),
                        adapter = AppScrollbarAdapter(rememberScrollbarAdapter(vScroll)),
                    )
                    AppHorizontalScrollbar(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(end = 12.dp, start = 6.dp),
                        adapter = AppScrollbarAdapter(rememberScrollbarAdapter(hScroll)),
                    )
                }
            }
        }
    }
}
