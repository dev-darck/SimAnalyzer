package com.analyzer.setup.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.adaptive.ResponsiveScreen

@Composable
internal fun SetupScreen(modifier: Modifier = Modifier) {
    ResponsiveScreen(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        backgroundColor = SimAnalyzerTheme.material.background,
    ) {
    }
}
